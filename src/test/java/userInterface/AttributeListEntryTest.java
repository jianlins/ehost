package userInterface;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import resultEditor.annotations.AnnotationAttributeDef;

import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rows of the attribute lists keep the wording eHOST has always used, and
 * the values two annotators disagree on stand out.
 */
class AttributeListEntryTest {

    private final JList list = new JList();
    private final AttributeListCellRenderer renderer = new AttributeListCellRenderer();

    @Test
    @DisplayName("a row reads: \"name\" = value")
    void rowKeepsTheHistoricalWording() {
        AttributeListEntry entry =
                new AttributeListEntry(new AnnotationAttributeDef("certainty", "positive"), false);

        assertEquals(" \"certainty\" = positive", entry.toString());
    }

    @Test
    @DisplayName("an attribute without a value only shows its name")
    void rowWithoutValueOnlyShowsTheName() {
        assertEquals(" \"certainty\"", new AttributeListEntry("certainty", null, false).toString());
    }

    @Test
    @DisplayName("a differing value is painted in red and bold")
    void differingValueIsHighlighted() {
        Component rendered = render(new AttributeListEntry("certainty", "positive", true));

        assertEquals(AttributeListCellRenderer.DIFFERENCE_FOREGROUND, rendered.getForeground());
        assertEquals(AttributeListCellRenderer.DIFFERENCE_BACKGROUND, rendered.getBackground());
        assertTrue(rendered.getFont().isBold(), "a difference should be bold");
        assertNotEquals(null, ((JLabel) rendered).getToolTipText());
    }

    @Test
    @DisplayName("a matching value keeps the plain look of the list")
    void matchingValueIsNotHighlighted() {
        Component rendered = render(new AttributeListEntry("certainty", "positive", false));

        assertEquals(list.getForeground(), rendered.getForeground());
        assertFalse(rendered.getFont().isBold(), "a matching value should stay plain");
        assertEquals(null, ((JLabel) rendered).getToolTipText());
    }

    @Test
    @DisplayName("the bold of a difference does not leak into the next rows")
    void boldIsResetBetweenRows() {
        render(new AttributeListEntry("certainty", "positive", true));
        Component rendered = render(new AttributeListEntry("severity", "mild", false));

        assertEquals(Font.PLAIN, rendered.getFont().getStyle());
    }

    private Component render(AttributeListEntry entry) {
        return renderer.getListCellRendererComponent(list, entry, 0, false, false);
    }
}
