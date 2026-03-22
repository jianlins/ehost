# EHOST-006: Save Prompt on Mode Switch

## Summary
Adds save prompts when switching between annotation and adjudication modes, and introduces an `adjudicationModified` flag to track whether the user has made actual changes in adjudication mode.

## Problem
eHOST had no save prompt when switching between annotation and adjudication modes (EHOST-004). The `directsave()` method only writes the `adjudication/` folder when `GUI.reviewmode == adjudicationMode`, so switching modes before saving could silently discard work.

An initial fix prompted based on `AdjudicationDepot.isReady()`, but this fires whenever the depot has data — even without user changes — causing an annoying dialog on every mode switch.

## Solution

### 1. `adjudicationModified` flag (`GUI.java`)

Added a new `adjudicationModified` boolean field alongside the existing `modified` flag. It tracks whether the user made any changes while in adjudication mode.

```java
protected boolean modified = false;
protected boolean adjudicationModified = false;
```

### 2. Automatic flag setting via `setModified()` (`GUI.java`)

Rather than adding `setAdjudicationModified()` calls to every individual modification point, `setModified()` was enhanced to also set `adjudicationModified` when in adjudication mode:

```java
public void setModified() {
    this.modified = true;
    if (reviewmode == ReviewMode.adjudicationMode) {
        this.adjudicationModified = true;
    }
}
```

This catches all existing call sites automatically:
- `Popmenu.java` — right-click annotation creation
- `rightClickOnAnnotPopUp.java` — right-click annotation deletion
- `GUI.java` — span editing, relationship deletion, remove all
- `AnnotationBuilder.java` — annotation builder
- `Comparator.java` — comparator panel edits

The explicit `setAdjudicationModified()` calls in `ExpandButton.java` (accept/reject/acceptAll) remain as belt-and-suspenders.

### 3. Save prompt leaving adjudication mode (`GUI.java`)

In `mode_enterAnnotationMode()`, prompts only when `adjudicationModified` is true:

```java
if (reviewmode == ReviewMode.adjudicationMode && adjudicationModified) {
    boolean save = popDialog_Asking_YesNo(
        "Do you want to save your adjudication work before switching to annotation mode?",
        "Save Adjudication:");
    if (save) {
        saveto_originalxml();
    }
    adjudicationModified = false;
}
```

The save happens BEFORE the mode switch so `directsave()` writes the `adjudication/` folder.

### 4. Save prompt leaving annotation mode (`GUI.java`)

In `jRadioButton_adjudicationModeMouseReleased()`, prompts only when `modified` is true:

```java
if (this.modified) {
    boolean save = popDialog_Asking_YesNo(
        "Do you want to save your annotation work before switching to adjudication mode?",
        "Save Annotations:");
    if (save) {
        saveto_originalxml();
        this.modified = false;
    }
}
```

### 5. Enhanced window close/exit (`GUI.java`)

`saveModification()` now also checks `adjudicationModified`:

```java
boolean needsSave = this.modified;
if (adjudicationModified) {
    needsSave = true;
}
```

### 6. Direct assignment fix (`GUI.java`)

Changed `this.modified = true` in `addSpan()` to `this.setModified()` so the adjudication flag is set when adding spans in adjudication mode.

### 7. Reset `adjudicationModified` on explicit save (`GUI.java`)

The save button handler (`jButton_saveActionPerformed`) originally only reset the `modified` flag. This meant that after explicitly saving in adjudication mode, the `adjudicationModified` flag stayed `true`, causing a redundant "Save adjudication work?" prompt on the next mode switch even though nothing had changed since the last save.

Fixed by resetting both flags in the save handler:

```java
private void jButton_saveActionPerformed(ActionEvent evt) {
    saveto_originalxml();
    commons.Tools.beep();
    this.modified = false;
    this.adjudicationModified = false;
}
```

## Files Changed
| File | Change |
|------|--------|
| `src/main/java/userInterface/GUI.java` | Added `adjudicationModified` flag, save prompts on mode switch, enhanced `setModified()`, `saveModification()`, and `jButton_saveActionPerformed()` to reset both flags |
| `src/main/java/userInterface/annotationCompare/ExpandButton.java` | Added `gui.setAdjudicationModified()` to accept/reject/acceptAll methods |

## Related
- **EHOST-004**: Bug report for missing save prompt on mode switch
- **EHOST-005**: Adjudication resume robustness (parent enhancement)
