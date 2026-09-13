package userInterface;

import resultEditor.annotations.AnnotationAttributeDef;

/**
 * One row of an attribute list, on the annotation editor panel or on the
 * comparator panel. It knows whether its value differs from the value the other
 * annotator gave to the same attribute, so the renderer can point it out.
 */
public class AttributeListEntry {

    /** The name of the attribute. */
    public final String name;

    /** The value of the attribute, may be null. */
    public final String value;

    /** True when the other annotation holds a different value. */
    public final boolean different;

    public AttributeListEntry(String name, String value, boolean different) {
        this.name = name;
        this.value = value;
        this.different = different;
    }

    public AttributeListEntry(AnnotationAttributeDef attribute, boolean different) {
        this(attribute.name, attribute.value, different);
    }

    /**
     * The text shown in the list. It keeps the layout eHOST has always used:
     * {@code "name" = value}.
     */
    @Override
    public String toString() {
        if (value == null)
            return " \"" + name + "\"";

        return " \"" + name + "\" = " + value;
    }
}
