# GitHub Issue: Duplicate Adjudication Elements in knowtator.xml

## Issue Title
Duplicate adjudication annotations saved to knowtator.xml when exiting project in adjudication mode

## Issue Description
When in adjudication mode, saving the project (either manually or when exiting) can result in duplicate annotation elements being written to knowtator.xml files in the "saved" folder.

## Labels
- bug
- adjudication
- priority: medium
- area: save

## Steps to Reproduce
1. Open a project in eHOST
2. Enter adjudication mode (annotations are copied from Depot to AdjudicationDepot)
3. Make changes (accept/reject annotations) - these modifications are stored in AdjudicationDepot
4. Exit the project - system prompts to save changes
5. Click "Yes" to save
6. Check knowtator.xml in "saved" folder - annotations may be duplicated

## Expected Behavior
Each annotation should appear only once in knowtator.xml.

## Actual Behavior
Annotations that exist in both the regular Depot and AdjudicationDepot are saved twice.

## Root Cause
In `OutputToXML.java`, the `buildxml()` method was saving annotations from both the regular Depot AND the AdjudicationDepot to the "saved" folder, causing duplicates.

## Fix Applied
- **Date**: 2026-03-04
- **PR**: (to be created)
- **Files changed**:
  - `src/main/java/resultEditor/save/OutputToXML.java` - Removed duplicate save logic
  - `src/test/java/resultEditor/save/OutputToXMLTest.java` - Added tests

## Verification
- All 34 tests pass
- Manual testing confirmed no duplicates in knowtator.xml after fix

## Related Files
- Bug report: `docs/bugs/EHOST-001-duplicate-adjudication-elements.md`
