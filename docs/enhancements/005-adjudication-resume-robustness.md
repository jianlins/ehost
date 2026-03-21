# EHOST-005: Robust Adjudication Resume After App Restart

## Summary
Fixes adjudication resume failure (EHOST-003) introduced in commit `e98e707` and adds robustness improvements so that adjudication state is reliably detected and restored when eHOST is reopened.

## Problem
After EHOST-001 fix removed `addAdjudicatingAnnotations()` from the save path, two failures occurred on app restart:
1. The resume dialog was never shown (detection relied solely on in-memory state).
2. All adjudicated annotations were lost (AdjudicationDepot was empty, with no `<adjudicating>` elements in saved XML to repopulate it).

## Solution

### 1. Restore `<adjudicating>` element persistence (`OutputToXML.java`)

Re-enabled `addAdjudicatingAnnotations()` for the `saved/` folder. The `<adjudicating>` elements use a distinct XML tag from `<annotation>`, and `ImportXML` routes them to `AdjudicationDepot` (type=5 → `recordAnnotationAdj()`), not the regular `Depot`. They are not true duplicates despite EHOST-001's analysis.

**Before:**
```java
if(!is_outputing_adjudicated_annotations){
    root = adjudicationParameters( root );
}
```

**After:**
```java
if(!is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );
    root = adjudicationParameters( root );
}
```

### 2. Broaden resume detection (`ContentRenderer.java`)

The original condition required both `AdjudicationDepot.isReady()` and `Paras.isReadyForAdjudication()` to be true simultaneously. After restart, `Paras` is restored from `<eHOST_Adjudication_Status>` in saved XML via `ImportXML.getAdjudicationSetting()`, but the depot may still be loading. The new condition detects adjudication from any available source.

**Before:**
```java
if ((Paras.__adjudicated) && (Paras.isReadyForAdjudication())) {
```

**After:**
```java
boolean hasPersistedAdjudication = AdjudicationLoader.isAdjudicationAvailable();
if (Paras.isReadyForAdjudication() || hasPersistedAdjudication) {
```

Detection sources (any one is sufficient):
- `Paras.isReadyForAdjudication()` — Paras restored from saved XML (≥2 annotators selected)
- `AdjudicationLoader.isAdjudicationAvailable()` — `.knowtator.xml` files exist in `adjudication/` folder

### 3. Fallback recovery in resume path (`GUI.java`)

Added safety nets in `mode_continuePreviousAdjudicationWork()` for edge cases:

**Paras fallback**: If `Paras` was not restored from XML (e.g., corrupted file, first-time load), reconstruct annotators and classes by scanning all annotations via `CollectInfo`.

```java
if (!Paras.isReadyForAdjudication()) {
    rebuildParasFromAnnotations();
}
```

**AdjudicationDepot fallback**: If the depot is still empty after XML import (e.g., legacy project without `<adjudicating>` elements), populate it from the regular `Depot` using `copyAnnotations(true)`. This allows adjudication to resume with all annotations, though prior adjudication decisions will need to be re-done.

```java
if (!adjudication.data.AdjudicationDepot.isReady()) {
    adjudication.data.AdjudicationDepot depotOfAdj =
            new adjudication.data.AdjudicationDepot();
    depotOfAdj.copyAnnotations(
            Paras.getAnnotators(), Paras.getClasses(), true);
}
```

## Data Flow

### Save (exiting adjudication mode)
```
OutputToXML.buildxml(saved/, is_outputing=false)
  ├── addAnnotations()          → writes <annotation> elements (regular Depot)
  ├── addAdjudicatingAnnotations() → writes <adjudicating> elements (AdjudicationDepot)
  └── adjudicationParameters()  → writes <eHOST_Adjudication_Status> (Paras settings)
```

### Load (reopening project)
```
ImportXML.readXMLContents()
  ├── <annotation> elements     → Depot (regular annotations)
  ├── <adjudicating> elements   → AdjudicationDepot (type=5, with status preserved)
  └── <eHOST_Adjudication_Status> → Paras (annotators, classes, check flags)
```

### Resume (clicking Adjudication Mode)
```
ContentRenderer.setReviewMode(adjudicationMode)
  ├── Check: Paras.isReadyForAdjudication() || hasPersistedAdjudication?
  │   ├── YES → Show resume dialog
  │   │   ├── "Yes, please" → mode_continuePreviousAdjudicationWork()
  │   │   │   ├── Ensure Paras ready (fallback: rebuildParasFromAnnotations)
  │   │   │   ├── Ensure AdjudicationDepot ready (fallback: copyAnnotations)
  │   │   │   └── checkAnnotations(false) → preserves MATCHES_OK, re-analyzes rest
  │   │   ├── "No, Start new" → new Adjudication dialog
  │   │   └── "Cancel" → back to annotation mode
  │   └── NO → new Adjudication dialog
```

## Files Changed
| File | Change |
|------|--------|
| `src/main/java/resultEditor/save/OutputToXML.java` | Restored `addAdjudicatingAnnotations()` call |
| `src/main/java/userInterface/ContentRenderer.java` | Broadened resume detection condition, added `AdjudicationLoader` import |
| `src/main/java/userInterface/GUI.java` | Added Paras and AdjudicationDepot fallback recovery in resume path |

## Related
- **EHOST-001**: Duplicate adjudication elements (the fix that introduced this regression)
- **EHOST-003**: Bug report for this adjudication resume failure
