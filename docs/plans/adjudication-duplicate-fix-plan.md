# Fix Plan: Duplicate Annotations When Resuming Adjudication

**Date**: 2026-09-03
**Status**: ✅ Implemented and manually verified — `mvn test` → 111 tests, 0 failures
**Bug**: reopening eHOST → continuing a previous adjudication → saving produces duplicate annotations
**Analysis**: [adjudication-duplicate-annotations-analysis.md](./adjudication-duplicate-annotations-analysis.md)
**中文版**: [adjudication-duplicate-fix-plan.zh-CN.md](./adjudication-duplicate-fix-plan.zh-CN.md)

> Manual GUI verification (§7) found two further defects beyond the original double-write, both now
> fixed: overlapping agreements were reported as disagreements, and absorbed match partners were
> persisted so the file held twice what the editor showed. See **§7.3**.

---

## 1. Summary

An annotation created or edited **during** adjudication that is **not yet an agreed match** is written
to `adjudication/<doc>.txt.knowtator.xml` **twice** — once as `<annotation>`, once as `<adjudicating>` —
because the two writers' filters overlap. On reopen it is read back as **two** `AdjudicationDepot`
entries with conflicting statuses.

The fix is **not** to drop one of the two element types: both are required, and each has a different
consumer. It is to make the two selection rules **disjoint**, and to stop discarding the adjudication
status at write time.

Work proceeds in two phases: **Phase A** locks in failing tests that reproduce the bug; **Phase B**
implements the fix and turns those tests green.

---

## 2. Ground truth: why both element types must stay

| Element | Meaning | Written by | Read by |
|---|---|---|---|
| `<annotation>` | **final adjudicated result** (the deliverable) | `addAnnotations(root, true)` | IAA reporting, via `AdjudicationLoader.load()` — which explicitly strips `type == 5` first |
| `<adjudicating>` | **outstanding working state** (so a session can resume): open disagreements and rejections | `addAdjudicatingAnnotations(root)` | resume, via `AdjudicationLoader.loadWorkingState()` |

`docs/bugs/EHOST-001` already tried deleting the `<adjudicating>` writer; that caused
`docs/bugs/EHOST-003` (total loss of adjudication state on restart) and was reverted. **Do not repeat
that approach.**

> Refined by §7.3: `<adjudicating>` holds only annotations that still carry a *decision*. The partner
> that an agreed match absorbed (`MATCHES_DLETED`) is derived from the surviving result and is no
> longer written.

---

## 3. Current vs target routing

Only one row of this table is wrong today.

| Status | Annotator | Before the fix | Now |
|---|---|---|---|
| `MATCHES_OK` | any | `<annotation>` | `<annotation>` |
| not `MATCHES_OK` | `ADJUDICATION` | 🔴 **`<annotation>` + `<adjudicating>`** | `<annotation>` |
| `MATCHES_DLETED` | anyone else | `<adjudicating>` | *not written* (§7.3) |
| not `MATCHES_OK` | anyone else | `<adjudicating>` | `<adjudicating>` |

```
CURRENT (broken)                          TARGET (fixed)
────────────────                          ──────────────
 annotation                                annotation
     │                                         │
     ├──► Pass 1: status==OK                   ▼
     │    OR annotator==ADJUDICATION?    ┌───────────────────────┐
     │         └─► <annotation>          │ status == MATCHES_OK  │
     │                                   │ OR annotator == ADJU. │
     └──► Pass 2: status!=OK?            └────┬─────────────┬────┘
              └─► <adjudicating>          YES │             │ NO
                                              ▼             ▼
   both can fire ⇒ 2 elements          <annotation>   <adjudicating>
                                          exactly one branch ⇒ 1 element
```

---

## 4. Phase A — tests that prove the bug (partly done)

### A.1 Status

[`src/test/java/resultEditor/save/AdjudicationRoundTripTest.java`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/test/java/resultEditor/save/AdjudicationRoundTripTest.java)
already exists and currently **fails 7 of 9** — i.e. it successfully reproduces the bug.

| # | Test | Now | After fix |
|---|---|---|---|
| 1 | `adjudicationAuthored_unresolved_isSerializedOnce` | ❌ got 2 elements | ✅ |
| 2 | `normalAnnotator_unresolved_isSerializedOnce` (control) | ✅ | ✅ |
| 3 | `agreedMatch_isSerializedOnceAsAnnotation` (control) | ✅ | ✅ |
| 4 | `reopenAfterSave_doesNotDuplicate` | ❌ got 2 | ✅ |
| 5 | `reopenAfterSave_mixedSession_doesNotDuplicate` | ❌ got 5 of 4 | ✅ |
| 6 | `repeatedCycles_remainStable` | ❌ drifts at cycle 1 | ✅ |
| 7 | `fullLifecycle_twoAdjudicationSessions_noDuplicates` | ❌ got 4 of 3 | ✅ |
| 8 | `adjudicationStatus_survivesRoundTrip` | ❌ | ✅ |
| 9 | `annotationEqualsHashCodeContract` | ❌ | ✅ |

The two controls passing is what proves the tests are diagnostic rather than merely strict: they
isolate the failure to exactly the `annotator == ADJUDICATION` + unresolved combination.

### A.1.1 How the lifecycle test works

Test #7 reproduces the full user-reported sequence
(*open → adjudicate → save → close → reopen → adjudicate → save*) **headlessly**, with no GUI. The
technique matters because the obvious entry points all require a `userInterface.GUI` instance.

**Simulating each step**

| User action | In the test | Why |
|---|---|---|
| **open project** | `seedAndSave(doc, …)` — populate `AdjudicationDepot` with `Article` + `Annotation` objects | `Adjudication.checkAnnotations()` needs a GUI; the depot is the state it would produce |
| **adjudicate** | `adjudicateAccept()` / `adjudicateReject()` / `adjudicateCreateNew()` | The GUI mutates depot entries in place — see [Adjudication.java:1395](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/Adjudication.java:1395) (accept), [:1463](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/Adjudication.java:1463) (reject), [:1421](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/Adjudication.java:1421) (delete). The helpers reproduce those exact mutations |
| **save** | `new OutputToXML().directsave(txtFile)` | The real save path; writes both `saved/` and `adjudication/` |
| **close** | `new Depot().clear(); AdjudicationDepot.clear();` | Both depots are `static`, so clearing them *is* a process restart as far as the code can tell |
| **reopen** | `ImportAnnotation.XMLImporter(savedXmls)` | What [`Reload.extractAnnotation_fromXML`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/reloadSavedAnnotations/Reload.java:205) calls. `Reload.load()` itself takes a `GUI` and cannot be used |
| **continue adjudication** | `AdjudicationLoader.loadWorkingState()` | Exactly what [`GUI.mode_continuePreviousAdjudicationWork()`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/userInterface/GUI.java:10606) calls |

Two details make the simulation faithful rather than merely convenient:

- **`GUI.reviewmode = adjudicationMode`** is set in `@BeforeEach`. `directsave` only writes the
  `adjudication/` folder when this static flag is set ([OutputToXML.java:77](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:77)), so
  without it the test would silently exercise the wrong path.
- **`adjudicateCreateNew()` leaves `adjudicationStatus` at its default** and calls
  `setUnProcessed()`, mirroring [`AdjudicationDepot.addANewAnnotation`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/data/AdjudicationDepot.java:785). Setting it to
  `MATCHES_OK` instead — the intuitive choice — would sidestep the bug entirely and the test would
  pass against broken code.

**What it asserts.** Counting `AdjudicationDepot` entries across sessions, plus the raw element
count in the XML:

```java
seedAndSave(doc, fever, cough);        // 2 annotations from 2 annotators
adjudicateAccept(doc, "fever");        // → MATCHES_OK
adjudicateReject(doc, "cough");        // → NON_MATCHES
adjudicateCreateNew(doc, "chest pain", …);   // → ADJUDICATION, unresolved
save(doc);                             // 3 annotations on disk

restartAndResumeAdjudication();        // close + reopen + continue
assertEquals(3, adjudicationDepotCount(doc));   // ← FAILS TODAY: gets 4
```

The fourth entry is `chest pain` — the only one of the three matching
*`annotator == ADJUDICATION` **and** unresolved*, so the only one both writers claim. `fever`
(`MATCHES_OK`) and `cough` (rejected, normal annotator) each land in exactly one bucket. That is the
bug isolated to a single annotation inside an otherwise realistic session.

The test then runs a **third** session to prove convergence — because after a reopen the duplicated
state becomes the *input* to the next round, so the error compounds per session rather than staying
at one extra. Currently the test cannot get that far: it fails at session 2.

**Why the assertion is a count, not a diff.** Comparing XML text would be brittle (mention IDs are
regenerated on every save via `latestUsedMentionID`). Counting depot entries and XML child elements
is stable across runs and states the invariant directly: *N annotations in must equal N annotations
out.*

### A.2 Tests still to add before Phase B

- **`legacyFile_withoutStatus_stillLoads`** — build an `adjudication/*.knowtator.xml` in the *old*
  format (an `<annotation>` with no `<AdjudicationStatus>` child), load it, and assert it comes back
  as `MATCHES_OK`. This pins the backward-compatibility contract before step B.2 changes the writer.
- **`savedFolderOutput_isUnchanged`** — assert that `saved/<doc>.knowtator.xml` (annotation mode, the
  normal deliverable) is byte-identical before and after the change. The fix must not alter the
  non-adjudication save path.
- **`iaaReportPath_unaffected`** — `AdjudicationLoader.load()` (used by IAA reporting, distinct from
  `loadWorkingState()`) must still see the same set of final annotations.

### A.3 Baseline to capture first

Record the current full-suite result **before** touching `src/main`, so any new breakage is
attributable. Captured 2026-09-03:

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
    AdjudicationRoundTripTest ......................  8  ❌ 6 failures  ← the bug
    OutputToXMLTest ................................ 14  ✅
    ProjectLockTest ................................  6  ✅
```

**Every failure is in `AdjudicationRoundTripTest`; the other 74 tests pass.** The lifecycle test
(#7) was added after this baseline, taking that class to 9 tests / 7 failures and the suite to
81 / 7. After Phase B the expected result is `Tests run: 84, Failures: 0` (81 + the three new tests
from A.2).

---

## 5. Phase B — the fix

Five changes. B.1–B.3 are the fix proper and are **interdependent — do not ship them separately**;
B.4–B.5 remove the latent hazards that let the bug hide.

### B.1 Make the two writers disjoint

**File**: [`OutputToXML.java:367`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:367) in `addAdjudicatingAnnotations`

```java
// before
if (annotation.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK) {
    continue;
}

// after — skip whatever addAnnotations(root, true) already claimed
if (annotation.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK
        || "ADJUDICATION".equals(annotation.getFullAnnotator())) {
    continue;
}
```

> ⚠️ **Do not instead narrow the `addAnnotations(root, true)` filter to `MATCHES_OK` only.**
> Its `|| annotator == "ADJUDICATION"` clause is load-bearing:
> [`AdjudicationDepot.addANewAnnotation`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/data/AdjudicationDepot.java:785) calls only `setUnProcessed()` for
> adjudicator-authored annotations, which touches `isMatchingAnalysisForIAAProcessed` and **not**
> `adjudicationStatus` — so those annotations keep the field default `EXCLUDED`. Removing the clause
> would silently delete the adjudicator's own work from the final output.

### B.2 Persist the status on `<annotation>` too

**File**: [`OutputToXML.java:636`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:636) in `buildAnnotationNode`

`<processed>` and `<AdjudicationStatus>` are currently emitted only when
`outputAnnotationInMirrorMemeory` is true — i.e. only for `<adjudicating>`. After B.1,
`<annotation>` legitimately carries non-`MATCHES_OK` adjudicator work, so its real status must be
written. Remove the `if (outputAnnotationInMirrorMemeory)` guard around those two elements.

### B.3 Re-enable the status reader for `<annotation>`

**File**: [`ImportXML.java:283‑296`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/imports/ImportXML.java:283)

This is easy to miss. The `<annotation>` parsing loop (`root.getChildren("annotation")`, line 256)
has its status parsing **commented out**, while the `<adjudicating>` loop (line 363) has the
identical block **active** at line 390:

```java
// currently commented out in the <annotation> loop — must be restored
String adjudication_status = "NOBODY";
//Element element_adjudicationStatus = annotations.getChild("AdjudicationStatus");
//if( element_adjudicationStatus != null ){
//    adjudication_status = element_adjudicationStatus.getText();
//}
```

Uncomment both this and the `<processed>` block below it. **Without B.3, B.2 writes a status that
nothing ever reads** and the round trip still loses state.

For backward compatibility, keep the fallback: a missing `<AdjudicationStatus>` on an `<annotation>`
must still resolve to `MATCHES_OK`, since that is what every existing file implies.

### B.4 Stop inventing the status on load

**File**: [`AdjudicationLoader.java:206`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:206)

```java
ann.adjudicationStatus = Annotation.AdjudicationStatus.MATCHES_OK;   // remove
```

With B.2 + B.3 the real status now survives in the XML, so this hard-coded assignment becomes both
unnecessary and wrong. Apply the `MATCHES_OK` default only when the XML genuinely omits the field
(the legacy case pinned by test A.2).

### B.5 Fix the identity hazards

- **`Annotation.hashCode()`** — [`Annotation.java:1266`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/annotations/Annotation.java:1266) overrides `equals()` (value-based)
  with no matching `hashCode()`. Add one consistent with `equals()`'s fields
  (`annotationText` lower-cased, `annotationclass`, `annotator`, `creationDate`, `spanset`).
- **`AdjudicationLoader` `removeAll` calls** — [line 195](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:195) and
  [line 119](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:119) use value-equality, so they can remove annotations they never
  added. Switch to identity or `uniqueIndex` matching.
- **`seenAdjudicationKeys`** — [OutputToXML.java:281‑306](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:281) is a band-aid for the very
  duplication B.1 removes, and it silently collapses distinct annotations when `spanset` is null.
  Re-evaluate and most likely delete once B.1 holds — but only **after** the tests are green, as a
  separate commit.

---

## 6. Execution order

| Step | Action | Gate |
|---|---|---|
| 1 | Capture `mvn test` baseline | ✅ done — 81 tests, 7 failures, all in `AdjudicationRoundTripTest` |
| 2 | Add the three Phase A.2 tests | ✅ done — 12 tests / 8 failures; the two controls passed immediately, `legacyFile_withoutStatus_stillLoads` failed |
| 3 | Apply **B.1 + B.2 + B.3 together** | ✅ tests 1, 4, 5, 6, 7, 8 green |
| 4 | Apply B.4 | ✅ legacy-format test green |
| 5 | Apply B.5 (`hashCode`, `removeAll`) | ✅ test 9 green |
| 6 | Full `mvn test` | ✅ `Tests run: 86, Failures: 0` |
| 7 | Manual GUI verification (§7) | 🟡 partly automated — step 7 (upgrade path) is now a test; the GUI-only steps remain |
| 8 | Separately: remove `seenAdjudicationKeys` | ✅ removed; pinned by `identicalLookingAnnotations_areBothPreserved` |
| 9 | Heal folders written by the pre-fix build | ✅ added after §7 step 7 was found to be unmet — see §6.2 |

Steps 3–5 should be separate commits so any regression bisects cleanly.

### 6.1 Deviations from the plan as written

Three things turned out differently once the code was in front of us.

- **B.2 is scoped, not unconditional.** `buildAnnotationNode` is shared by the
  `saved/` writer, so simply deleting the `if (outputAnnotationInMirrorMemeory)` guard would have
  leaked `<processed>` / `<AdjudicationStatus>` into the plain annotation-mode deliverable. The guard
  became `outputAnnotationInMirrorMemeory || is_outputing_adjudicated_annotations`, which the A.2
  test `savedFolderOutput_isUnchanged` pins.
- **A fourth writer of duplicates had to go.** `ImportXML.readXMLContents` contained a "Backward
  Compatibility Fix" that synthesised an `<adjudicating>` twin for every `<annotation>` in an
  adjudication file that had none. That is precisely the legacy case B.3 + B.4 now handle natively,
  and leaving it in place made `legacyFile_withoutStatus_stillLoads` load one annotation as two. It
  was removed.
- **The legacy `MATCHES_OK` default lives in `AdjudicationLoader`, not `ImportXML`.** `ImportXML`
  parses faithfully and leaves the `"NOBODY"` sentinel when `<AdjudicationStatus>` is absent — the
  parser is shared with `saved/`, where defaulting to `MATCHES_OK` would change unrelated behaviour.
  `loadWorkingState()` rewrites that sentinel to `MATCHES_OK` for `<annotation>` nodes only, which
  is exactly the documented backward-compatibility contract and nothing wider.

`seenAdjudicationKeys` (step 8) was deleted rather than kept: after B.1 it can no longer prevent a
duplicate — `addAnnotations(root, true)` emits at most one element per depot entry — so its only
remaining effect was to silently discard adjudicator work that happened to look alike, violating the
very *N in == N out* invariant this fix establishes.

### 6.2 Step 9 — the upgrade path (a gap the plan under-specified)

§7 step 7 asks for a legacy check against *"an `adjudication/` folder produced by the current
build"*. The A.2 test `legacyFile_withoutStatus_stillLoads` does **not** satisfy this: it exercises
the *ancient* format (a lone status-less `<annotation>`), whereas the pre-fix build writes the
**duplicate pair**. Those are different inputs, and only the second is what real users have on disk.

Captured verbatim from the pre-fix writer, an adjudicator-authored unresolved annotation is stored as:

```xml
<annotation>                       <!-- no <AdjudicationStatus> -->
    <spannedText>chest pain</spannedText>
</annotation>
<adjudicating>                     <!-- the same annotation again -->
    <spannedText>chest pain</spannedText>
    <AdjudicationStatus>NON_MATCHES</AdjudicationStatus>
</adjudicating>
```

B.1–B.5 alone did **not** heal this. Resuming produced `[chest pain, cough, chest pain, fever]` — the
duplicate survives the upgrade, and because the fixed writer routes *both* copies to the
`<annotation>` side it would then persist through every subsequent save. The fix would have shipped
looking green while leaving every in-flight project permanently broken.

`AdjudicationLoader.healLegacyNodes()` closes this. A status-less `<annotation>` in the adjudication
folder can only come from an older build, and the presence of an `<adjudicating>` twin
(matched on span + text + annotator + creation date, since mention ids are regenerated on every save)
distinguishes the two legacy shapes:

| Legacy shape | Twin? | Action |
|---|---|---|
| final result from a pre-status build | no | default to `MATCHES_OK` (the A.2 contract) |
| the double-write defect | yes | drop the status-less `<annotation>`; the twin holds the true status |

Pinned by `preFixDuplicatePair_healsOnResume`, which asserts the collapse, the preserved
`NON_MATCHES`, and that the next save rewrites the file in the healed one-element-per-annotation
form.

---

## 7. Manual verification

Automated tests cover the data round trip, not the UI. After the suite is green:

1. Open a project with annotations from ≥ 2 annotators; enter adjudication mode.
2. Adjudicate so at least one annotation ends up authored by `ADJUDICATION` **and** unresolved.
3. Save. Inspect `adjudication/<doc>.txt.knowtator.xml` — that annotation must appear **once**.
4. Close eHOST, reopen, choose *continue previous adjudication*.
5. The adjudication view shows **one** entry, with its original status intact.
6. Save again; re-inspect. Still one element — no growth across cycles.
7. **Legacy check**: repeat 4–6 against an `adjudication/` folder produced by the *current* build,
   confirming pre-existing in-progress work still loads.
   *Now also covered automatically by `preFixDuplicatePair_healsOnResume` (§6.2), which resumes
   against a verbatim capture of the pre-fix writer's output. This step originally revealed that
   B.1–B.5 alone left the duplicate intact — keep it in the manual pass as a sanity check.*
8. Generate an IAA report and confirm adjudication results still appear.
9. **Overlap check** (added after §7.3): have both annotators tag one span under two classes, enter
   adjudication, and confirm *both* render as adjudicated — neither should carry a disagreement
   underwave. Adjust one span boundary, save, and confirm the file holds exactly the annotations the
   editor shows.

> **Status: done.** Steps 1–9 were walked through in the GUI. Steps 1–8 passed; step 9 failed twice
> and produced the two defects fixed in §7.3. A re-run after those fixes was confirmed correct by the
> reporter.
>
> ⚠️ Run the manual pass against a **copy** of a project, never `src/test/resources/` — see the second
> bullet in §7.2 for what happened when that rule was broken.

### 7.1 Headless end-to-end coverage over real projects

`src/test/java/adjudication/TwoAnnotatorProjectAdjudicationTest.java` (12 tests) now drives steps
2–8 without a display, over two *real* eHOST project directories rather than a hand-seeded depot:

- `testsupport/EhostProjectFixture` writes a `config/ corpus/ saved/` project with a two-document
  clinical corpus, then the whole directory is **copied** and re-annotated as a second annotator —
  the artefacts a real two-annotator study produces. Spans are resolved with `indexOf` against the
  document text, so every `<span>` provably covers its own `<spannedText>`.
- The two annotator sets cover every relationship the comparison engine distinguishes: exact
  agreement, partial span overlap, same span with a different class, and single-annotator spans.
- The pipeline is eHOST's own: `ImportAnnotation.XMLImporter` → `AdjudicationDepot.copyAnnotations`
  → `Adjudication.searchDifferenceinArticle` → `OutputToXML.directsave` →
  `AdjudicationLoader.loadWorkingState`. Adjudicator actions call the production mutators
  (`Depot.setAnnotationToMatchedOK_byUID`, `deleteAnnotation_byUID_onAdjudicationMode`,
  `AdjudicationDepot.deleteAnnotation_byUID`) rather than assigning fields, so the simulated
  session behaves like `data_onlyKeepPrimaryAnnotation` does.
- The core assertion is that the XML on disk is an exact multiset image of the in-memory working
  set — the precise anti-duplication invariant, rather than a heuristic duplicate scan.

**Verified against the broken code**: reverting the four fixed source files to `70606f4` makes 4 of
the 12 fail, including `substernal chest pain` appearing twice in one file and the `MATCHES_OK`
count drifting 7 → 8 across a restart. The suite therefore detects the original defect rather than
merely passing alongside it.

Two behaviours this exercise pinned down, both pre-existing and both worth knowing before the GUI
pass, because either could otherwise be mistaken for a duplicate:

- An **exact agreement is auto-resolved by the comparison engine**, not by the adjudicator: one copy
  becomes `MATCHES_OK`, its partner `MATCHES_DLETED`. Both are retained in the working set. Only the
  accepted copy is persisted (§7.3), so what must hold across a restart is that the agreement stays
  settled — exactly one copy returns, still `MATCHES_OK`
  (`autoResolvedMatch_survivesRestart`).
- `addAnnotations(root, true)` **relabels every final `<annotation>` in `adjudication/` as
  `ADJUDICATION`**, whoever authored it, and `setAnnotationToMatchedOK_byUID` re-attributes an
  accepted annotation the same way. Original authorship is therefore not recoverable from the
  adjudication folder for resolved annotations — only `<adjudicating>` entries keep their author.
  Unchanged by this fix; noted because it makes two accepted annotations indistinguishable by
  annotator name.

What step 7's manual pass still adds is the one thing no test covers: how the adjudication view
*renders* what it loads — which is exactly what turned up the two defects in §7.3.

### 7.2 First report: 2 `<annotation>` + 2 `<adjudicating>`

> ⚠️ **The conclusion below was wrong** — see §7.3. The file was *internally consistent*, but it
> should never have contained the two `<adjudicating>` entries, and the state that produced it was
> itself the result of a matching bug. Kept because the two side findings still stand.

The manual pass raised `proj2/adjudication/doc3.txt.knowtator.xml`: after editing one of two
overlapping annotations and accepting both, the editor showed two annotations but the file held four
entries. Both annotators had tagged the *same span* under *two different classes*, so adjudication
carried four annotations:

| Entry | Annotator | Class | Status | Shown in editor |
|---|---|---|---|---|
| `<annotation>` | `ADJUDICATION` | CONCEPT | `MATCHES_OK` | yes (edited span) |
| `<annotation>` | `ADJUDICATION` | CON2 | `MATCHES_OK` | yes |
| `<adjudicating>` | `a2` | CONCEPT | `MATCHES_DLETED` | no |
| `<adjudicating>` | `a2` | CON2 | `MATCHES_DLETED` | no |

The two writers are disjoint by construction (§B.1), so four annotations produced four elements, and
`GUI.reloadAnnotationsToScreen` skips `*_DLETED`, so the editor painted two. That much was accurate.
What this analysis missed is that an absorbed partner has no reason to be on disk at all: the
argument that "the tombstones let a resume rebuild the pairing" does not hold, because a settled
agreement has no pairing left to rebuild. §7.3 removes them.

Two real problems surfaced while confirming it, and both stand:

- **The legacy healer could drop a genuine annotation.** `legacyTwinKey` identified an annotation by
  span + text + annotator + `creationDate` — none of which separates two annotations that share a
  span and differ only by class. `creationDate` has one-second resolution, so tagging one span twice
  in quick succession collides, and a status-less legacy final would be silently discarded as a
  "duplicate". The key now includes the class, resolved through `classMentions`. Reverting just that
  change fails `legacyHealer_keepsDifferentClassesOnTheSameSpan` (working set 1 instead of 2).
- **The manual run overwrote committed fixtures.** Running the GUI against `src/test/resources/proj2`
  rewrote its `saved/`, `adjudication/` and `reports/` files in place (commit `a0d226f`), dropping the
  `att1`/`att2` attribute data that `IAACalculationTest` pins its expected counts to, and leaving 11
  tests red at HEAD. The fixture has been restored to its `e7e838f` contents. **Do not point a manual
  eHOST session at `src/test/resources/`** — copy the project elsewhere first. New tests should build
  their own fixture in a temp directory, as `OverlappingClassAdjudicationTest` now does, rather than
  reading a shared one other tests depend on.

### 7.3 The follow-up: agreements shown as disagreements, and the file the editor disagreed with

The conclusion in §7.2 — that four elements were correct — held for the *writer*, but a second manual
pass showed the layout was a symptom of two deeper problems.

**Agreements were reported as disagreements (pre-existing, `Adjudication.java`).** In
`searchDifferenceinArticle`, an overlapping annotation that failed the class/attribute comparison set
`foundDifference = true`, which marked the whole candidate group `NON_MATCHES`. So when both
annotators tagged one span under two classes — two agreements — the first pair resolved and the
second was drawn with a disagreement underwave, though the annotators had written the same thing.

An annotation that overlaps but compares differently is simply a *different* annotation, not evidence
that this one is disputed. Whether the annotators agreed is already decided further down by
`checkAnnotators()`, which requires every selected annotator to be represented among the matches;
the short-circuit pre-empted it. The flag is now gone. Genuine disagreements still surface through
`checkAnnotators()`:

| Fixture | Before | After |
|---|---|---|
| a1{CONCEPT}, a2{CONCEPT} | OK + DLETED | unchanged |
| a1{CONCEPT, CON2}, a2{CONCEPT, CON2} | OK + DLETED + **2 NON_MATCHES** | OK×2 + DLETED×2 |
| a1{CONCEPT}, a2{CON2} | 2 NON_MATCHES | unchanged |
| a1{CONCEPT, CON2}, a2{CONCEPT} | — | OK + DLETED + NON_MATCHES (a1's extra CON2) |

Verified against `70606f4`, so this predates the duplicate fix. `OverlappingAgreementMatchingTest`
covers both directions; reverting only `Adjudication.java` fails 3 of its 7 tests.

**Absorbed partners are no longer persisted (`OutputToXML.addAdjudicatingAnnotations`).**
`MATCHES_DLETED` marks the partner an *agreed* match absorbed. It is derived entirely from the
surviving `MATCHES_OK` annotation, is hidden by `GUI.reloadAnnotationsToScreen`, and carries no
decision — so writing it made the adjudication file hold twice what the editor showed, which is what
looked like duplication. It is now skipped. Rejections (`NONMATCHES_DLETED`) and open disagreements
(`NON_MATCHES`) are real decisions and are still written.

This is safe because `loadWorkingState()` rebuilds `AdjudicationDepot` **only** from the adjudication
folder, so a partner that is not written simply stays absent — it cannot resurface as a fresh
disagreement. Older files that still contain the partners load fine and are normalised on the next
save. Re-running the comparison repopulates everything from `saved/` regardless.

---

## 8. Risks

| Risk | Mitigation |
|---|---|
| Breaking resume for in-flight adjudication folders | Two distinct legacy shapes, both handled by `healLegacyNodes()` and pinned by `legacyFile_withoutStatus_stillLoads` (no twin → `MATCHES_OK`) and `preFixDuplicatePair_healsOnResume` (twin → collapse). See §6.2 |
| Repeating the EHOST-001 → EHOST-003 regression | Both element types are retained; only the *filters* change. Tests 4–6 are the direct guard |
| Adjudicator-authored work silently dropped | B.1 changes the `<adjudicating>` side, never the `<annotation>` side — see the warning in B.1 |
| `hashCode()` altering behaviour elsewhere | `Annotation` is not currently used as a hash key anywhere; verify with a usage search before committing |
| Adjudication XML file size growing | Neutral: one element is removed per overlapping annotation, two small child elements added per `<annotation>` |
| Dropping `MATCHES_DLETED` losing state (§7.3) | `loadWorkingState()` rebuilds `AdjudicationDepot` only from `adjudication/`, so an unwritten partner stays absent rather than resurfacing as a new disagreement. The accepted result it was absorbed into is still written. Pinned by `save_writesOnlyTheAcceptedResults` and `layoutIsStableAcrossResume` |
| Relaxing the match rule hiding real disagreements (§7.3) | `checkAnnotators()` still requires every selected annotator to be represented. `OverlappingAgreementMatchingTest` asserts both directions — class disagreements, single-annotator findings and one-sided extra classes all remain `NON_MATCHES` |

---

## 9. Out of scope

- The UMLS / CUI work in the `chrisleng` fork ([fork review §4.2](./fork-chrisleng-netbeans-review.md)).
- The dead `adjudicationParameters()` / `getAdjudicationSetting()` methods (fork review §4.9) — they
  are related but independent; resolve separately.
- Any change to the `saved/` (annotation-mode) output path.

---

## 10. Final state

| | |
|---|---|
| Test suite | 111 tests, 0 failures (`mvn test`) |
| Source files changed | `Adjudication.java`, `ImportXML.java`, `AdjudicationLoader.java`, `Annotation.java`, `OutputToXML.java` |
| Defects fixed | double-written annotation (§1); legacy twin key ignoring class (§7.2); overlapping agreements reported as disagreements (§7.3); absorbed partners persisted (§7.3) |
| Manual GUI verification | §7 steps 1–9, confirmed by the reporter |

Each fix is guarded by a test that fails when that fix alone is reverted — verified for all four.
