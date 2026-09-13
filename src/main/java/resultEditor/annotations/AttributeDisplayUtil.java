package resultEditor.annotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import env.Parameters.AttributeDisplay;
import relationship.simple.dataTypes.AttributeSchemaDef;
import resultEditor.annotationClasses.AnnotationClass;

/**
 * Helper that puts the attributes of an annotation into the order chosen by the
 * user, and that tells which attributes hold different values between two
 * annotations.
 *
 * <p>Annotations keep their attributes in the order they were read from disk,
 * which is not necessarily the same for two annotators. Listing them in a
 * stable order makes a side by side comparison readable.
 */
public final class AttributeDisplayUtil {

    private AttributeDisplayUtil() {
    }

    /**
     * Get the attributes of an annotation in the order configured by the user.
     *
     * @param annotation the annotation to read the attributes from; may be null
     * @return a new vector holding the non-null attributes of the annotation,
     *         never null
     */
    public static Vector<AnnotationAttributeDef> getAttributesInDisplayOrder(Annotation annotation) {
        if (annotation == null)
            return new Vector<AnnotationAttributeDef>();

        return sortForDisplay(annotation.annotationclass, annotation.attributes);
    }

    /**
     * Sort a list of attributes for display.
     *
     * @param annotationClass the class of the annotation owning these
     *                        attributes, used to look up the schema order
     * @param attributes      the attributes to sort; may be null
     * @return a new vector holding the non-null attributes, never null
     */
    public static Vector<AnnotationAttributeDef> sortForDisplay(String annotationClass,
            Vector<AnnotationAttributeDef> attributes) {

        Vector<AnnotationAttributeDef> ordered = new Vector<AnnotationAttributeDef>();
        if (attributes == null)
            return ordered;

        for (AnnotationAttributeDef attribute : attributes) {
            if ((attribute == null) || (attribute.name == null))
                continue;
            ordered.add(attribute);
        }

        AttributeDisplay.Order order = AttributeDisplay.order;
        if (order == null)
            order = AttributeDisplay.Order.SCHEMA;

        switch (order) {
            case NAME:
                Collections.sort(ordered, new java.util.Comparator<AnnotationAttributeDef>() {
                    public int compare(AnnotationAttributeDef left, AnnotationAttributeDef right) {
                        return compareNames(left.name, right.name);
                    }
                });
                break;

            case SCHEMA:
                final Map<String, Integer> schemaOrder = getSchemaOrder(annotationClass);
                Collections.sort(ordered, new java.util.Comparator<AnnotationAttributeDef>() {
                    public int compare(AnnotationAttributeDef left, AnnotationAttributeDef right) {
                        int leftIndex = indexInSchema(schemaOrder, left.name);
                        int rightIndex = indexInSchema(schemaOrder, right.name);
                        if (leftIndex != rightIndex)
                            return (leftIndex < rightIndex) ? -1 : 1;

                        // attributes outside the schema are listed last, and
                        // among themselves by name
                        if (leftIndex == Integer.MAX_VALUE)
                            return compareNames(left.name, right.name);

                        return 0;
                    }
                });
                break;

            default:
                // UNSORTED: keep the order the attributes were stored in
                break;
        }

        return ordered;
    }

    /**
     * Find the attributes that hold a different value in the two annotations.
     * An attribute that is only present on one side counts as a difference.
     *
     * @return the (trimmed) names of the differing attributes, never null
     */
    public static Set<String> getDifferingAttributeNames(Annotation one, Annotation other) {
        Set<String> differences = new HashSet<String>();
        if ((one == null) || (other == null))
            return differences;

        Map<String, String> valuesOfOne = toValueMap(one);
        Map<String, String> valuesOfOther = toValueMap(other);

        Set<String> names = new HashSet<String>();
        names.addAll(valuesOfOne.keySet());
        names.addAll(valuesOfOther.keySet());

        for (String name : names) {
            if (!sameValue(valuesOfOne.get(name), valuesOfOther.get(name)))
                differences.add(name);
        }

        return differences;
    }

    /**
     * Tell whether the given attribute name is part of a set of differing
     * attribute names. The name is matched after trimming, the way
     * {@link #getDifferingAttributeNames(Annotation, Annotation)} records it.
     */
    public static boolean isDifferent(Set<String> differingNames, String attributeName) {
        if ((differingNames == null) || (differingNames.isEmpty()) || (attributeName == null))
            return false;

        return differingNames.contains(attributeName.trim());
    }

    /**
     * Sort the rows of the attribute editor by attribute name, but only when
     * the user asked for the attributes to be listed by name. The other orders
     * leave the rows where the caller put them, which is the schema order.
     *
     * @param entries the rows of the attribute editor, may be null
     */
    public static void sortEditorRowsByNameIfRequested(
            Vector<resultEditor.relationship.iListable> entries) {

        if ((entries == null) || (AttributeDisplay.order != AttributeDisplay.Order.NAME))
            return;

        Collections.sort(entries, new java.util.Comparator<resultEditor.relationship.iListable>() {
            public int compare(resultEditor.relationship.iListable left,
                    resultEditor.relationship.iListable right) {
                return compareNames(nameOf(left), nameOf(right));
            }
        });
    }

    private static String nameOf(resultEditor.relationship.iListable entry) {
        if (entry == null)
            return null;

        try {
            return entry.getSelectedItem(0);
        } catch (Exception ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ //
    // helpers
    // ------------------------------------------------------------------ //

    /**
     * Build the position of every attribute name defined in the schema for the
     * given annotation class. Public (inherited) attributes come first, then
     * the private attributes of the class, which is the order used by the
     * attribute editor.
     */
    private static Map<String, Integer> getSchemaOrder(String annotationClass) {
        Map<String, Integer> order = new LinkedHashMap<String, Integer>();

        try {
            AnnotationClass currentClass = null;
            if ((annotationClass != null) && (annotationClass.trim().length() > 0)) {
                currentClass = new resultEditor.annotationClasses.Depot()
                        .getAnnotatedClass(annotationClass);
            }

            boolean inheritsPublicAttributes = (currentClass == null)
                    || currentClass.inheritsPublicAttributes;

            if (inheritsPublicAttributes && (env.Parameters.AttributeSchemas != null)) {
                for (AttributeSchemaDef attribute : env.Parameters.AttributeSchemas.getAttributes())
                    remember(order, attribute);
            }

            if ((currentClass != null) && (currentClass.privateAttributes != null)) {
                for (AttributeSchemaDef attribute : currentClass.privateAttributes)
                    remember(order, attribute);
            }

        } catch (Exception ex) {
            log.LoggingToFile.log(Level.WARNING, "error 2506090001:: fail to read the schema "
                    + "order of the attributes:: " + ex.toString());
        }

        return order;
    }

    private static void remember(Map<String, Integer> order, AttributeSchemaDef attribute) {
        if ((attribute == null) || (attribute.getName() == null))
            return;

        String name = attribute.getName().trim();
        if ((name.length() < 1) || order.containsKey(name))
            return;

        order.put(name, order.size());
    }

    private static int indexInSchema(Map<String, Integer> schemaOrder, String attributeName) {
        if ((schemaOrder == null) || (attributeName == null))
            return Integer.MAX_VALUE;

        Integer index = schemaOrder.get(attributeName.trim());
        return (index == null) ? Integer.MAX_VALUE : index.intValue();
    }

    private static int compareNames(String left, String right) {
        if (left == null)
            return (right == null) ? 0 : 1;
        if (right == null)
            return -1;

        int result = left.trim().compareToIgnoreCase(right.trim());
        if (result != 0)
            return result;

        return left.trim().compareTo(right.trim());
    }

    private static Map<String, String> toValueMap(Annotation annotation) {
        Map<String, String> values = new HashMap<String, String>();
        if ((annotation == null) || (annotation.attributes == null))
            return values;

        for (AnnotationAttributeDef attribute : annotation.attributes) {
            if ((attribute == null) || (attribute.name == null))
                continue;

            String name = attribute.name.trim();
            if (name.length() < 1)
                continue;

            // an attribute listed twice keeps its first value, the way the
            // editor shows it
            if (!values.containsKey(name))
                values.put(name, attribute.value);
        }

        return values;
    }

    /** Null and blank values are considered the same, as "not set". */
    private static boolean sameValue(String one, String other) {
        String left = (one == null) ? "" : one.trim();
        String right = (other == null) ? "" : other.trim();

        return left.equals(right);
    }
}
