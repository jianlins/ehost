# Adjudication duplicate annotations — fork check + root-cause analysis

**Date**: 2026-09-02
**Status**: 🔍 Analysis — no code changes made
**Question**: does `chrisleng/ehost` contain adjudication comparison/saving fixes we could adopt to
solve duplicate annotations on *reopen → continue adjudication → save*?
**Fix plan**: [adjudication-duplicate-fix-plan.md](./adjudication-duplicate-fix-plan.md)
· [中文版](./adjudication-duplicate-fix-plan.zh-CN.md)

---

## 1. Short answer on the fork

**No. The fork contains nothing useful for this.** Do not spend more time on it for this problem.

Evidence:

| Check | Result |
|---|---|
| `adjudication/**` file list, BASE (`4d06899`) vs FORK | **Identical** — same 8 files, no additions |
| `adjudication/data/AdjudicationDepot.java` | 1 changed line: `AdjudicationDepot.getArticleByFilename` → `adjudication.data.AdjudicationDepot.getArticleByFilename`. Name qualification only. |
| `adjudication/parameters/Paras.java` | **Zero** differences |
| `adjudication/statusBar/DiffCounter.java` | One `@Override` added |
| `imports/importedXML/eXMLFile.java` | **Zero** differences |
| `output/xmloutput/WriteToXML.java` | **Zero** differences |
| `resultEditor/save/OutputToXML.java` | Only the `SaveDialog.outputMainBodyOnly` guards (see fork review §4.4) + name qualification |

The fork's last source commit is **2018‑07‑09**. Every piece of adjudication engineering in our repo —
`docs/enhancements/005-adjudication-resume-robustness.md`, `007-simplify-adjudication-detection.md`,
`008-adjudication-xml-optimization.md`, `009-adjudication-restart-warning.md`, plus
`AdjudicationLoader` (which does not exist in the fork at all) — postdates it. **We are strictly ahead
of the fork here.** The duplicate bug is in our own newer code, not something the fork ever solved.

---

## 2. Root cause: one annotation is serialized twice, then read back as two

### 2.0 The two element types are *roles*, not alternatives

`adjudication/<doc>.txt.knowtator.xml` contains two different element types that serve two
different purposes and have **two different consumers**:

| Element | Means | Written by | Read by |
|---|---|---|---|
| `<annotation>` | **final adjudicated result** — the deliverable | `addAnnotations(root, true)` | IAA reporting, via `AdjudicationLoader.load()`, which explicitly *strips* `type == 5` first ([AdjudicationLoader.java:64‑67](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:64)) |
| `<adjudicating>` | **in-progress working state** — so a session can resume | `addAdjudicatingAnnotations(root)` | resume, via `AdjudicationLoader.loadWorkingState()` |

Both are genuinely required. EHOST-001 tried deleting the `<adjudicating>` writer outright and that
caused EHOST-003 (total loss of adjudication state on restart). **The fix is not to pick one.** It is
to make the two *selection rules* disjoint, so every annotation lands in exactly one of them.

### 2.1 The two writers overlap

When saving in adjudication mode, `OutputToXML.buildxml(XMLfile, true)` writes
`adjudication/<doc>.txt.knowtator.xml` using **two** passes over the *same* `AdjudicationDepot` article:

**Pass 1** — [`addAnnotations(root, true)`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:273):

```java
if ((annotation.adjudicationStatus != Annotation.AdjudicationStatus.MATCHES_OK)
        && (annotation.getFullAnnotator().compareTo("ADJUDICATION") != 0)) {
    continue;
}
```

→ writes `<annotation>` when `status == MATCHES_OK` **OR** `annotator == "ADJUDICATION"`.

**Pass 2** — [`addAdjudicatingAnnotations(root)`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:367):

```java
if (annotation.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK) {
    continue;
}
```

→ writes `<adjudicating>` when `status != MATCHES_OK`.

**These two conditions are not mutually exclusive.** Their intersection is:

> `status != MATCHES_OK` **AND** `annotator == "ADJUDICATION"`

which is precisely **an annotation the user created or edited during adjudication that is not yet
marked as an agreed match** — i.e. the most common product of real adjudication work.

Such an annotation is written **twice into the same file**: once as `<annotation>`, once as
`<adjudicating>`.

### 2.2 The reader turns those two elements into two in-memory annotations

On reopen → *continue previous adjudication*,
[`GUI.mode_continuePreviousAdjudicationWork()`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/userInterface/GUI.java:10606)
calls [`AdjudicationLoader.loadWorkingState()`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:149), and
[`ImportAnnotation.XMLExtractor`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/annotations/ImportAnnotation.java:130) routes by element type:

| XML element | `type` | Destination |
|---|---|---|
| `<annotation>` | ≠ 5 | regular `Depot` → then copied into `AdjudicationDepot` by `loadWorkingState` |
| `<adjudicating>` | 5 | `AdjudicationDepot` directly, status preserved |

So the single logical annotation from §2.1 comes back as **two separate `AdjudicationDepot` entries**.

### 2.3 …with contradictory statuses, because status is not persisted on `<annotation>`

[`buildAnnotationNode`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:636) only emits the status fields for mirror-memory (`<adjudicating>`) output:

```java
if (outputAnnotationInMirrorMemeory) {
    // <processed> and <AdjudicationStatus> written here — and ONLY here
}
```

`addAnnotations` passes `false` for that flag, so **`<annotation>` elements in `adjudication/` carry
no `<AdjudicationStatus>`**. `loadWorkingState` therefore has to guess, and hard-codes it
([AdjudicationLoader.java:206](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:206)):

```java
ann.adjudicationStatus = Annotation.AdjudicationStatus.MATCHES_OK;
```

Net effect for the overlapping annotation:

| Entry | Origin | Annotator | Status after load |
|---|---|---|---|
| A | `<annotation>` | `ADJUDICATION` | **forced** `MATCHES_OK` |
| B | `<adjudicating>` | `ADJUDICATION` | real status (e.g. `NON_MATCHES`) |

One annotation in, **two out**, disagreeing about their own adjudication state. This is what surfaces
as duplicates in the adjudication view.

### 2.4 Why the existing dedup band-aid does not catch it

`addAnnotations` has a `seenAdjudicationKeys` guard
([OutputToXML.java:281‑306](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/save/OutputToXML.java:281)) keyed on span + class + text + attributes.
`addAdjudicatingAnnotations` has **no dedup at all**. So the guard suppresses a second `<annotation>`
but never a redundant `<adjudicating>`. It also silently collapses genuinely distinct annotations
whenever `spanset` is null, since the key then omits spans entirely.

### 2.5 Visual comparison — current vs proposed

#### Current (broken) — the save path

Two independent passes walk the *same* list, and their conditions overlap:

```
                        AdjudicationDepot (in memory)
                     ┌────────────────────────────────────┐
                     │ Annotation                         │
                     │   status    = NON_MATCHES          │
                     │   annotator = ADJUDICATION         │
                     │ (adjudicator's own unresolved work)│
                     └─────────────────┬──────────────────┘
                                       │
                 the SAME annotation is visited by BOTH passes
                 ┌─────────────────────┴─────────────────────┐
                 ▼                                           ▼
  ┌──────────────────────────────────┐  ┌──────────────────────────────────┐
  │ PASS 1                           │  │ PASS 2                           │
  │ addAnnotations(root, true)       │  │ addAdjudicatingAnnotations(root) │
  ├──────────────────────────────────┤  ├──────────────────────────────────┤
  │ write when:                      │  │ write when:                      │
  │   status == MATCHES_OK           │  │   status != MATCHES_OK           │
  │   OR annotator == ADJUDICATION   │  │                                  │
  └────────────────┬─────────────────┘  └────────────────┬─────────────────┘
                   │ YES ✓                               │ YES ✓
                   ▼                                     ▼
  ┌──────────────────────────────────┐  ┌──────────────────────────────────┐
  │ <annotation>                     │  │ <adjudicating>                   │
  │   annotator forced "ADJUDICATION"│  │   status = NON_MATCHES        ✓  │
  │   NO <AdjudicationStatus>     ✗  │  │                                  │
  └────────────────┬─────────────────┘  └────────────────┬─────────────────┘
                   └──────────────────┬──────────────────┘
                                      ▼
                    adjudication/doc.txt.knowtator.xml
                 ***  2 elements written for 1 annotation  ***
```

#### Current (broken) — the reload path

Those two elements are routed to two different places, and one of them has to invent a status:

```
                  adjudication/doc.txt.knowtator.xml
                     │                          │
                     │ type != 5                │ type == 5
                     ▼                          ▼
              <annotation>                 <adjudicating>
                     │                          │
                     ▼                          │
              regular Depot                     │
                     │                          │
                     │ loadWorkingState()       │ status read
                     │ copies it across AND     │ straight from
                     │ HARD-CODES the status    │ the XML
                     ▼                          ▼
           ┌──────────────────┐       ┌──────────────────┐
           │ Entry A          │       │ Entry B          │
           │ MATCHES_OK       │       │ NON_MATCHES      │
           │ ⚠ invented       │       │ ✓ real           │
           └────────┬─────────┘       └────────┬─────────┘
                    └───────────┬──────────────┘
                                ▼
                        AdjudicationDepot
              ***  2 entries, conflicting status  ***
              →  this is the duplicate the user sees
```

#### Proposed — one router, status always persisted

```
                        AdjudicationDepot (in memory)
                                     │
                                     ▼
              ┌────────────────────────────────────────────┐
              │  SINGLE ROUTER — one either/or decision    │
              │                                            │
              │     status == MATCHES_OK                   │
              │     OR annotator == ADJUDICATION  ?        │
              └──────────┬──────────────────────┬──────────┘
                     YES │                      │ NO
             "final result"                     │ "still in progress"
                         ▼                      ▼
           ┌──────────────────────┐  ┌──────────────────────┐
           │ <annotation>         │  │ <adjudicating>       │
           │   + status  ✓ (new)  │  │   + status  ✓        │
           └───────────┬──────────┘  └──────────┬───────────┘
                       └───────────┬────────────┘
                                   ▼
                 adjudication/doc.txt.knowtator.xml
              ***  exactly 1 element per annotation  ***
                                   │
                                reload
                                   ▼
                          AdjudicationDepot
              ***  1 entry, real status preserved  ***
```

The only behavioural change is that `addAdjudicatingAnnotations` stops re-claiming what Pass 1
already took — turning two overlapping filters into a single either/or decision.

#### Side-by-side routing table

| Status | Annotator | Current | Proposed |
|---|---|---|---|
| `MATCHES_OK` | any | `<annotation>` | `<annotation>` |
| not `MATCHES_OK` | `ADJUDICATION` | 🔴 **`<annotation>` + `<adjudicating>`** | `<annotation>` |
| not `MATCHES_OK` | anyone else | `<adjudicating>` | `<adjudicating>` |

Only the middle row changes. See §4 for the exact code change and why the fix must go on the
`<adjudicating>` side rather than the `<annotation>` side.

---

## 3. Contributing defects found along the way

### 3.1 🔴 `Annotation` overrides `equals()` but not `hashCode()`

[Annotation.java:1266](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/resultEditor/annotations/Annotation.java:1266) implements value-based `equals()` (compares
`annotationText` case-insensitively, `annotationclass`, `annotator`, `creationDate`, `spanset`).
There is **no `hashCode()`** anywhere in the class.

This breaks the `Object` contract. Concretely, in this code path:
- `List.contains` / `Vector.removeAll` → **value** semantics
- any `HashSet<Annotation>` / `HashMap<Annotation,…>` → **identity** semantics

so two pieces of dedup logic sitting next to each other disagree about what "the same annotation" means.

### 3.2 🟠 `loadWorkingState()` computes "newly added" with value-equality

[AdjudicationLoader.java:193‑195](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:193):

```java
Vector<Annotation> newlyAdded = new Vector<>(depotArticle.annotations);
newlyAdded.removeAll(originalAnnotations);
```

`Vector.removeAll` removes **every** element value-equal to anything in `originalAnnotations`. Once an
`ADJUDICATION`-annotator annotation exists in `saved/` (so it is already in the Depot), the matching
one just imported from `adjudication/` is value-equal → dropped from `newlyAdded` → **never restored
into `AdjudicationDepot`**. That is a silent *loss* of adjudication state, the mirror image of the
duplication above, and likely explains any "some decisions came back, others vanished" reports.

### 3.3 🟠 `cleanup()` can delete genuine user annotations

[AdjudicationLoader.java:119](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/AdjudicationLoader.java:119) removes injected IAA annotations with
`article.annotations.removeAll(loadedAnnotations)` — again value-based. Any real annotation whose
annotator happens to be `"Adjudication"` is indistinguishable and gets deleted from the Depot.

Note this path (`AdjudicationLoader.load()` / `cleanup()`) is only used by IAA report generation
([IAA.java:245](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/report/iaaReport/IAA.java:245)), and it *is* correctly wrapped in `try/finally`. So it is not the
resume-path bug — but it is the same latent hazard.

### 3.4 🟡 Two dead adjudication methods

`OutputToXML.adjudicationParameters(...)` and `ImportXML.getAdjudicationSetting(...)` are both defined
and never called (see fork review §4.9). The former means `<eHOST_Adjudication_Status>` is no longer
written, so `Paras` must be rebuilt heuristically by `rebuildParasFromAnnotations()` on resume — which
infers the annotator/class selection from whatever annotations happen to be loaded rather than from
what the user actually chose.

---

## 4. Recommended fix

The invariant to restore is simple:

> **Every `AdjudicationDepot` annotation must be serialized to `adjudication/*.knowtator.xml`
> exactly once, and must round-trip with its status intact.**

Proposed, in priority order:

1. **Make the two writers disjoint.** Change the `addAdjudicatingAnnotations` filter to skip
   `annotator == "ADJUDICATION"` as well as `status == MATCHES_OK`:

   ```java
   // addAdjudicatingAnnotations — write only what addAnnotations did NOT write
   if (annotation.adjudicationStatus == MATCHES_OK
           || "ADJUDICATION".equals(annotation.getFullAnnotator())) {
       continue;
   }
   ```

   This yields a total, disjoint partition of every `AdjudicationDepot` annotation:

   | Status | Annotator | Element |
   |---|---|---|
   | `MATCHES_OK` | any | `<annotation>` |
   | not `MATCHES_OK` | `ADJUDICATION` | `<annotation>` |
   | not `MATCHES_OK` | anyone else | `<adjudicating>` |

   > ⚠️ **Do not instead narrow the `addAnnotations(root, true)` filter to `MATCHES_OK` only.**
   > The `|| annotator == "ADJUDICATION"` clause there is **load-bearing**. `AdjudicationDepot`'s
   > [`addANewAnnotation`](/c:/Users/VHASLCShiJ/Projects/IntelliJProjects/ehost/src/main/java/adjudication/data/AdjudicationDepot.java:785) calls only `setUnProcessed()` for adjudicator-authored
   > annotations, which touches `isMatchingAnalysisForIAAProcessed` and **not**
   > `adjudicationStatus` — so those annotations keep the field default `EXCLUDED`. Dropping the
   > clause would silently delete adjudicator-authored annotations from the final output.

2. **Persist the status on both element types.** Emit `<processed>` / `<AdjudicationStatus>`
   unconditionally in `buildAnnotationNode`, and delete the `= MATCHES_OK` force in
   `loadWorkingState`. Guessing state that was thrown away at write time is the underlying design
   flaw. This becomes *more* important under fix 1, since `<annotation>` now legitimately carries
   non-`MATCHES_OK` adjudicator work whose real status must survive.

3. **Give `Annotation` a stable identity and dedup on it.** `uniqueIndex` already exists — use it.
   Replace the `removeAll` calls in `AdjudicationLoader` with identity- or `uniqueIndex`-based
   removal, and replace `seenAdjudicationKeys` with a `uniqueIndex` set.

4. **Add `hashCode()`** consistent with the existing `equals()`, or explicitly document `Annotation`
   as identity-compared and remove the `equals()` override. Either is fine; the current state is not.

5. **Then re-evaluate** whether `seenAdjudicationKeys` (OutputToXML.java:281‑306) is still needed. If
   steps 1–3 hold, it is masking a bug that no longer exists and should be removed — it currently
   risks collapsing distinct annotations that share a span.

⚠️ Steps 1 and 2 change the on-disk `adjudication/` format. Existing in-progress adjudication folders
must still load: keep reading `<annotation>`-without-status as `MATCHES_OK` for backward
compatibility, and treat a missing `<AdjudicationStatus>` as that default.

---

## 5. Suggested reproduction test

No automated coverage exists for this round-trip. Before changing anything, pin the current behaviour:

1. Open a project with annotations from ≥ 2 annotators; enter adjudication mode.
2. Adjudicate so that at least one annotation ends with `annotator == "ADJUDICATION"` and
   `status != MATCHES_OK` (edit/create an annotation, leave it unresolved).
3. Save. Inspect `adjudication/<doc>.txt.knowtator.xml` and **count elements for that annotation** —
   the bug predicts one `<annotation>` *and* one `<adjudicating>` with the same span/class/text.
4. Close eHOST, reopen, choose *continue previous adjudication*.
5. The adjudication view should now show **two** entries for that annotation, with different statuses.
6. Save again and re-inspect the XML.

A unit-level equivalent is feasible without the GUI: populate `AdjudicationDepot` directly, call
`OutputToXML`, parse the result with `ImportXML.readXMLContents`, feed it through
`AdjudicationLoader.loadWorkingState()`, and assert `AdjudicationDepot` size is unchanged across the
round trip. That assertion is the real regression guard, and it should fail today.

---

## 6. Related existing docs

- `docs/bugs/EHOST-001-duplicate-adjudication-elements.md` — earlier duplicate report; its fix was
  **reverted** because removing `addAdjudicatingAnnotations()` wholesale caused EHOST-003.
- `docs/bugs/EHOST-003-adjudication-resume-failure.md` — the regression that reverted EHOST-001.
- `docs/enhancements/005-adjudication-resume-robustness.md`, `008-adjudication-xml-optimization.md`.

The analysis above supersedes EHOST-001's diagnosis: the problem was never `<annotation>` vs
`<adjudicating>` being duplicates *in principle* (EHOST-003 proved both are needed). It is that the
**two writers' filters overlap**, so a specific subset of annotations lands in both — and that
`<annotation>` loses its status on the way out, forcing the reader to invent one.
