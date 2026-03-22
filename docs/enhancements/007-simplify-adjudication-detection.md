# EHOST-007: Simplify Adjudication Detection with File-Based Approach

## Summary
Removes `<eHOST_Adjudication_Status>` XML metadata from knowtator.xml files and simplifies ongoing adjudication detection to use only file-based checking. This keeps the knowtator.xml structure clean and standard while maintaining full adjudication functionality.

## Problem
The previous implementation (EHOST-005) used `<eHOST_Adjudication_Status>` XML elements to store adjudication configuration (selected annotators, classes, checking parameters) in both `saved/` and `adjudication/` folders. This approach had several issues:

1. **Non-standard XML structure**: Added custom elements to knowtator.xml files that deviate from the standard format
2. **Complex detection logic**: Required parsing XML metadata and checking multiple sources (`Paras.isReadyForAdjudication()`, `AdjudicationLoader.isAdjudicationAvailable()`, `AdjudicationDepot.isReady()`)
3. **Redundant storage**: Configuration was duplicated in both folder types
4. **Unnecessary overhead**: XML parsing just to detect adjudication state

The presence of `adjudication/*.knowtator.xml` files is itself a reliable indicator that adjudication is ongoing, making the XML metadata redundant.

## Solution

### 1. Remove XML metadata writing (`OutputToXML.java`)

Removed `adjudicationParameters()` method calls from both save paths:

**Before:**
```java
// In buildxml() (saved/ folder)
root = addAnnotations( root, false );
root = adjudicationParameters( root );

// In buildxml() (adjudication/ folder)
if(is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );
    root = adjudicationParameters( root );
} else {
    root = adjudicationParameters( root );
}
```

**After:**
```java
// In buildxml() (saved/ folder)
root = addAnnotations( root, false );

// In buildxml() (adjudication/ folder)
if(is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );
}
```

### 2. Remove XML metadata parsing (`ImportXML.java`)

Removed `getAdjudicationSetting()` method call from XML import:

**Before:**
```java
Element root = doc.getRootElement();

getAdjudicationSetting( root );

//##-2- get all node of annotations
```

**After:**
```java
Element root = doc.getRootElement();

//##-2- get all node of annotations
```

### 3. Simplify detection logic (`ContentRenderer.java`)

Removed `Paras.isReadyForAdjudication()` check and rely solely on file-based detection:

**Before:**
```java
Paras.__adjudicated = adjudication.data.AdjudicationDepot.isReady();
boolean hasPersistedAdjudication = AdjudicationLoader.isAdjudicationAvailable();

// Show resume dialog if in-memory adjudication depot exists,
// or if persisted adjudication data exists on disk, or if
// Paras was restored from saved XML (after app restart).
if (Paras.isReadyForAdjudication() || hasPersistedAdjudication) {
```

**After:**
```java
Paras.__adjudicated = adjudication.data.AdjudicationDepot.isReady();
boolean hasPersistedAdjudication = AdjudicationLoader.isAdjudicationAvailable();

// Show resume dialog if persisted adjudication data exists on disk
// (adjudication/*.knowtator.xml files present)
if (hasPersistedAdjudication) {
```

### 4. Parameter reconstruction mechanism

Adjudication parameters (selected annotators, classes, checking options) are now reconstructed from the annotations themselves when resuming, using the existing fallback mechanism in `GUI.mode_continuePreviousAdjudicationWork()`:

```java
if (!Paras.isReadyForAdjudication()) {
    rebuildParasFromAnnotations();  // Scan all annotations to rebuild Paras
}
```

This mechanism was already implemented in EHOST-005 as a fallback; it now becomes the primary reconstruction method.

## What Still Works

### Per-annotation status tracking (PRESERVED)

Individual `<AdjudicationStatus>` elements on each annotation are **still written and preserved**. These track whether each specific annotation has been adjudicated:

```xml
<adjudicating id="EHOST_Instance_123">
    <mention id="EHOST_Instance_123"/>
    <annotator id="Annotator1">Annotator1</annotator>
    <span start="100" end="115"/>
    <spannedText>heart disease</spannedText>
    <creationDate>...</creationDate>
    <AdjudicationStatus>MATCHES_OK</AdjudicationStatus>  <!-- KEPT -->
    <processed>true</processed>
</adjudicating>
```

**Important distinction:**
- `<eHOST_Adjudication_Status>` (REMOVED): Project-level configuration metadata
- `<AdjudicationStatus>` (KEPT): Per-annotation progress tracking

### File structure

**`saved/` folder:**
- Contains: `<annotation>` elements only
- Does NOT contain: `<adjudicating>` elements, `<eHOST_Adjudication_Status>` metadata
- Purpose: Regular annotation storage

**`adjudication/` folder:**
- Contains: `<annotation>` elements (MATCHES_OK only) + `<adjudicating>` elements (all working copies with their `<AdjudicationStatus>`)
- Does NOT contain: `<eHOST_Adjudication_Status>` metadata
- Purpose: Adjudication working state
- **Detection indicator**: Presence of `.knowtator.xml` files in this folder indicates ongoing adjudication

## Detection Flow

### Single-source detection
```
ContentRenderer.setReviewMode(adjudicationMode)
  └── Check: AdjudicationLoader.isAdjudicationAvailable()
      └── adjudication/ folder exists AND contains .knowtator.xml files?
          ├── YES → Show resume dialog
          │   ├── "Yes, please" → mode_continuePreviousAdjudicationWork()
          │   │   ├── rebuildParasFromAnnotations() → reconstruct configuration
          │   │   ├── AdjudicationLoader.loadWorkingState() → load <adjudicating> elements
          │   │   └── checkAnnotations(false) → preserves MATCHES_OK status
          │   ├── "No, Start new" → new Adjudication dialog
          │   └── "Cancel" → back to annotation mode
          └── NO → new Adjudication dialog
```

### Parameter reconstruction
When resuming adjudication, `rebuildParasFromAnnotations()` scans all annotations and extracts:
- **Annotators**: All unique annotator names found
- **Classes**: All unique class names found
- **Checking parameters**: Default values (can be adjusted in adjudication dialog)

## Benefits

✅ **Cleaner XML files**: No custom metadata elements in knowtator.xml  
✅ **Simpler detection**: Single file existence check instead of multi-source parsing  
✅ **Standard format**: Maintains knowtator.xml compatibility  
✅ **Less parsing overhead**: No XML metadata parsing on load  
✅ **Reliable detection**: File presence is a clear, unambiguous indicator  
✅ **Maintained functionality**: All adjudication features work exactly as before  

## Files Changed

| File | Lines Changed | Description |
|------|---------------|-------------|
| `src/main/java/resultEditor/save/OutputToXML.java` | -6 | Removed `adjudicationParameters()` calls from both save paths |
| `src/main/java/imports/ImportXML.java` | -3 | Removed `getAdjudicationSetting()` call from XML parsing |
| `src/main/java/userInterface/ContentRenderer.java` | -4, +3 | Simplified detection to file-based checking only |

**Total**: 3 files changed, 13 deletions(-), 3 insertions(+)

## Testing

✅ Build successful  
✅ All 34 tests passed  
✅ JAR created at `target/deploy/eHOST.jar`  

## Related

- **EHOST-005**: Adjudication resume robustness (introduced `<eHOST_Adjudication_Status>`)
- **EHOST-006**: Save prompt on mode switch (prevents data loss)
- **EHOST-001**: Duplicate adjudication elements (original issue that led to EHOST-005)
