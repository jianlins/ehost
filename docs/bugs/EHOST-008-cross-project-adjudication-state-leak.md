# Bug Report: Adjudication State Leaks Between Projects That Share Document Filenames

## Bug ID
EHOST-008

## Summary
`AdjudicationDepot` and `Paras` are process-wide static stores that were never reset when the user switched projects. Opening a second project that shares document filenames — a copy of the same corpus — and choosing "continue your previous adjudication work" resumed on the *previous* project's adjudication data. Saving from that session then wrote the wrong project's rulings into the open project's `adjudication/` folder.

## Severity
High — silent cross-project data loss. No dialog, no log warning, and the corrupted state is persisted on the next save.

## Environment
- eHOST version: all versions with the adjudication resume prompt, prior to fix
- Java version: 8
- OS: All

## Description

### Root Cause
Three separate gaps combined:

1. `NavigationManager.goBackToProjectList()` released the project lock, switched the navigator tab and nulled `CurrentProject`, but never cleared `AdjudicationDepot`.

2. `NavigationManager.selectProject()` opened the new project and called `Reload.load()`, whose first act is `clearAnnotationDepot()`. That resets the regular `resultEditor.annotations.Depot` only — the adjudication side was left holding the previous project's data.

3. `GUI.mode_continuePreviousAdjudicationWork()` gates the on-disk reload behind a bare emptiness check:

   ```java
   if (!adjudication.data.AdjudicationDepot.isReady()) {
       if (!report.iaaReport.AdjudicationLoader.loadWorkingState()) {
           ...
       }
   }
   ```

   `AdjudicationDepot.isReady()` is just `depotOfAdj.size() >= 1`. It carries no notion of which project the contents came from — `AdjudicationDepot` holds no reference to `CurrentProject` anywhere in the class. With stale data still present the check passed, so `AdjudicationLoader.loadWorkingState()` — the only code that reads the open project's `adjudication/` folder — was skipped entirely.

`AdjudicationDepot` articles are keyed purely by document filename, so with two copies of a project every filename matched and nothing downstream detected the mismatch. `Paras` (selected annotators and classes) leaked the same way, guarded by the parallel `Paras.isReadyForAdjudication()` check.

This is the same misuse of `isReady()` as a proxy for state that was noted in EHOST-004.

### Symptoms
With two project copies sharing filenames (a routine setup — each annotator is handed a copy of the same corpus):

1. Findings settled in the open project silently disappear from the adjudicated set.
2. Findings settled in the *other* project appear as already adjudicated.
3. A shared finding can display the other project's class on a real annotation.
4. Continuing to work and pressing Save overwrites the open project's `adjudication/` folder with the other project's rulings.

### Reproduction Steps
1. Create a project `A` with a corpus and two annotators' annotations.
2. Copy the whole project directory to `B`, so both share the same document filenames.
3. Open `A`, enter Adjudication Mode, settle some findings, and save.
4. Open `B`, enter Adjudication Mode, settle a *different* set of findings, and save.
5. Click "Go back to project list ...".
6. Re-open project `A` and enter Adjudication Mode.
7. Answer "Yes, please" to "Would you like to continue your previous adjudication work?".

Expected: A's own adjudication work is restored from `A/adjudication/`.

Actual: B's adjudication state is shown under A's documents.

### Why the other branch was no safer
Choosing "No, Start a new adjudication" did call `AdjudicationDepot.clear()`, which fixed the staleness — but it also calls `clearAdjudicationFiles()`, which deletes the open project's real `.knowtator.xml` adjudication files. So neither answer at the prompt recovered the project's true state once another project's session had polluted the depot.

## Fix
Added `NavigationManager.discardAdjudicationState()`, which clears `AdjudicationDepot` and resets `Paras`. It runs on both project-switching boundaries:

- `selectProject()` — after the pending auto-save, before the project's own config and annotations are loaded. This also covers the REST API, which switches project without passing through the project list.
- `goBackToProjectList()` — after the save prompt, once no project is open.

Both stores are repopulated from the open project's own files afterwards: `Paras` by `ImportXML.getAdjudicationSetting()` during import, the depot by `AdjudicationLoader.loadWorkingState()` on resume.

`goBackToProjectList()` also now includes `adjudicationModified` in its save prompt, matching `saveModification()`. Previously only `modified` was checked, so unsaved adjudication work was dropped without a prompt; with the working set now discarded on leaving, that prompt is what preserves it. The prompt stays ahead of the `setReviewMode(OTHERS)` call, because `directsave()` only writes the `adjudication/` folder while `reviewmode == adjudicationMode`.

## Affected Files
- `src/main/java/userInterface/NavigationManager.java` — added `discardAdjudicationState()`; called from `selectProject()` and `goBackToProjectList()`; adjudication-aware save prompt

## Tests
- `src/test/java/adjudication/CrossProjectAdjudicationDepotLeakTest.java` — builds two real on-disk project copies with differing adjudication progress and drives the switch headlessly.

## Status
**FIXED** — 2026-09-18
