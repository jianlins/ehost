# Bug Report: Duplicate Adjudication Elements in knowtator.xml

## Bug ID
EHOST-001

## Summary
When in adjudication mode, saving the project (either manually or when exiting) can result in duplicate annotation elements being written to knowtator.xml files in the "saved" folder.

## Severity
Medium - Data integrity issue that can cause confusion and incorrect annotation counts.

## Environment
- eHOST version: All versions using the current save mechanism
- Java version: 8
- OS: All

## Description

### Root Cause
In `OutputToXML.java`, the `buildxml()` method has a logic flaw that causes duplicate annotations when saving:

```java
// Lines 190-196 in OutputToXML.java
root = addAnnotations( root, is_outputing_adjudicated_annotations );

if(!is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );            
    root = adjudicationParameters( root );
}
```

When `is_outputing_adjudicated_annotations = false` (saving to "saved" folder):
1. `addAnnotations()` gets annotations from the **regular Depot** (line 243)
2. `addAdjudicatingAnnotations()` **ALSO** gets annotations from **AdjudicationDepot** (line 310)

Since AdjudicationDepot contains COPIES of annotations from the regular Depot, this results in duplicates.

### Reproduction Steps
1. Open a project in eHOST
2. Enter **adjudication mode** (annotations are copied from Depot to AdjudicationDepot)
3. Make changes (accept/reject annotations) - these modifications are stored in AdjudicationDepot
4. Exit the project - system prompts to save changes
5. Click "Yes" to save
6. Check knowtator.xml in "saved" folder - annotations may be duplicated

### Expected Behavior
Each annotation should appear only once in knowtator.xml.

### Actual Behavior
Annotations that exist in both the regular Depot and AdjudicationDepot are saved twice.

## Affected Files
- `src/main/java/resultEditor/save/OutputToXML.java` (lines 190-196)

## Fix Description
Remove the call to `addAdjudicatingAnnotations()` when saving to the "saved" folder. The annotations in AdjudicationDepot are copies of the original annotations, and any accepted/rejected changes should be handled through the regular Depot mechanism.

## Status
**REVERTED** - 2026-03-20

The fix for EHOST-001 was reverted because it caused a more severe regression (EHOST-003): complete loss of adjudication state on app restart. The `<adjudicating>` elements use a different XML tag from `<annotation>` and are loaded into a separate data store (`AdjudicationDepot`), so they are not true duplicates in terms of application behavior. See `docs/bugs/EHOST-003-adjudication-resume-failure.md` and `docs/enhancements/005-adjudication-resume-robustness.md` for details.

## Fix Details

### Fix Applied
- **Date**: 2026-03-04
- **Fixed by**: opencode (AI assistant)
- **Commit**: `e98e707` (tag: 1.39a2)
- **Verification**: All 34 tests pass

### Fix Reverted
- **Date**: 2026-03-20
- **Reason**: Caused EHOST-003 (adjudication resume failure — all adjudication state lost on restart)

### Fix Description
Removed the call to `addAdjudicatingAnnotations()` when saving to the "saved" folder to prevent duplicate annotations.

**Changed file**: `src/main/java/resultEditor/save/OutputToXML.java` (lines 190-198)

**Before (buggy)**:
```java
if(!is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );            
    root = adjudicationParameters( root );
}
```

**After (fixed)**:
```java
if(!is_outputing_adjudicated_annotations){
    root = adjudicationParameters( root );
}
```

### Test Cases
- Added `src/test/java/resultEditor/save/OutputToXMLTest.java` with 3 test cases
- All existing tests pass (34 total)

### Related Issues
- GitHub Issue: See `docs/issues/` folder for tracking
