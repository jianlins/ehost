package userInterface;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

/**
 * Application wide handling of the document navigation hotkeys
 * (ctrl+PageUp / ctrl+PageDown).
 * <p>
 * A {@link KeyEventDispatcher} sees key events before they reach the focus
 * owner, which is what makes these hotkeys work no matter which component is
 * currently focused - the text display, the annotation editor, one of the
 * lists, or nothing at all right after a project has been opened.
 */
class DocumentNavigationHotkeys implements KeyEventDispatcher {

    /**
     * What the hotkeys act on, kept separate from the keystroke matching so the
     * matching can be tested without a screen.
     */
    interface Target {

        /**
         * @param eventSource the component the key event was targeted at
         * @return true when this keystroke belongs to the window owning these
         * hotkeys and document navigation is possible right now
         */
        boolean acceptsHotkey(Component eventSource);

        /**
         * Open the document before the current one.
         */
        void previousDocument();

        /**
         * Open the document after the current one.
         */
        void nextDocument();
    }

    private final Target target;

    DocumentNavigationHotkeys(Target target) {
        this.target = target;
    }

    public boolean dispatchKeyEvent(KeyEvent evt) {
        int keyCode = evt.getKeyCode();
        if (keyCode != KeyEvent.VK_PAGE_UP && keyCode != KeyEvent.VK_PAGE_DOWN)
            return false;
        // ctrl+shift+PageDown still selects a page of text, alt/meta variants
        // are left to whoever binds them
        if (!evt.isControlDown() || evt.isAltDown() || evt.isShiftDown() || evt.isMetaDown())
            return false;
        Object source = evt.getSource();
        if (!(source instanceof Component) || !target.acceptsHotkey((Component) source))
            return false;

        if (evt.getID() == KeyEvent.KEY_PRESSED) {
            if (keyCode == KeyEvent.VK_PAGE_DOWN)
                target.nextDocument();
            else
                target.previousDocument();
        }
        // the released/typed events of the same keystroke are swallowed too, so
        // that no component reacts to a half handled hotkey
        evt.consume();
        return true;
    }
}
