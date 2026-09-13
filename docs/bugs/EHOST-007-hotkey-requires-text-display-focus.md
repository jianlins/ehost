# Bug Report: Ctrl+PageUp / Ctrl+PageDown Only Work When the Text Display Has Focus

## Bug ID
EHOST-007

## Summary
The document navigation hotkeys (Ctrl+PageUp / Ctrl+PageDown) only worked while
the Text Display text pane owned the keyboard focus *and* no annotation was
selected. Right after opening a project, or after clicking an annotation or
working in the Annotation Editor, the hotkeys silently did nothing.

## Severity
Low - Usability; the toolbar buttons still work.

## Environment
- eHOST version: All versions prior to fix (reported on 1.39)
- Java version: 8
- OS: All

## Description

### Root Cause
The hotkeys were implemented inside `textPaneforClinicalNotesKeyPressed(KeyEvent)`
in `GUI.java`, a key listener attached to the Text Display text pane only. Two
things followed from that:

1. Swing delivers key events to the focus owner, so any other focus owner (the
   annotation list, the class tree, the Annotation Editor fields, or the frame
   itself right after a project is opened and nothing has been clicked) never
   reached the listener.
2. Even with the text pane focused, the `VK_PAGE_UP` / `VK_PAGE_DOWN` cases sat
   in the `else` branch of `if (WorkSet.currentAnnotation != null)`, so
   selecting an annotation disabled them.

### Symptoms
1. Open a project and press Ctrl+PageDown without clicking anything — nothing
   happens.
2. Click the Text Display area — the hotkeys work.
3. Click an annotation, or click into the Annotation Editor panel — the hotkeys
   stop working again.

### Reproduction Steps
1. Open a project with at least two documents.
2. Press Ctrl+PageDown → the document does not change.
3. Click in the Text Display area, press Ctrl+PageDown → the document changes.
4. Click any annotation, press Ctrl+PageDown → the document does not change.

## Fix
`GUI` now registers a `KeyEventDispatcher` with the current
`KeyboardFocusManager` (`installDocumentNavigationHotkeys()`). The dispatcher
sees key events before they are delivered to the focus owner, so Ctrl+PageUp /
Ctrl+PageDown navigate documents from anywhere in the eHOST window. The
keystroke matching itself lives in `DocumentNavigationHotkeys`, so it can be
unit tested without a screen. It is deliberately narrow:

- Only plain Ctrl + PageUp/PageDown (no Alt, Shift or Meta) is handled, and the
  event is consumed so components with their own bindings for those keystrokes
  (for example `JTabbedPane`) do not also react. Ctrl+Shift+PageDown therefore
  keeps selecting a page of text in the Text Display, which the old text pane
  handler used to hijack.
- Only events targeted at the main eHOST window are handled; dialogs such as the
  schema editors keep their own behaviour.
- The hotkeys stay inert unless the navigation buttons are on screen and the
  project actually holds documents.

The dispatcher is unregistered in `GUI.dispose()`. The superseded
`VK_PAGE_UP` / `VK_PAGE_DOWN` cases were removed from
`textPaneforClinicalNotesKeyPressed(KeyEvent)`: the dispatcher consumes those
keystrokes before the text pane sees them, so keeping a second implementation
would only invite the two copies to drift apart.

## Affected Files
- `src/main/java/userInterface/GUI.java`
- `src/main/java/userInterface/DocumentNavigationHotkeys.java` (new)
- `src/test/java/userInterface/DocumentNavigationHotkeysTest.java` (new)

## Status
**FIXED** — 2026-09-13 (GitHub issue #27)
