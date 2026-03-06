# Bug Report: Missing Annotator Name and ID on Annotation Class Change

## Bug ID
EHOST-002

## Summary
When changing an annotation's class using the AnnotationClassChooser, the annotator name and annotator ID were not being set on the annotation, potentially resulting in annotations without proper annotator attribution.

## Severity
Low - Data completeness issue affecting annotation metadata.

## Environment
- eHOST version: All versions prior to 1.39b1
- Java version: 8
- OS: All

## Description

### Root Cause
In `AnnotationClassChooser.java`, when an annotation class is selected and applied to the current annotation, only the annotation class and creation date were being set. The annotator name and annotator ID fields were not being populated.

### Affected Code Location
`src/main/java/resultEditor/spanEdit/AnnotationClassChooser.java` - around line 131

### Expected Behavior
When an annotation class is chosen, the annotation should have:
- Annotation class
- Creation date
- **Annotator name**
- **Annotator ID**

### Actual Behavior
The annotator name and annotator ID were not being set when changing the annotation class.

## Affected Files
- `src/main/java/resultEditor/spanEdit/AnnotationClassChooser.java`

## Fix Description
Added code to set the annotator name and annotator ID on the current annotation when a class is chosen:

```java
resultEditor.workSpace.WorkSet.currentAnnotation.setAnnotator(
        resultEditor.annotator.Manager.getAnnotatorName_OutputOnly());
resultEditor.workSpace.WorkSet.currentAnnotation.annotatorid =
        resultEditor.annotator.Manager.getAnnotatorID_outputOnly();
```

## Status
**FIXED** - 2026-03-04

## Fix Details

### Fix Applied
- **Date**: 2026-03-04
- **Fixed by**: jianlins
- **Commit**: 2ba4da30feecd2249829493c4f4c8972c30f3127
- **Version**: 1.39b1

### Code Change
**File**: `src/main/java/resultEditor/spanEdit/AnnotationClassChooser.java`

**Before**:
```java
resultEditor.workSpace.WorkSet.currentAnnotation.annotationclass = annotationclass;
resultEditor.workSpace.WorkSet.currentAnnotation.creationDate = commons.OS.getCurrentDate();

if( resultEditor.workSpace.WorkSet.currentAnnotation.hasAttribute()){
```

**After**:
```java
resultEditor.workSpace.WorkSet.currentAnnotation.annotationclass = annotationclass;
resultEditor.workSpace.WorkSet.currentAnnotation.creationDate = commons.OS.getCurrentDate();

resultEditor.workSpace.WorkSet.currentAnnotation.setAnnotator(
        resultEditor.annotator.Manager.getAnnotatorName_OutputOnly());
resultEditor.workSpace.WorkSet.currentAnnotation.annotatorid =
        resultEditor.annotator.Manager.getAnnotatorID_outputOnly();

if( resultEditor.workSpace.WorkSet.currentAnnotation.hasAttribute()){
```

## Related Issues
- Ensures consistency of annotator information across all annotation operations
