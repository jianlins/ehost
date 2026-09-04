# 修复方案：恢复裁决工作时出现重复标注

**日期**：2026-09-03
**状态**：✅ 已实施并通过手工验证 —— `mvn test` → 111 个测试，0 个失败
**缺陷**：重新打开 eHOST → 继续之前的裁决工作 → 保存，会产生重复标注
**分析文档**：[adjudication-duplicate-annotations-analysis.md](./adjudication-duplicate-annotations-analysis.md)
**English version**: [adjudication-duplicate-fix-plan.md](./adjudication-duplicate-fix-plan.md)

> 手工 GUI 验证（§7）在最初的重复写入之外又发现两处缺陷，均已修复：重叠的**一致**标注被误报为
> 分歧；被一致匹配吸收掉的搭档仍被持久化，导致文件条目数是界面显示的两倍。详见 **§7.3**。

---

## 1. 概述

在裁决过程中创建或修改、且**尚未确认为一致匹配**的标注，会被**两次**写入
`adjudication/<doc>.txt.knowtator.xml`——一次写成 `<annotation>`，一次写成 `<adjudicating>`——
原因是两个写入器的筛选条件相互重叠。重新打开时，它会被读回为**两条** `AdjudicationDepot` 记录，
且两者的状态相互矛盾。

修复方案**不是**删除其中一种元素类型：两者都必需，且各自有不同的使用方。正确做法是让两条筛选规则
**互斥**，并停止在写入时丢弃裁决状态。

工作分两个阶段推进：**阶段 A** 固化能复现该缺陷的失败测试；**阶段 B** 实施修复并让这些测试通过。

---

## 2. 前提事实：为什么两种元素都必须保留

| 元素 | 含义 | 写入方 | 读取方 |
|---|---|---|---|
| `<annotation>` | **最终裁决结果**（交付物） | `addAnnotations(root, true)` | IAA 报告，经由 `AdjudicationLoader.load()`——该方法会先显式剔除 `type == 5` |
| `<adjudicating>` | **尚未了结的工作状态**（用于恢复会话）：未决分歧与驳回 | `addAdjudicatingAnnotations(root)` | 恢复流程，经由 `AdjudicationLoader.loadWorkingState()` |

`docs/bugs/EHOST-001` 曾尝试删除 `<adjudicating>` 写入器；结果引发了
`docs/bugs/EHOST-003`（重启后裁决状态完全丢失），最终被回退。**请勿重复该做法。**

> §7.3 对此作了收窄：`<adjudicating>` 只保存仍带有*决策*的标注。被一致匹配吸收掉的搭档
> （`MATCHES_DLETED`）可由存活的结果推导得出，不再写入。

---

## 3. 当前路由 vs 目标路由

下表中目前只有一行是错误的。

| 状态 | 标注者 | 修复前 | 现在 |
|---|---|---|---|
| `MATCHES_OK` | 任意 | `<annotation>` | `<annotation>` |
| 非 `MATCHES_OK` | `ADJUDICATION` | 🔴 **`<annotation>` + `<adjudicating>`** | `<annotation>` |
| `MATCHES_DLETED` | 其他任何人 | `<adjudicating>` | *不再写入*（§7.3） |
| 非 `MATCHES_OK` | 其他任何人 | `<adjudicating>` | `<adjudicating>` |

```
当前（有缺陷）                              目标（修复后）
──────────────                              ──────────────
 标注                                        标注
   │                                           │
   ├──► 第 1 遍：status==OK                    ▼
   │    或 annotator==ADJUDICATION？    ┌───────────────────────┐
   │         └─► <annotation>           │ status == MATCHES_OK  │
   │                                    │ 或 annotator == ADJU. │
   └──► 第 2 遍：status!=OK？            └────┬─────────────┬────┘
            └─► <adjudicating>            是 │             │ 否
                                             ▼             ▼
   两者都可能触发 ⇒ 2 个元素          <annotation>   <adjudicating>
                                        只走一个分支 ⇒ 1 个元素
```

---

## 4. 阶段 A —— 证明缺陷存在的测试（部分已完成）

### A.1 现状

[`src/test/java/resultEditor/save/AdjudicationRoundTripTest.java`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/test/java/resultEditor/save/AdjudicationRoundTripTest.java)
已经创建，目前 **9 个用例中有 7 个失败**——即已成功复现该缺陷。

| # | 测试 | 当前 | 修复后 |
|---|---|---|---|
| 1 | `adjudicationAuthored_unresolved_isSerializedOnce` | ❌ 得到 2 个元素 | ✅ |
| 2 | `normalAnnotator_unresolved_isSerializedOnce`（对照组） | ✅ | ✅ |
| 3 | `agreedMatch_isSerializedOnceAsAnnotation`（对照组） | ✅ | ✅ |
| 4 | `reopenAfterSave_doesNotDuplicate` | ❌ 得到 2 条 | ✅ |
| 5 | `reopenAfterSave_mixedSession_doesNotDuplicate` | ❌ 4 条变成 5 条 | ✅ |
| 6 | `repeatedCycles_remainStable` | ❌ 第 1 轮即漂移 | ✅ |
| 7 | `fullLifecycle_twoAdjudicationSessions_noDuplicates` | ❌ 3 条变成 4 条 | ✅ |
| 8 | `adjudicationStatus_survivesRoundTrip` | ❌ | ✅ |
| 9 | `annotationEqualsHashCodeContract` | ❌ | ✅ |

两个对照组用例通过，恰好证明这些测试具有**诊断性**而非单纯严格：它们把故障精确定位到
`annotator == ADJUDICATION` 且未解决这一特定组合上。

### A.1.1 全生命周期测试的实现方式

测试 #7 在**无 GUI 环境**下复现了用户报告的完整流程
（*打开 → 裁决 → 保存 → 关闭 → 重新打开 → 再次裁决 → 保存*）。这一实现方式值得说明，
因为所有直观的入口方法都需要 `userInterface.GUI` 实例。

**各步骤的模拟方式**

| 用户操作 | 测试中的实现 | 原因 |
|---|---|---|
| **打开项目** | `seedAndSave(doc, …)` —— 用 `Article` + `Annotation` 对象填充 `AdjudicationDepot` | `Adjudication.checkAnnotations()` 需要 GUI；而 depot 正是它会产生的状态 |
| **裁决** | `adjudicateAccept()` / `adjudicateReject()` / `adjudicateCreateNew()` | GUI 是就地修改 depot 记录——参见 [Adjudication.java:1395](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/Adjudication.java:1395)（接受）、[:1463](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/Adjudication.java:1463)（拒绝）、[:1421](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/Adjudication.java:1421)（删除）。辅助方法完全复现了这些修改 |
| **保存** | `new OutputToXML().directsave(txtFile)` | 真实的保存路径；同时写入 `saved/` 与 `adjudication/` |
| **关闭** | `new Depot().clear(); AdjudicationDepot.clear();` | 两个 depot 都是 `static`，因此清空它们对代码而言*就是*一次进程重启 |
| **重新打开** | `ImportAnnotation.XMLImporter(savedXmls)` | 即 [`Reload.extractAnnotation_fromXML`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/reloadSavedAnnotations/Reload.java:205) 所调用的方法。`Reload.load()` 本身需要 `GUI` 参数，无法使用 |
| **继续裁决** | `AdjudicationLoader.loadWorkingState()` | 与 [`GUI.mode_continuePreviousAdjudicationWork()`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/userInterface/GUI.java:10606) 所调用的完全一致 |

有两个细节决定了该模拟是**忠实的**而非仅仅方便：

- **`GUI.reviewmode = adjudicationMode`** 在 `@BeforeEach` 中设置。只有该静态标志被设置时，
  `directsave` 才会写入 `adjudication/` 目录（[OutputToXML.java:77](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:77)）；
  否则测试会静默地走到错误的代码路径上。
- **`adjudicateCreateNew()` 保持 `adjudicationStatus` 为默认值**并调用 `setUnProcessed()`，
  与 [`AdjudicationDepot.addANewAnnotation`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/data/AdjudicationDepot.java:785) 保持一致。若按直觉将其设为
  `MATCHES_OK`，就会完全绕开该缺陷，测试在有问题的代码上也会通过。

**断言内容**：跨会话统计 `AdjudicationDepot` 记录数，以及 XML 中的原始元素数：

```java
seedAndSave(doc, fever, cough);        // 来自 2 名标注者的 2 条标注
adjudicateAccept(doc, "fever");        // → MATCHES_OK
adjudicateReject(doc, "cough");        // → NON_MATCHES
adjudicateCreateNew(doc, "chest pain", …);   // → ADJUDICATION，未解决
save(doc);                             // 磁盘上应为 3 条标注

restartAndResumeAdjudication();        // 关闭 + 重新打开 + 继续
assertEquals(3, adjudicationDepotCount(doc));   // ← 当前失败：得到 4 条
```

多出的第 4 条正是 `chest pain`——三条中唯一同时满足
*`annotator == ADJUDICATION` **且**未解决*的标注，因而也是唯一被两个写入器同时认领的一条。
`fever`（`MATCHES_OK`）与 `cough`（被拒绝、普通标注者）各自只落入一个分支。这就把缺陷精确
隔离到了一个真实会话中的单条标注上。

随后测试还会运行**第三个**会话以验证收敛性——因为重新打开后，重复的状态会成为下一轮的*输入*，
所以误差会按会话累积，而不是始终只多一条。目前测试还跑不到那一步：它在第 2 个会话就失败了。

**为何断言用计数而非文本比对**：比对 XML 文本会很脆弱（每次保存都会通过
`latestUsedMentionID` 重新生成 mention ID）。统计 depot 记录数与 XML 子元素数在多次运行间保持
稳定，且直接表达了不变式：*进去 N 条标注，出来必须还是 N 条*。

### A.2 进入阶段 B 之前仍需补充的测试

- **`legacyFile_withoutStatus_stillLoads`** —— 构造一个**旧格式**的
  `adjudication/*.knowtator.xml`（`<annotation>` 不含 `<AdjudicationStatus>` 子元素），加载后断言其
  状态为 `MATCHES_OK`。这会在步骤 B.2 改动写入器之前，先锁定向后兼容契约。
- **`savedFolderOutput_isUnchanged`** —— 断言 `saved/<doc>.knowtator.xml`（标注模式下的常规交付物）
  在改动前后逐字节一致。本次修复不得影响非裁决的保存路径。
- **`iaaReportPath_unaffected`** —— `AdjudicationLoader.load()`（供 IAA 报告使用，与
  `loadWorkingState()` 不同）必须仍能看到同一组最终标注。

### A.3 需先记录的基线

在改动 `src/main` **之前**记录当前全量测试结果，以便后续任何新增故障都可归因。
记录时间 2026-09-03：

```
mvn test
→ Tests run: 80, Failures: 6, Errors: 0, Skipped: 0

    eHOSTTest ......................................  1  ✅
    AnalysisTest ...................................  7  ✅
    ComparatorTest .................................  9  ✅
    OverlappingAnnotationsTest .....................  9  ✅
    HtmlReportIntegrationTest ......................  7  ✅
    IAACalculationTest ............................. 11  ✅
    IAATest ........................................  8  ✅
    AdjudicationRoundTripTest ......................  8  ❌ 6 个失败  ← 本缺陷
    OutputToXMLTest ................................ 14  ✅
    ProjectLockTest ................................  6  ✅
```

**所有失败都集中在 `AdjudicationRoundTripTest`，其余 74 个测试全部通过。**
全生命周期测试（#7）是在该基线之后补充的，使该测试类变为 9 个用例 / 7 个失败，整个套件变为
81 / 7。阶段 B 完成后，预期结果为 `Tests run: 84, Failures: 0`（81 个 + A.2 新增的 3 个）。

---

## 5. 阶段 B —— 修复实施

共 5 项改动。B.1–B.3 是修复主体，**相互依赖，不可分开发布**；B.4–B.5 用于消除掩盖该缺陷的
潜在隐患。

### B.1 让两个写入器互斥

**文件**：[`OutputToXML.java:367`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:367)，位于 `addAdjudicatingAnnotations`

```java
// 修改前
if (annotation.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK) {
    continue;
}

// 修改后 —— 跳过 addAnnotations(root, true) 已经处理过的标注
if (annotation.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK
        || "ADJUDICATION".equals(annotation.getFullAnnotator())) {
    continue;
}
```

> ⚠️ **切勿改为把 `addAnnotations(root, true)` 的条件收窄为仅 `MATCHES_OK`。**
> 其中的 `|| annotator == "ADJUDICATION"` 子句是关键依赖：
> [`AdjudicationDepot.addANewAnnotation`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/data/AdjudicationDepot.java:785) 对裁决者创建的标注只调用了
> `setUnProcessed()`，该方法修改的是 `isMatchingAnalysisForIAAProcessed`，**并不会**修改
> `adjudicationStatus`——因此这些标注仍保持字段默认值 `EXCLUDED`。删除该子句会导致裁决者本人的
> 工作成果被静默地从最终输出中丢弃。

### B.2 在 `<annotation>` 上同样持久化状态

**文件**：[`OutputToXML.java:636`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:636)，位于 `buildAnnotationNode`

目前 `<processed>` 与 `<AdjudicationStatus>` 仅在 `outputAnnotationInMirrorMemeory` 为真时才写出，
也就是只对 `<adjudicating>` 写出。执行 B.1 之后，`<annotation>` 会合法地承载非 `MATCHES_OK` 的
裁决者工作，因此必须写出其真实状态。请移除包裹这两个元素的 `if (outputAnnotationInMirrorMemeory)`
判断。

### B.3 重新启用 `<annotation>` 的状态读取

**文件**：[`ImportXML.java:283‑296`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/imports/ImportXML.java:283)

这一点很容易被遗漏。`<annotation>` 的解析循环（`root.getChildren("annotation")`，第 256 行）
中状态解析被**注释掉了**，而 `<adjudicating>` 的循环（第 363 行）中完全相同的代码块在第 390 行
是**启用**的：

```java
// 在 <annotation> 循环中当前被注释掉 —— 必须恢复
String adjudication_status = "NOBODY";
//Element element_adjudicationStatus = annotations.getChild("AdjudicationStatus");
//if( element_adjudicationStatus != null ){
//    adjudication_status = element_adjudicationStatus.getText();
//}
```

请同时取消该段以及其下方 `<processed>` 代码块的注释。**若缺少 B.3，B.2 写出的状态将无人读取**，
往返过程仍会丢失状态。

为保证向后兼容，需保留兜底逻辑：`<annotation>` 上缺失 `<AdjudicationStatus>` 时，仍须解析为
`MATCHES_OK`，因为现有文件隐含的就是这一语义。

### B.4 停止在加载时臆造状态

**文件**：[`AdjudicationLoader.java:206`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:206)

```java
ann.adjudicationStatus = Annotation.AdjudicationStatus.MATCHES_OK;   // 删除
```

有了 B.2 + B.3，真实状态已能在 XML 中留存，这条硬编码赋值既无必要也是错误的。仅当 XML 确实缺失
该字段时（即 A.2 中锁定的遗留场景）才应用 `MATCHES_OK` 默认值。

### B.5 修复标识判定隐患

- **`Annotation.hashCode()`** —— [`Annotation.java:1266`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/annotations/Annotation.java:1266) 重写了基于值的
  `equals()`，却没有对应的 `hashCode()`。需按 `equals()` 所用字段补充实现
  （小写化的 `annotationText`、`annotationclass`、`annotator`、`creationDate`、`spanset`）。
- **`AdjudicationLoader` 中的 `removeAll` 调用** —— [第 195 行](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:195) 与
  [第 119 行](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:119) 使用值相等语义，可能删除并非自己添加的标注。应改为按引用标识或
  `uniqueIndex` 匹配。
- **`seenAdjudicationKeys`** —— [OutputToXML.java:281‑306](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:281) 正是针对 B.1 所要消除的
  重复问题打的补丁，而且在 `spanset` 为 null 时会静默合并本不相同的标注。B.1 生效后应重新评估并
  大概率删除——但务必在测试全绿**之后**，作为独立提交进行。

---

## 6. 执行顺序

| 步骤 | 操作 | 通过标准 |
|---|---|---|
| 1 | 记录 `mvn test` 基线 | ✅ 已完成 —— 81 个测试，7 个失败，全部位于 `AdjudicationRoundTripTest` |
| 2 | 补充 A.2 的三个测试 | ✅ 已完成 —— 12 个测试 / 8 个失败；两个对照测试立即通过，`legacyFile_withoutStatus_stillLoads` 失败 |
| 3 | **同时应用 B.1 + B.2 + B.3** | ✅ 测试 1、4、5、6、7、8 转为通过 |
| 4 | 应用 B.4 | ✅ 遗留格式测试通过 |
| 5 | 应用 B.5（`hashCode`、`removeAll`） | ✅ 测试 9 转为通过 |
| 6 | 全量 `mvn test` | ✅ `Tests run: 86, Failures: 0` |
| 7 | 手工 GUI 验证（见 §7） | 🟡 部分已自动化 —— 第 7 步（升级路径）已转为测试；纯 GUI 步骤仍待完成 |
| 8 | 单独处理：移除 `seenAdjudicationKeys` | ✅ 已移除；由 `identicalLookingAnnotations_areBothPreserved` 锁定 |
| 9 | 修复由缺陷版本写出的文件夹 | ✅ 在发现 §7 第 7 步未被满足后补充 —— 见 §6.2 |

步骤 3–5 应拆分为独立提交，以便出现回归时能干净地二分定位。

### 6.1 与原计划的偏差

真正动手后有三处与计划书写的不同。

- **B.2 是有条件的，而非无条件移除。** `buildAnnotationNode` 同时服务于 `saved/` 写出路径，
  若直接删除 `if (outputAnnotationInMirrorMemeory)` 判断，`<processed>` / `<AdjudicationStatus>`
  会泄漏到普通标注模式的交付物中。该判断改为
  `outputAnnotationInMirrorMemeory || is_outputing_adjudicated_annotations`，并由 A.2 的
  `savedFolderOutput_isUnchanged` 锁定。
- **还有第四处产生重复的代码需要删除。** `ImportXML.readXMLContents` 中有一段"向后兼容修复"，
  会为裁决文件里每个没有 `<adjudicating>` 孪生元素的 `<annotation>` 合成一个副本。这正是
  B.3 + B.4 现已原生处理的遗留场景，保留它会让 `legacyFile_withoutStatus_stillLoads`
  把一条标注读成两条，因此已移除。
- **遗留的 `MATCHES_OK` 默认值放在 `AdjudicationLoader` 而非 `ImportXML`。** `ImportXML`
  忠实解析，缺少 `<AdjudicationStatus>` 时保留 `"NOBODY"` 哨兵值 —— 该解析器与 `saved/`
  共用，在那里默认成 `MATCHES_OK` 会改变无关行为。`loadWorkingState()` 仅针对 `<annotation>`
  节点把该哨兵值改写为 `MATCHES_OK`，恰好是文档约定的向后兼容契约，不多也不少。

第 8 步的 `seenAdjudicationKeys` 选择删除而非保留：应用 B.1 之后它已无法阻止任何重复
——`addAnnotations(root, true)` 对每条仓库条目至多输出一个元素 —— 其唯一残留作用就是悄悄丢弃
外观相同的裁决成果，恰恰违反本次修复要确立的 *进出数量一致* 不变式。

### 6.2 第 9 步 —— 升级路径（计划书未充分说明的缺口）

§7 第 7 步要求针对*"由当前版本生成的 `adjudication/` 文件夹"*做遗留检查。A.2 的
`legacyFile_withoutStatus_stillLoads` 测试**并不**满足该要求：它验证的是*远古*格式（单独一个
无状态的 `<annotation>`），而缺陷版本写出的是**重复配对**。二者输入不同，且只有后者才是真实
用户磁盘上的内容。

从缺陷版本的写出器原样捕获，一条裁决者创建且未解决的标注被存储为：

```xml
<annotation>                       <!-- 无 <AdjudicationStatus> -->
    <spannedText>chest pain</spannedText>
</annotation>
<adjudicating>                     <!-- 同一条标注再次出现 -->
    <spannedText>chest pain</spannedText>
    <AdjudicationStatus>NON_MATCHES</AdjudicationStatus>
</adjudicating>
```

仅有 B.1–B.5 **无法**修复这种文件。恢复时得到 `[chest pain, cough, chest pain, fever]` ——
重复项在升级后依然存在；又因为修复后的写出器会把*两份*副本都送往 `<annotation>` 一侧，
它将在此后每次保存中永久延续。修复本可以在测试全绿的表象下发布，却让每个进行中的项目
永久损坏。

`AdjudicationLoader.healLegacyNodes()` 弥补了这一点。裁决文件夹中无状态的 `<annotation>`
只可能来自旧版本，而是否存在 `<adjudicating>` 孪生元素（按 跨度 + 文本 + 标注者 + 创建日期
匹配，因为 mention id 每次保存都会重新生成）可区分两种遗留形态：

| 遗留形态 | 有孪生？ | 处理 |
|---|---|---|
| 无状态版本写出的最终结果 | 否 | 默认为 `MATCHES_OK`（A.2 契约） |
| 双重写出缺陷 | 是 | 丢弃无状态的 `<annotation>`；孪生元素持有真实状态 |

由 `preFixDuplicatePair_healsOnResume` 锁定，该测试断言了合并结果、`NON_MATCHES` 状态得以保留，
以及下一次保存会以修复后的"每条标注一个元素"形式重写文件。

---

## 7. 手工验证

自动化测试覆盖的是数据往返，而非界面。测试套件全绿之后：

1. 打开一个包含 ≥ 2 名标注者标注的项目，进入裁决模式。
2. 进行裁决，使至少一条标注最终由 `ADJUDICATION` 创建**且**处于未解决状态。
3. 保存。检查 `adjudication/<doc>.txt.knowtator.xml`——该标注必须只出现**一次**。
4. 关闭 eHOST，重新打开，选择*继续之前的裁决工作*。
5. 裁决视图中应显示**一条**记录，且原始状态保持不变。
6. 再次保存并重新检查。仍应只有一个元素——多轮循环不应增长。
7. **遗留兼容检查**：针对由*当前*版本生成的 `adjudication/` 目录重复第 4–6 步，确认已有的
   进行中工作仍能正常加载。
   *现已由 `preFixDuplicatePair_healsOnResume`（见 §6.2）自动覆盖，该测试针对缺陷版本写出器
   输出的原样捕获执行恢复。正是这一步最初揭示了仅有 B.1–B.5 无法消除重复 —— 手工验证时
   仍建议保留此步作为兜底检查。*
8. 生成一次 IAA 报告，确认裁决结果仍能正常显示。
9. **重叠检查**（§7.3 之后新增）：让两名标注者在同一 span 上标注两个不同类别，进入裁决模式，
   确认两条都渲染为已裁决——都不应带有表示分歧的波浪下划线。再调整其中一条的 span 边界并保存，
   确认文件中的条目与界面显示完全一致。

> **状态：已完成。** 第 1–9 步均已在 GUI 中走查。第 1–8 步通过；第 9 步两次失败，暴露出 §7.3
> 中修复的两个缺陷。修复后的复测已由报告者确认正确。
>
> ⚠️ 手工验证请针对项目的**副本**进行，切勿直接使用 `src/test/resources/`——原因见 §7.2 第二条。

### 7.1 面向真实项目的无界面端到端覆盖

`src/test/java/adjudication/TwoAnnotatorProjectAdjudicationTest.java`（12 个测试）在无显示环境下
覆盖第 2–8 步，且基于两个**真实**的 eHOST 项目目录，而非手工填充的 depot：

- `testsupport/EhostProjectFixture` 生成含 `config/ corpus/ saved/` 的项目与双文档临床语料，随后
  **整目录复制**并以第二名标注者的身份重新标注——这正是真实双标注者研究产生的文件形态。span 通过
  `indexOf` 在正文中定位，因此每个 `<span>` 都可证明覆盖其自身的 `<spannedText>`。
- 两组标注覆盖了比对引擎能区分的全部关系：完全一致、部分跨度重叠、同一 span 不同类别、以及仅由
  单个标注者标注的跨度。
- 流程使用 eHOST 自身的实现：`ImportAnnotation.XMLImporter` → `AdjudicationDepot.copyAnnotations`
  → `Adjudication.searchDifferenceinArticle` → `OutputToXML.directsave` →
  `AdjudicationLoader.loadWorkingState`。裁决者操作调用生产代码中的修改方法
  （`Depot.setAnnotationToMatchedOK_byUID`、`deleteAnnotation_byUID_onAdjudicationMode`、
  `AdjudicationDepot.deleteAnnotation_byUID`），而非直接赋值字段。
- 核心断言是磁盘上的 XML 与内存工作集构成精确的多重集镜像——这才是防重复的准确不变式，而不是
  启发式的重复扫描。

**已针对缺陷代码验证**：将四个已修复的源文件回退到 `70606f4` 后，12 个测试中有 4 个失败，包括
`substernal chest pain` 在同一文件中出现两次、以及重启后 `MATCHES_OK` 计数由 7 漂移到 8。

此外还厘清了两点既有行为，避免被误认为重复：

- **完全一致的标注由比对引擎自动了结**，无需裁决者介入：一份变为 `MATCHES_OK`，其搭档变为
  `MATCHES_DLETED`。两者都保留在工作集中，但只有被接受的那份会被持久化（§7.3），因此重启后需要
  保持的是「该一致结论仍然成立」——恰好返回一条、且仍为 `MATCHES_OK`
  （`autoResolvedMatch_survivesRestart`）。
- `addAnnotations(root, true)` 会把 `adjudication/` 中所有最终 `<annotation>` 的标注者
  **一律改写为 `ADJUDICATION`**，`setAnnotationToMatchedOK_byUID` 同样如此。因此对已解决的标注而言，
  原始作者信息无法从裁决目录中恢复——只有 `<adjudicating>` 条目保留作者。本次修复未改变该行为。

### 7.2 首次报告：2 个 `<annotation>` + 2 个 `<adjudicating>`

> ⚠️ **以下结论是错的** —— 见 §7.3。该文件内部是自洽的，但它本就不该包含那两个 `<adjudicating>`
> 条目，而产生它的状态本身还是一个匹配缺陷的结果。保留此节是因为其中两项旁支发现依然成立。

手工验证提出了 `proj2/adjudication/doc3.txt.knowtator.xml`：编辑两条重叠标注之一并接受两者后，
界面显示两条标注，文件却有四个条目。两名标注者在*同一 span* 上标注了*两个不同类别*，因此裁决中
确实承载四条标注：

| 条目 | 标注者 | 类别 | 状态 | 界面显示 |
|---|---|---|---|---|
| `<annotation>` | `ADJUDICATION` | CONCEPT | `MATCHES_OK` | 是（已编辑的 span） |
| `<annotation>` | `ADJUDICATION` | CON2 | `MATCHES_OK` | 是 |
| `<adjudicating>` | `a2` | CONCEPT | `MATCHES_DLETED` | 否 |
| `<adjudicating>` | `a2` | CON2 | `MATCHES_DLETED` | 否 |

两个写入器按构造互斥（§B.1），所以四条标注产生四个元素；而 `GUI.reloadAnnotationsToScreen` 会跳过
`*_DLETED`，所以界面画出两条。这部分分析是准确的。遗漏之处在于：被吸收的搭档根本没有理由留在磁盘上
——「墓碑记录可让恢复流程重建配对关系」这一论据并不成立，因为已了结的一致结论压根不存在待重建的配对。
§7.3 已将其移除。

在核查过程中还发现两个真实问题，二者依然成立：

- **旧格式修复逻辑会误删真实标注。** `legacyTwinKey` 用 span + 文本 + 标注者 + `creationDate`
  标识一条标注，这些字段都无法区分「同一 span、仅类别不同」的两条标注。`creationDate` 只精确到秒，
  同一秒内对一个 span 标注两次即会撞键，导致一条没有状态的历史结果被当作「重复」悄悄丢弃。现已将
  类别（通过 `classMentions` 解析）纳入标识。单独回退该改动会使
  `legacyHealer_keepsDifferentClassesOnTheSameSpan` 失败（工作集为 1 而非 2）。
- **手工验证覆盖了受版本控制的夹具。** GUI 会话直接指向 `src/test/resources/proj2`，就地重写了其
  `saved/`、`adjudication/` 与 `reports/` 文件（提交 `a0d226f`），破坏了 `IAACalculationTest`
  据以核对期望值的 `att1`/`att2` 属性数据，导致 HEAD 上有 11 个测试变红。夹具已恢复到 `e7e838f`
  的内容。**请勿让 eHOST 手工会话直接指向 `src/test/resources/`** —— 先把项目复制到别处。新测试
  应像 `OverlappingClassAdjudicationTest` 那样在临时目录自建夹具，而不是读取其他测试所依赖的共享夹具。

### 7.3 后续报告：一致被显示为分歧，以及与界面不符的文件

§7.2 中「四个元素是正确的」这一结论，就*写入器*而言成立；但第二轮手工验证表明，该形态其实是两个
更深层问题的表征。

**一致被报告为分歧（既有缺陷，`Adjudication.java`）。** 在 `searchDifferenceinArticle` 中，只要有
一条重叠标注未通过类别/属性比对，就会置 `foundDifference = true`，从而把整组候选标记为
`NON_MATCHES`。于是当两名标注者在同一 span 上标注两个类别（即**两次一致**）时，第一对被解决，
第二对却带着分歧波浪线呈现给裁决者，尽管两人写的内容完全相同。

重叠但比对结果不同的标注只是**另一条标注**，不能作为「这条有争议」的证据。是否真的一致，下方的
`checkAnnotators()` 早已负责判断——它要求每位选定标注者都出现在匹配集合中；该短路把它跳过了。
现已移除该标志。真实分歧仍由 `checkAnnotators()` 报出：

| 场景 | 修复前 | 修复后 |
|---|---|---|
| a1{CONCEPT}, a2{CONCEPT} | OK + DLETED | 不变 |
| a1{CONCEPT, CON2}, a2{CONCEPT, CON2} | OK + DLETED + **2 个 NON_MATCHES** | OK×2 + DLETED×2 |
| a1{CONCEPT}, a2{CON2} | 2 个 NON_MATCHES | 不变 |
| a1{CONCEPT, CON2}, a2{CONCEPT} | — | OK + DLETED + NON_MATCHES（a1 多出的 CON2） |

已针对 `70606f4` 验证，说明该问题早于本次重复修复即存在。`OverlappingAgreementMatchingTest`
覆盖了两个方向；单独回退 `Adjudication.java` 会使其 7 个测试中的 3 个失败。

**不再持久化被吸收的搭档（`OutputToXML.addAdjudicatingAnnotations`）。** `MATCHES_DLETED` 标记的是
被*一致*匹配吸收掉的搭档。它完全由存活的 `MATCHES_OK` 标注推导而来，被 `GUI.reloadAnnotationsToScreen`
隐藏，且不承载任何决策——写入它只会让裁决文件的条目数是界面显示的两倍，这正是看起来像重复的原因。
现已跳过。驳回（`NONMATCHES_DLETED`）与未决分歧（`NON_MATCHES`）是真实决策，照常写入。

这样做是安全的：`loadWorkingState()` **仅**从裁决目录重建 `AdjudicationDepot`，所以未写入的搭档
就是单纯不存在，不会以新分歧的形式重新出现。仍包含搭档的旧文件可正常加载，并在下次保存时自动规整。
重新运行比对则会从 `saved/` 完整重建。

---

## 8. 风险

| 风险 | 缓解措施 |
|---|---|
| 破坏进行中裁决目录的恢复功能 | 两种不同的遗留形态均由 `healLegacyNodes()` 处理，并分别由 `legacyFile_withoutStatus_stillLoads`（无孪生 → `MATCHES_OK`）与 `preFixDuplicatePair_healsOnResume`（有孪生 → 合并）锁定。见 §6.2 |
| 重蹈 EHOST-001 → EHOST-003 的回归覆辙 | 两种元素类型均予保留，仅改动*筛选条件*。测试 4–6 为直接防线 |
| 裁决者创建的工作被静默丢弃 | B.1 改动的是 `<adjudicating>` 一侧，绝不触碰 `<annotation>` 一侧——参见 B.1 的警告 |
| `hashCode()` 影响其他位置的行为 | `Annotation` 目前未在任何地方用作哈希键；提交前需先做用法检索确认 |
| 裁决 XML 文件体积增大 | 影响中性：每条重叠标注减少一个元素，每个 `<annotation>` 增加两个小的子元素 |
| 丢弃 `MATCHES_DLETED` 导致状态丢失（§7.3） | `loadWorkingState()` 仅从 `adjudication/` 重建工作集，未写入的搭档只是缺席，不会变成新的分歧；它被吸收进的那条已接受结果仍会写入。由 `save_writesOnlyTheAcceptedResults` 与 `layoutIsStableAcrossResume` 锁定 |
| 放宽匹配规则掩盖真实分歧（§7.3） | `checkAnnotators()` 仍要求每位选定标注者都有匹配。`OverlappingAgreementMatchingTest` 对两个方向都做了断言：类别分歧、单标注者独有标注、以及一方多出的类别，均仍为 `NON_MATCHES` |

---

## 9. 不在本次范围内

- `chrisleng` 分支中的 UMLS / CUI 相关工作（见[分支评估 §4.2](./fork-chrisleng-netbeans-review.md)）。
- 已失效的 `adjudicationParameters()` / `getAdjudicationSetting()` 方法（分支评估 §4.9）——
  相关但独立，另行处理。
- 对 `saved/`（标注模式）输出路径的任何改动。

---

## 10. 最终状态

| | |
|---|---|
| 测试套件 | 111 个测试，0 个失败（`mvn test`） |
| 改动的源文件 | `Adjudication.java`、`ImportXML.java`、`AdjudicationLoader.java`、`Annotation.java`、`OutputToXML.java` |
| 已修复的缺陷 | 标注被写入两次（§1）；旧格式孪生标识忽略类别（§7.2）；重叠的一致被报为分歧（§7.3）；被吸收的搭档仍被持久化（§7.3） |
| 手工 GUI 验证 | §7 第 1–9 步，已由报告者确认 |

每项修复都有对应测试守护：单独回退该项修复即会失败——四项均已逐一验证。
