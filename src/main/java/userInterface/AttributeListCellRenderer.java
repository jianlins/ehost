package userInterface;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * Paints the attributes of an annotation, marking in red and bold the ones
 * whose value differs from the value given by the other annotator.
 */
public class AttributeListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    /** The colour used for the attributes the two annotators disagree on. */
    public static final Color DIFFERENCE_FOREGROUND = new Color(178, 34, 34);

    /** The background used for those attributes when the row is not selected. */
    public static final Color DIFFERENCE_BACKGROUND = new Color(255, 244, 214);

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        Component component = super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);

        boolean different = (value instanceof AttributeListEntry)
                && ((AttributeListEntry) value).different;

        Font font = component.getFont();
        if (font != null) {
            component.setFont(font.deriveFont(different ? Font.BOLD : Font.PLAIN));
        }

        if (different) {
            setToolTipText("This value is different from the value of the other annotation.");
            if (!isSelected) {
                component.setForeground(DIFFERENCE_FOREGROUND);
                component.setBackground(DIFFERENCE_BACKGROUND);
            }
        } else {
            setToolTipText(null);
        }

        return component;
    }
}
