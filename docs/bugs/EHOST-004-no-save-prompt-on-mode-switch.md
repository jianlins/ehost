# Bug Report: No Save Prompt When Switching Between Annotation and Adjudication Modes

## Bug ID
EHOST-004

## Summary
When switching between annotation mode and adjudication mode, eHOST does not prompt the user to save unsaved work. This can lead to silent data loss — adjudication decisions or annotation edits may be discarded without warning.

## Severity
Medium - Unsaved work lost silently on mode switch.

## Environment
- eHOST version: All versions prior to fix
- Java version: 8
- OS: All

## Description

### Root Cause
The `directsave()` method in `OutputToXML.java` only writes the `adjudication/` folder when `GUI.reviewmode == adjudicationMode`. If the user switches to annotation mode before saving, the adjudication folder is not updated and in-memory adjudication decisions are lost on next restart.

Conversely, annotation edits made in annotation mode are not saved before switching to adjudication mode, so closing the app from adjudication mode may not preserve those annotation changes.

The mode-switching code (`mode_enterAnnotationMode()` and `jRadioButton_adjudicationModeMouseReleased()`) had no save prompts. The existing `saveModification()` method (used on window close and file navigation) only checked the `modified` flag, which tracked annotation-mode changes but not adjudication-mode changes.

### Symptoms
1. User makes adjudication decisions (accept/reject), switches to annotation mode, closes app → adjudication work lost.
2. User makes annotation edits, switches to adjudication mode, closes app → annotation edits lost.
3. No "save your work?" dialog appears during mode switch, unlike file switching or app close.

### Reproduction Steps
1. Open a project with multiple annotators
2. Enter adjudication mode and adjudicate some annotations
3. Switch back to annotation mode (click Annotation Mode radio button)
4. Close eHOST — no save prompt for adjudication work
5. Reopen → adjudication decisions are lost

### Additional Issue
The initial fix (EHOST-005 save-on-mode-switch) used `AdjudicationDepot.isReady()` to decide whether to prompt. This checks whether the depot *has data*, which is always true in adjudication mode — even if the user made no changes. This caused an unnecessary save dialog every time the user switched modes.

## Fix
See enhancement EHOST-006.

## Affected Files
- `src/main/java/userInterface/GUI.java` — missing save prompts in mode-switch methods

## Status
**FIXED** — 2026-03-21
