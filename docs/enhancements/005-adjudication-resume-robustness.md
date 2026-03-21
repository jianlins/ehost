# EHOST-005: Robust Adjudication Resume After App Restart

## Summary
Fixes adjudication resume failure (EHOST-003) introduced in commit `e98e707` and adds robustness improvements so that adjudication state is reliably detected and restored when eHOST is reopened.

## Problem
After EHOST-001 fix removed `addAdjudicatingAnnotations()` from the save path, two failures occurred on app restart:
1. The resume dialog was never shown (detection relied solely on in-memory state).
2. All adjudicated annotations were lost (AdjudicationDepot was empty, with no `<adjudicating>` elements in saved XML to repopulate it).

## Solution

### 1. Clean folder separation (`OutputToXML.java`)

Adjudication working state (`<adjudicating>` elements + `adjudicationParameters`) is now saved exclusively to the `adjudication/` folder. The `saved/` folder contains only regular `<annotation>` elements and `adjudicationParameters` (annotator/class selections for Paras restore).

This eliminates the EHOST-001 concern entirely — `saved/` XMLs never contain `<adjudicating>` elements.

**Save paths:**
- `saved/` folder: `<annotation>` + `<eHOST_Adjudication_Status>`
- `adjudication/` folder: `<annotation>` (MATCHES_OK only) + `<adjudicating>` (all working copies) + `<eHOST_Adjudication_Status>`

**Before:**
```java
if(!is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );
    root = adjudicationParameters( root );
}
```

**After:**
```java
if(is_outputing_adjudicated_annotations){
    root = addAdjudicatingAnnotations( root );
    root = adjudicationParameters( root );
} else {
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

### 3. Load adjudication working state from adjudication/ folder (`AdjudicationLoader.java`)

Added `loadWorkingState()` method that loads `<adjudicating>` elements from `adjudication/` folder XMLs into AdjudicationDepot. Uses the existing `ImportAnnotation.XMLImporter()` → `XMLExtractor()` pipeline, which routes type=5 annotations to AdjudicationDepot with their AdjudicationStatus preserved.

### 4. Fallback recovery in resume path (`GUI.java`)

Added safety nets in `mode_continuePreviousAdjudicationWork()` for edge cases:

**Paras fallback**: If `Paras` was not restored from XML (e.g., corrupted file, first-time load), reconstruct annotators and classes by scanning all annotations via `CollectInfo`.

```java
if (!Paras.isReadyForAdjudication()) {
    rebuildParasFromAnnotations();
}
```

**AdjudicationDepot fallback**: If the depot is still empty after XML import (e.g., legacy project without `<adjudicating>` elements), first try `AdjudicationLoader.loadWorkingState()` to load from the `adjudication/` folder. If that also fails, populate from the regular `Depot` using `copyAnnotations(true)`.

```java
if (!adjudication.data.AdjudicationDepot.isReady()) {
    if (!report.iaaReport.AdjudicationLoader.loadWorkingState()) {
        adjudication.data.AdjudicationDepot depotOfAdj =
                new adjudication.data.AdjudicationDepot();
        depotOfAdj.copyAnnotations(
                Paras.getAnnotators(), Paras.getClasses(), true);
    }
}
```

## Data Flow

### Save (exiting adjudication mode)
```
OutputToXML.directsave()
  ├── buildxml(saved/, is_outputing=false)
  │   ├── addAnnotations()          → writes <annotation> elements (regular Depot)
  │   └── adjudicationParameters()  → writes <eHOST_Adjudication_Status> (Paras settings)
  └── buildxml(adjudication/, is_outputing=true)
      ├── addAnnotations()              → writes <annotation> (MATCHES_OK only, from AdjudicationDepot)
      ├── addAdjudicatingAnnotations()  → writes <adjudicating> (ALL working copies with statuses)
      └── adjudicationParameters()      → writes <eHOST_Adjudication_Status> (Paras settings)
```

### Load (reopening project)
```
Reload.load() → ImportAnnotation.XMLImporter() (saved/ folder only)
  ├── <annotation> elements                → regular Depot
  └── <eHOST_Adjudication_Status>         → Paras (annotators, classes, check flags)
```

### Resume (clicking Adjudication Mode)
```
ContentRenderer.setReviewMode(adjudicationMode)
  ├── Check: Paras.isReadyForAdjudication() || hasPersistedAdjudication?
  │   ├── YES → Show resume dialog
  │   │   ├── "Yes, please" → mode_continuePreviousAdjudicationWork()
  │   │   │   ├── Ensure Paras ready (fallback: rebuildParasFromAnnotations)
  │   │   │   ├── Ensure AdjudicationDepot ready:
  │   │   │   │   ├── Try AdjudicationLoader.loadWorkingState() (adjudication/ folder)
  │   │   │   │   │   → <adjudicating> elements → AdjudicationDepot (status preserved)
  │   │   │   │   └── Fallback: copyAnnotations from regular Depot
  │   │   │   └── checkAnnotations(false) → preserves MATCHES_OK, re-analyzes rest
  │   │   ├── "No, Start new" → new Adjudication dialog
  │   │   └── "Cancel" → back to annotation mode
  │   └── NO → new Adjudication dialog
```

## Files Changed
| File | Change |
|------|--------|
| `src/main/java/resultEditor/save/OutputToXML.java` | Moved `addAdjudicatingAnnotations()` to adjudication/ path only |
| `src/main/java/userInterface/ContentRenderer.java` | Broadened resume detection condition, added `AdjudicationLoader` import |
| `src/main/java/userInterface/GUI.java` | Added Paras fallback, load working state from adjudication/ folder |
| `src/main/java/report/iaaReport/AdjudicationLoader.java` | Added `loadWorkingState()` method |

## Related
- **EHOST-001**: Duplicate adjudication elements (the fix that introduced this regression)
- **EHOST-003**: Bug report for this adjudication resume failure
- **EHOST-004 / EHOST-006**: Save prompt on mode switch (prevents data loss from switching modes without saving)
