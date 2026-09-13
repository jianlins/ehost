package adjudication;

import adjudication.data.AdjudicationDepot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import relationship.simple.dataTypes.AttributeList;
import relationship.simple.dataTypes.AttributeSchemaDef;
import resultEditor.annotationClasses.AnnotationClass;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.AnnotationAttributeDef;
import resultEditor.annotations.Depot;
import userInterface.GUI;

import java.awt.Color;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Adding a new annotation in adjudication mode used to leave every attribute
 * empty: the annotation was stored in {@link AdjudicationDepot} while the
 * schema defaults were applied through {@link Depot}, which never found it.
 *
 * <p>These tests pin the two modes to the same outcome - a brand new annotation
 * carries the default value of every private and inherited public attribute.
 */
public class DefaultAttributeOnNewAnnotationTest {

    private static final String DOC = "doc1.txt";
    private static final String CONCEPT = "CONCEPT";

    private static final String PRIVATE_ATTRIBUTE = "severity";
    private static final String PRIVATE_DEFAULT = "mild";
    private static final String PUBLIC_ATTRIBUTE = "certainty";
    private static final String PUBLIC_DEFAULT = "certain";

    private static final String PHRASE = "chest pain";

    private GUI.ReviewMode savedReviewMode;
    private AttributeList savedAttributeSchemas;

    @BeforeEach
    void setUp() {
        savedReviewMode = GUI.reviewmode;
        savedAttributeSchemas = env.Parameters.AttributeSchemas;
        resetGlobalState();
        buildSchema();
    }

    @AfterEach
    void tearDown() {
        GUI.reviewmode = savedReviewMode;
        env.Parameters.AttributeSchemas = savedAttributeSchemas;
        resetGlobalState();
    }

    private void resetGlobalState() {
        new Depot().clear();
        AdjudicationDepot.clear();
        resultEditor.annotationClasses.Depot.clear();
    }

    /**
     * One annotation class holding a private attribute with a default, plus an
     * inherited public attribute with a default.
     */
    private void buildSchema() {
        resultEditor.annotationClasses.Depot.addElement(CONCEPT, "test", Color.RED, true, false);

        AnnotationClass conceptClass = new resultEditor.annotationClasses.Depot()
                .getAnnotatedClass(CONCEPT);
        assertNotNull(conceptClass, "the test schema should contain the class " + CONCEPT);
        conceptClass.inheritsPublicAttributes = true;
        conceptClass.privateAttributes = new Vector<AttributeSchemaDef>();
        conceptClass.privateAttributes.add(attribute(PRIVATE_ATTRIBUTE, PRIVATE_DEFAULT));

        env.Parameters.AttributeSchemas = new AttributeList();
        env.Parameters.AttributeSchemas.Add(attribute(PUBLIC_ATTRIBUTE, PUBLIC_DEFAULT));
    }

    private AttributeSchemaDef attribute(String name, String defaultValue) {
        Vector<String> allowed = new Vector<String>();
        allowed.add(defaultValue);
        allowed.add("other");

        AttributeSchemaDef attribute = new AttributeSchemaDef(name, allowed);
        attribute.setDefaultValue(defaultValue);
        return attribute;
    }

    // ------------------------------------------------------------------ //
    // tests
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("annotation mode: a new annotation gets the schema defaults")
    void annotationModeAppliesDefaults() {
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;

        int uniqueIndex = 1001;
        Depot depot = new Depot();
        depot.addANewAnnotation(DOC, PHRASE, 10, 10 + PHRASE.length(),
                "Mon Jan 01 00:00:00 MST 2024", CONCEPT, "annotator1", "eHOST_2010",
                null, null, uniqueIndex);

        depot.setAttributeDefault(DOC, uniqueIndex);

        Annotation annotation = depot.getAnnotationByUnique(DOC, uniqueIndex);
        assertNotNull(annotation, "the new annotation should be in the annotation depot");
        assertEquals(PRIVATE_DEFAULT, valueOf(annotation, PRIVATE_ATTRIBUTE));
        assertEquals(PUBLIC_DEFAULT, valueOf(annotation, PUBLIC_ATTRIBUTE));
    }

    @Test
    @DisplayName("adjudication mode: a new annotation gets the very same defaults")
    void adjudicationModeAppliesDefaults() {
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        int uniqueIndex = 2002;
        AdjudicationDepot depotOfAdj = new AdjudicationDepot();
        depotOfAdj.addANewAnnotation(DOC, PHRASE, 10, 10 + PHRASE.length(),
                "Mon Jan 01 00:00:00 MST 2024", CONCEPT, "ADJUDICATION", "eHOST_2010",
                null, null, uniqueIndex);

        depotOfAdj.setAttributeDefault(DOC, uniqueIndex);

        Annotation annotation = depotOfAdj.getAnnotationByUnique(DOC, uniqueIndex);
        assertNotNull(annotation, "the new annotation should be in the adjudication depot");
        assertEquals(PRIVATE_DEFAULT, valueOf(annotation, PRIVATE_ATTRIBUTE));
        assertEquals(PUBLIC_DEFAULT, valueOf(annotation, PUBLIC_ATTRIBUTE));
    }

    @Test
    @DisplayName("adjudication mode: applying defaults twice does not duplicate attributes")
    void adjudicationModeDoesNotDuplicateAttributes() {
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        int uniqueIndex = 3003;
        AdjudicationDepot depotOfAdj = new AdjudicationDepot();
        depotOfAdj.addANewAnnotation(DOC, PHRASE, 10, 10 + PHRASE.length(),
                "Mon Jan 01 00:00:00 MST 2024", CONCEPT, "ADJUDICATION", "eHOST_2010",
                null, null, uniqueIndex);

        depotOfAdj.setAttributeDefault(DOC, uniqueIndex);
        depotOfAdj.setAttributeDefault(DOC, uniqueIndex);

        Annotation annotation = depotOfAdj.getAnnotationByUnique(DOC, uniqueIndex);
        assertNotNull(annotation);
        assertEquals(2, annotation.attributes.size(),
                "each attribute should be recorded exactly once");
    }

    @Test
    @DisplayName("an unknown annotation, or an unknown class, is ignored quietly")
    void missingAnnotationIsIgnored() {
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        // nothing was ever added under this unique index
        new AdjudicationDepot().setAttributeDefault(DOC, 4004);

        int uniqueIndex = 5005;
        AdjudicationDepot depotOfAdj = new AdjudicationDepot();
        depotOfAdj.addANewAnnotation(DOC, PHRASE, 10, 10 + PHRASE.length(),
                "Mon Jan 01 00:00:00 MST 2024", "NOT_IN_SCHEMA", "ADJUDICATION", "eHOST_2010",
                null, null, uniqueIndex);

        depotOfAdj.setAttributeDefault(DOC, uniqueIndex);

        Annotation annotation = depotOfAdj.getAnnotationByUnique(DOC, uniqueIndex);
        assertNotNull(annotation);
        assertEquals(null, valueOf(annotation, PRIVATE_ATTRIBUTE));
    }

    // ------------------------------------------------------------------ //
    // helpers
    // ------------------------------------------------------------------ //

    private String valueOf(Annotation annotation, String attributeName) {
        if (annotation.attributes == null) {
            return null;
        }
        for (AnnotationAttributeDef attribute : annotation.attributes) {
            if ((attribute != null) && (attributeName.equals(attribute.name))) {
                return attribute.value;
            }
        }
        return null;
    }
}
