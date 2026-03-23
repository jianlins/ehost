# Adjudication Restart Warning Dialog

**Date**: 2026-03-23  
**Tag**: 1.39b5  
**Status**: ✅ Completed  
**Contributors**: Jianlin Shi, GitHub Copilot CLI

---

## 📋 Summary

When switching to adjudication mode and previous adjudication files exist, the user is now presented with a three-option dialog:

1. **Continue** previous adjudication work
2. **Start a new** adjudication (with a follow-up warning)
3. **Cancel** and return to annotation mode

If the user selects "Start a new adjudication", a **warning confirmation dialog** is shown explaining that:

- All previously adjudicated annotations will be **erased**
- Annotators' original annotations will **not** be affected

Only after the user explicitly confirms does the application clear the in-memory `AdjudicationDepot` and delete the persisted `.knowtator.xml` files from the `adjudication/` folder.

---

## 🔍 Problem

Previously, when a user chose to start a new adjudication while existing adjudication files were present, the application proceeded immediately without warning. This could lead to unintentional loss of previously adjudicated work, which may have taken significant time and effort to produce.

---

## ✅ Solution

### Modified File

- `src/main/java/userInterface/ContentRenderer.java`

### Changes

1. **Warning dialog** — Added a `JOptionPane.showConfirmDialog` with `WARNING_MESSAGE` type when the user clicks "No, Start a new adjudication". The dialog clearly states that previous adjudication annotations will be erased and that annotators' original annotations are safe.

2. **Data cleanup on confirmation** — When the user confirms:
   - `AdjudicationDepot.clear()` removes all in-memory adjudication data
   - `clearAdjudicationFiles()` deletes all `.knowtator.xml` files from the project's `adjudication/` folder

3. **Cancellation path** — If the user declines the warning, the application returns to annotation mode without touching any data.

4. **New helper method** — `clearAdjudicationFiles()` safely iterates the `adjudication/` directory and removes only `.knowtator.xml` files.

### Dialog Flow

```
User clicks "Adjudication Mode"
    │
    ├─ No existing adjudication files → open adjudication setup dialog
    │
    └─ Existing adjudication files found
        │
        ├─ "Yes, please"     → resume previous adjudication
        ├─ "Cancel"          → return to annotation mode
        └─ "No, Start new"   → ⚠️ WARNING DIALOG
                                │
                                ├─ "Yes" → clear data + open adjudication setup
                                └─ "No"  → return to annotation mode
```

---

## 🧪 Testing

- Verified that the warning dialog appears when selecting "Start a new adjudication"
- Verified that declining the warning returns to annotation mode without data loss
- Verified that confirming the warning clears adjudication files and opens the setup dialog
- Build compiles successfully with `mvn clean package -DskipTests`
