package userInterface;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keystroke matching of the global document navigation hotkeys. Runs headless:
 * the part that needs a screen (the dispatcher actually being registered with
 * the {@link java.awt.KeyboardFocusManager}) is exercised by running eHOST.
 */
class DocumentNavigationHotkeysTest {

    private RecordingTarget target;
    private DocumentNavigationHotkeys hotkeys;
    private Component source;

    @BeforeEach
    void setUp() {
        target = new RecordingTarget();
        hotkeys = new DocumentNavigationHotkeys(target);
        source = new JPanel();
    }

    @Test
    void ctrlPageDownOpensNextDocument() {
        KeyEvent evt = key(KeyEvent.KEY_PRESSED, InputEvent.CTRL_MASK, KeyEvent.VK_PAGE_DOWN);

        assertTrue(hotkeys.dispatchKeyEvent(evt), "event should be handled here");
        assertEquals(1, target.next);
        assertEquals(0, target.previous);
        assertTrue(evt.isConsumed(), "event must not reach the focused component");
    }

    @Test
    void ctrlPageUpOpensPreviousDocument() {
        KeyEvent evt = key(KeyEvent.KEY_PRESSED, InputEvent.CTRL_MASK, KeyEvent.VK_PAGE_UP);

        assertTrue(hotkeys.dispatchKeyEvent(evt));
        assertEquals(1, target.previous);
        assertEquals(0, target.next);
    }

    @Test
    void releaseOfTheSameKeystrokeIsSwallowedWithoutNavigating() {
        KeyEvent evt = key(KeyEvent.KEY_RELEASED, InputEvent.CTRL_MASK, KeyEvent.VK_PAGE_DOWN);

        assertTrue(hotkeys.dispatchKeyEvent(evt), "release must not reach the focused component");
        assertEquals(0, target.next, "only the press navigates");
    }

    @Test
    void plainPageKeysStillScrollTheFocusedComponent() {
        assertFalse(hotkeys.dispatchKeyEvent(key(KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_PAGE_DOWN)));
        assertFalse(hotkeys.dispatchKeyEvent(key(KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_PAGE_UP)));
        assertEquals(0, target.calls());
    }

    @Test
    void modifiedVariantsAreLeftToTheFocusedComponent() {
        assertFalse(hotkeys.dispatchKeyEvent(key(KeyEvent.KEY_PRESSED,
                InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, KeyEvent.VK_PAGE_DOWN)),
                "ctrl+shift+PageDown still selects a page of text");
        assertFalse(hotkeys.dispatchKeyEvent(key(KeyEvent.KEY_PRESSED,
                InputEvent.CTRL_MASK | InputEvent.ALT_MASK, KeyEvent.VK_PAGE_UP)));
        assertEquals(0, target.calls());
    }

    @Test
    void otherKeysAreIgnored() {
        assertFalse(hotkeys.dispatchKeyEvent(
                key(KeyEvent.KEY_PRESSED, InputEvent.CTRL_MASK, KeyEvent.VK_Z)));
        assertEquals(0, target.calls());
    }

    @Test
    void nothingHappensWhenTheTargetRejectsTheKeystroke() {
        target.accept = false;
        KeyEvent evt = key(KeyEvent.KEY_PRESSED, InputEvent.CTRL_MASK, KeyEvent.VK_PAGE_DOWN);

        assertFalse(hotkeys.dispatchKeyEvent(evt), "other windows keep their own bindings");
        assertEquals(0, target.calls());
        assertFalse(evt.isConsumed());
    }

    private KeyEvent key(int id, int modifiers, int keyCode) {
        return new KeyEvent(source, id, System.currentTimeMillis(), modifiers, keyCode,
                KeyEvent.CHAR_UNDEFINED);
    }

    private static final class RecordingTarget implements DocumentNavigationHotkeys.Target {
        private boolean accept = true;
        private int previous;
        private int next;

        public boolean acceptsHotkey(Component eventSource) {
            return accept;
        }

        public void previousDocument() {
            previous++;
        }

        public void nextDocument() {
            next++;
        }

        private int calls() {
            return previous + next;
        }
    }
}
