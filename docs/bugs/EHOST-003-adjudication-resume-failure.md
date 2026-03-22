# Bug Report: Adjudication Resume Failure After App Restart

## Bug ID
EHOST-003

## Summary
When eHOST is closed during an in-progress adjudication and reopened, the application fails to detect the previous adjudication session. Instead of showing the "continue previous adjudication?" dialog, it goes straight to the new adjudication setup wizard. Even if the user manually navigates to resume, all previously adjudicated annotations (MATCHES_OK, NON_MATCHES, etc.) are lost.

## Severity
High - Complete loss of adjudication work on app restart.

## Environment
- eHOST version: 1.39a2 (commit `e98e707`)
- Java version: 8
- OS: All

## Description

### Root Cause
Commit `e98e707` ("fix: resolve duplicate adjudication elements in knowtator.xml (EHOST-001)") removed the call to `addAdjudicatingAnnotations()` from `OutputToXML.buildxml()`. This method writes `<adjudicating>` XML elements to `saved/*.knowtator.xml` files, which store:
- Annotation copies with their adjudication status (MATCHES_OK, NON_MATCHES, etc.)
- Processed/unprocessed flags

Without these elements, the adjudication state is completely lost on restart.

Additionally, the resume detection logic in `ContentRenderer.setReviewMode()` required both `AdjudicationDepot.isReady()` AND `Paras.isReadyForAdjudication()` to be true simultaneously. After restart, `AdjudicationDepot` was empty (because `<adjudicating>` elements were no longer saved), so the resume dialog was never shown.

### Two Symptoms
1. **No resume dialog**: The "Would you like to continue your previous adjudication work?" dialog is never displayed after restart.
2. **Lost adjudication state**: Even if adjudication mode is entered manually, all prior adjudication decisions (MATCHES_OK, accepted annotations, etc.) are gone.

### Reproduction Steps
1. Open a project in eHOST with annotations from multiple annotators
2. Enter adjudication mode and select annotators/classes
3. Adjudicate some annotations (accept matches, reject others)
4. Close eHOST (annotations are saved)
5. Reopen eHOST with the same project
6. Click "Adjudication Mode" radio button
7. **Expected**: Dialog asking to continue previous adjudication
8. **Actual**: New adjudication setup wizard appears; all previous work is lost

### Introduced By
- **Commit**: `e98e707` (tag: 1.39a2)
- **Message**: "fix: resolve duplicate adjudication elements in knowtator.xml (EHOST-001)"
- **Change**: Removed `root = addAdjudicatingAnnotations(root);` from `OutputToXML.buildxml()`

### Relationship to EHOST-001
EHOST-001 reported duplicate annotations in knowtator.xml. The fix for EHOST-001 was overly aggressive — it removed `addAdjudicatingAnnotations()` entirely, when in fact the `<adjudicating>` elements use a different XML tag than `<annotation>` and are loaded into a separate data store (`AdjudicationDepot`) during import. They are not true duplicates.

## Affected Files
- `src/main/java/resultEditor/save/OutputToXML.java` (line 196 — missing `addAdjudicatingAnnotations()`)
- `src/main/java/userInterface/ContentRenderer.java` (line 633 — resume detection too strict)
- `src/main/java/userInterface/GUI.java` (`mode_continuePreviousAdjudicationWork()` — no fallback for empty state)

## Status
**FIXED** - 2026-03-20
