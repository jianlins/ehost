package resultEditor.annotations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import relationship.simple.dataTypes.AttributeList;
import relationship.simple.dataTypes.AttributeSchemaDef;
import resultEditor.annotationClasses.AnnotationClass;

import java.awt.Color;
import java.util.Arrays;
import java.util.Set;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two annotators rarely store the attributes of an annotation in the same
 * order, which made the side by side comparison of the adjudication mode hard
 * to read. These tests pin the ordering rules and the detection of the values
 * the two annotators disagree on.
 */
public class AttributeDisplayUtilTest {

    private static final String CONCEPT = "CONCEPT";

    private env.Parameters.AttributeDisplay.Order savedOrder;
    private AttributeList savedAttributeSchemas;

    @BeforeEach
    void setUp() {
        savedOrder = env.Parameters.AttributeDisplay.order;
        savedAttributeSchemas = env.Parameters.AttributeSchemas;
        resultEditor.annotationClasses.Depot.clear();
        buildSchema();
    }

    @AfterEach
    void tearDown() {
        env.Parameters.AttributeDisplay.order = savedOrder;
        env.Parameters.AttributeSchemas = savedAttributeSchemas;
        resultEditor.annotationClasses.Depot.clear();
    }

    /**
     * A class with two private attributes, in a deliberate non alphabetical
     * order, plus one inherited public attribute.
     */
    private void buildSchema() {
        resultEditor.annotationClasses.Depot.addElement(CONCEPT, "test", Color.RED, true, false);

        AnnotationClass conceptClass = new resultEditor.annotationClasses.Depot()
                .getAnnotatedClass(CONCEPT);
        assertNotNull(conceptClass, "the test schema should contain the class " + CONCEPT);
        conceptClass.inheritsPublicAttributes = true;
        conceptClass.privateAttributes = new Vector<AttributeSchemaDef>();
        conceptClass.privateAttributes.add(attribute("severity"));
        conceptClass.privateAttributes.add(attribute("anatomy"));

        env.Parameters.AttributeSchemas = new AttributeList();
        env.Parameters.AttributeSchemas.Add(attribute("certainty"));
    }

    private AttributeSchemaDef attribute(String name) {
        Vector<String> allowed = new Vector<String>();
        allowed.add("yes");
        allowed.add("no");

        return new AttributeSchemaDef(name, allowed);
    }

    private Annotation annotation(String... namesAndValues) {
        Annotation annotation = new Annotation();
        annotation.annotationclass = CONCEPT;
        annotation.attributes = new Vector<AnnotationAttributeDef>();

        for (int i = 0; i < namesAndValues.length; i += 2)
            annotation.attributes.add(
                    new AnnotationAttributeDef(namesAndValues[i], namesAndValues[i + 1]));

        return annotation;
    }

    private Vector<String> namesOf(Vector<AnnotationAttributeDef> attributes) {
        Vector<String> names = new Vector<String>();
        for (AnnotationAttributeDef attribute : attributes)
            names.add(attribute.name);

        return names;
    }

    private Vector<String> expect(String... names) {
        return new Vector<String>(Arrays.asList(names));
    }

    // ------------------------------------------------------------------ //
    // ordering
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("schema order: public attributes first, then the private ones as defined")
    void schemaOrderFollowsTheSchema() {
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.SCHEMA;

        Annotation annotation = annotation(
                "anatomy", "chest",
                "certainty", "yes",
                "severity", "mild");

        assertEquals(expect("certainty", "severity", "anatomy"),
                namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(annotation)));
    }

    @Test
    @DisplayName("schema order: attributes outside the schema are listed last, by name")
    void schemaOrderPutsUnknownAttributesLast() {
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.SCHEMA;

        Annotation annotation = annotation(
                "zebra", "1",
                "anatomy", "chest",
                "alien", "2",
                "certainty", "yes");

        assertEquals(expect("certainty", "anatomy", "alien", "zebra"),
                namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(annotation)));
    }

    @Test
    @DisplayName("name order: attributes are listed alphabetically, ignoring the case")
    void nameOrderSortsAlphabetically() {
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.NAME;

        Annotation annotation = annotation(
                "severity", "mild",
                "Anatomy", "chest",
                "certainty", "yes");

        assertEquals(expect("Anatomy", "certainty", "severity"),
                namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(annotation)));
    }

    @Test
    @DisplayName("no sorting: attributes keep the order they were read in")
    void unsortedKeepsTheStoredOrder() {
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.UNSORTED;

        Annotation annotation = annotation(
                "severity", "mild",
                "certainty", "yes",
                "anatomy", "chest");

        assertEquals(expect("severity", "certainty", "anatomy"),
                namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(annotation)));
    }

    @Test
    @DisplayName("two annotators storing the same attributes differently end up aligned")
    void bothAnnotatorsGetTheSameOrder() {
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.SCHEMA;

        Annotation firstAnnotator = annotation(
                "anatomy", "chest", "certainty", "yes", "severity", "mild");
        Annotation secondAnnotator = annotation(
                "severity", "severe", "anatomy", "chest", "certainty", "no");

        assertEquals(namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(firstAnnotator)),
                namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(secondAnnotator)));
    }

    @Test
    @DisplayName("a null annotation, or one without attributes, gives an empty list")
    void nullAnnotationIsHandled() {
        assertTrue(AttributeDisplayUtil.getAttributesInDisplayOrder(null).isEmpty());

        Annotation annotation = new Annotation();
        annotation.attributes = null;
        assertTrue(AttributeDisplayUtil.getAttributesInDisplayOrder(annotation).isEmpty());
    }

    @Test
    @DisplayName("attributes without a name are dropped instead of breaking the list")
    void namelessAttributesAreDropped() {
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.NAME;

        Annotation annotation = annotation("certainty", "yes");
        annotation.attributes.add(null);
        annotation.attributes.add(new AnnotationAttributeDef(null, "orphan"));

        assertEquals(expect("certainty"),
                namesOf(AttributeDisplayUtil.getAttributesInDisplayOrder(annotation)));
    }

    // ------------------------------------------------------------------ //
    // differences
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("only the attributes holding another value are reported as different")
    void differentValuesAreReported() {
        Annotation firstAnnotator = annotation(
                "certainty", "yes", "severity", "mild", "anatomy", "chest");
        Annotation secondAnnotator = annotation(
                "certainty", "no", "severity", "mild", "anatomy", "chest");

        Set<String> differences =
                AttributeDisplayUtil.getDifferingAttributeNames(firstAnnotator, secondAnnotator);

        assertEquals(1, differences.size());
        assertTrue(AttributeDisplayUtil.isDifferent(differences, "certainty"));
        assertFalse(AttributeDisplayUtil.isDifferent(differences, "severity"));
        assertFalse(AttributeDisplayUtil.isDifferent(differences, "anatomy"));
    }

    @Test
    @DisplayName("an attribute only one annotator filled in counts as a difference")
    void missingAttributeIsADifference() {
        Annotation firstAnnotator = annotation("certainty", "yes", "severity", "mild");
        Annotation secondAnnotator = annotation("certainty", "yes");

        Set<String> differences =
                AttributeDisplayUtil.getDifferingAttributeNames(firstAnnotator, secondAnnotator);

        assertTrue(AttributeDisplayUtil.isDifferent(differences, "severity"));
        assertFalse(AttributeDisplayUtil.isDifferent(differences, "certainty"));
    }

    @Test
    @DisplayName("blank, null and surrounding spaces do not count as a difference")
    void blankValuesAreTheSameAsMissingOnes() {
        Annotation firstAnnotator = annotation("certainty", " yes ");
        firstAnnotator.attributes.add(new AnnotationAttributeDef("severity", null));

        Annotation secondAnnotator = annotation("certainty", "yes", "severity", "   ");

        assertTrue(AttributeDisplayUtil
                .getDifferingAttributeNames(firstAnnotator, secondAnnotator).isEmpty());
    }

    @Test
    @DisplayName("without a second annotation there is nothing to highlight")
    void noCounterpartMeansNoDifference() {
        Annotation annotation = annotation("certainty", "yes");

        assertTrue(AttributeDisplayUtil.getDifferingAttributeNames(annotation, null).isEmpty());
        assertTrue(AttributeDisplayUtil.getDifferingAttributeNames(null, annotation).isEmpty());
        assertFalse(AttributeDisplayUtil.isDifferent(null, "certainty"));
    }
}
