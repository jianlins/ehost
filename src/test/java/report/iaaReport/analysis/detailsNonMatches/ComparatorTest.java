package report.iaaReport.analysis.detailsNonMatches;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Vector;

import resultEditor.annotations.Annotation;
import resultEditor.annotations.SpanSetDef;
import resultEditor.annotations.AnnotationAttributeDef;
import resultEditor.annotations.AnnotationRelationship;
import resultEditor.annotations.AnnotationRelationshipDef;
import resultEditor.annotations.Article;

/**
 * Test class for Comparator.java
 */
public class ComparatorTest {

    private Annotation annotation1;
    private Annotation annotation2;

    /**
     * Setup method to initialize test data before each test
     */
    @BeforeEach
    public void setUp() {
        // Initialize test annotations
        annotation1 = new Annotation();
        annotation1.annotationclass = "Class1";
        annotation1.setAnnotator("Annotator1");
        annotation1.spanset = new SpanSetDef();
        annotation1.spanset.setOnlySpan(0, 10);
        annotation1.comments = "Test comment";
        annotation1.uniqueIndex = 1;

        annotation2 = new Annotation();
        annotation2.annotationclass = "Class1";
        annotation2.setAnnotator("Annotator1");
        annotation2.spanset = new SpanSetDef();
        annotation2.spanset.setOnlySpan(0, 10);
        annotation2.comments = "Test comment";
        annotation2.uniqueIndex = 2;
    }

    /**
     * Test for the equalSpans method
     */
    @Test
    @DisplayName("Test equalSpans method")
    public void testEqualSpans() {
        try {
            // Test with equal spans
            assertTrue(Comparator.equalSpans(annotation1, annotation2), 
                "Annotations with same spans should return true");

            // Test with different spans
            annotation2.spanset = new SpanSetDef();
            annotation2.spanset.setOnlySpan(5, 15);
            assertFalse(Comparator.equalSpans(annotation1, annotation2), 
                "Annotations with different spans should return false");

            // Test with null annotations
            assertFalse(Comparator.equalSpans(null, annotation2), 
                "Null annotation should return false");
            assertFalse(Comparator.equalSpans(annotation1, null), 
                "Null annotation should return false");

            // Test with empty spans
            Annotation emptySpanAnnotation = new Annotation();
            emptySpanAnnotation.spanset = new SpanSetDef();
            assertFalse(Comparator.equalSpans(annotation1, emptySpanAnnotation), 
                "Annotation with empty span should return false");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the isSpanOverLap method
     */
    @Test
    @DisplayName("Test isSpanOverLap method")
    public void testIsSpanOverLap() {
        try {
            // Test with overlapping spans
            annotation2.spanset = new SpanSetDef();
            annotation2.spanset.setOnlySpan(5, 15);
            assertTrue(Comparator.isSpanOverLap(annotation1, annotation2), 
                "Annotations with overlapping spans should return true");

            // Test with non-overlapping spans
            annotation2.spanset = new SpanSetDef();
            annotation2.spanset.setOnlySpan(20, 30);
            assertFalse(Comparator.isSpanOverLap(annotation1, annotation2), 
                "Annotations with non-overlapping spans should return false");

            // Test with null annotations
            assertFalse(Comparator.isSpanOverLap(null, annotation2), 
                "Null annotation should return false");
            assertFalse(Comparator.isSpanOverLap(annotation1, null), 
                "Null annotation should return false");

            // Test with empty spans
            Annotation emptySpanAnnotation = new Annotation();
            emptySpanAnnotation.spanset = new SpanSetDef();
            assertFalse(Comparator.isSpanOverLap(annotation1, emptySpanAnnotation), 
                "Annotation with empty span should return false");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the equalClasses method
     */
    @Test
    @DisplayName("Test equalClasses method")
    public void testEqualClasses() {
        try {
            // Test with equal classes
            assertTrue(Comparator.equalClasses(annotation1, annotation2), 
                "Annotations with same classes should return true");

            // Test with different classes
            annotation2.annotationclass = "Class2";
            assertFalse(Comparator.equalClasses(annotation1, annotation2), 
                "Annotations with different classes should return false");

            // Test with null classes - should throw exception
            Annotation nullClassAnnotation = new Annotation();
            nullClassAnnotation.setAnnotator("Annotator1");

            Exception exception = assertThrows(Exception.class, () -> {
                Comparator.equalClasses(annotation1, nullClassAnnotation);
            });
            assertTrue(exception.getMessage().contains("fail to compare classes"), 
                "Exception message should indicate failure to compare classes");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the equalAnnotator method
     */
    @Test
    @DisplayName("Test equalAnnotator method")
    public void testEqualAnnotator() {
        try {
            // Test with equal annotators
            assertTrue(Comparator.equalAnnotator(annotation1, annotation2), 
                "Annotations with same annotators should return true");

            // Test with different annotators
            annotation2.setAnnotator("Annotator2");
            assertFalse(Comparator.equalAnnotator(annotation1, annotation2), 
                "Annotations with different annotators should return false");

            // Test with null annotators - should throw exception
            Annotation nullAnnotatorAnnotation = new Annotation();
            nullAnnotatorAnnotation.annotationclass = "Class1";

            Exception exception = assertThrows(Exception.class, () -> {
                Comparator.equalAnnotator(annotation1, nullAnnotatorAnnotation);
            });
            assertTrue(exception.getMessage().contains("fail to determin"), 
                "Exception message should indicate failure to determine annotators");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the checkAnnotator method
     */
    @Test
    @DisplayName("Test checkAnnotator method")
    public void testCheckAnnotator() {
        try {
            // Test with matching annotator
            assertTrue(Comparator.checkAnnotator(annotation1, "Annotator1"), 
                "Annotation with matching annotator should return true");

            // Test with non-matching annotator
            assertFalse(Comparator.checkAnnotator(annotation1, "Annotator2"), 
                "Annotation with non-matching annotator should return false");

            // Test with null annotator - should throw exception
            Exception exception = assertThrows(Exception.class, () -> {
                Comparator.checkAnnotator(annotation1, null);
            });
            assertEquals("1109020357", exception.getMessage(), 
                "Exception message should be the expected error code");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the equalComments method
     */
    @Test
    @DisplayName("Test equalComments method")
    public void testEqualComments() {
        try {
            // Test with equal comments
            assertTrue(Comparator.equalComments(annotation1, annotation2), 
                "Annotations with same comments should return true");

            // Test with different comments
            annotation2.comments = "Different comment";
            assertFalse(Comparator.equalComments(annotation1, annotation2), 
                "Annotations with different comments should return false");

            // Test with null comments
            annotation1.comments = null;
            annotation2.comments = null;
            assertTrue(Comparator.equalComments(annotation1, annotation2), 
                "Annotations with null comments should return true");

            // Test with one null comment and one empty comment
            annotation1.comments = "";
            assertTrue(Comparator.equalComments(annotation1, annotation2), 
                "Annotation with empty comment and null comment should return true");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the equalAttributes method
     */
    @Test
    @DisplayName("Test equalAttributes method")
    public void testEqualAttributes() {
        try {
            // Set up attributes for both annotations
            annotation1.attributes = new Vector<>();
            annotation2.attributes = new Vector<>();

            // Add identical attributes to both annotations
            AnnotationAttributeDef attr1 = new AnnotationAttributeDef();
            attr1.name = "attribute1";
            attr1.value = "value1";

            AnnotationAttributeDef attr2 = new AnnotationAttributeDef();
            attr2.name = "attribute2";
            attr2.value = "value2";

            annotation1.attributes.add(attr1);
            annotation1.attributes.add(attr2);

            AnnotationAttributeDef attr1Copy = new AnnotationAttributeDef();
            attr1Copy.name = "attribute1";
            attr1Copy.value = "value1";

            AnnotationAttributeDef attr2Copy = new AnnotationAttributeDef();
            attr2Copy.name = "attribute2";
            attr2Copy.value = "value2";

            annotation2.attributes.add(attr1Copy);
            annotation2.attributes.add(attr2Copy);

            // Test with equal attributes
            assertTrue(Comparator.equalAttributes(annotation1, annotation2), 
                "Annotations with same attributes should return true");

            // Test with different attribute values
            annotation2.attributes.clear();
            AnnotationAttributeDef attr1Modified = new AnnotationAttributeDef();
            attr1Modified.name = "attribute1";
            attr1Modified.value = "differentValue";

            annotation2.attributes.add(attr1Modified);
            annotation2.attributes.add(attr2Copy);

            assertFalse(Comparator.equalAttributes(annotation1, annotation2), 
                "Annotations with different attribute values should return false");

            // Test with different number of attributes
            annotation2.attributes.clear();
            annotation2.attributes.add(attr1Copy);

            assertFalse(Comparator.equalAttributes(annotation1, annotation2), 
                "Annotations with different number of attributes should return false");

            // Test with null attributes
            annotation1.attributes = null;
            annotation2.attributes = null;

            assertTrue(Comparator.equalAttributes(annotation1, annotation2), 
                "Annotations with null attributes should return true");

            // Test with one null and one empty attributes
            annotation1.attributes = new Vector<>();

            assertTrue(Comparator.equalAttributes(annotation1, annotation2), 
                "Annotation with empty attributes and null attributes should return true");

        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the equalRelationships method
     */
    @Test
    @DisplayName("Test equalRelationships method")
    public void testEqualRelationships() {
        try {
            // Create a test article
            Article article = new Article("testFile.txt");

            // Create a third annotation to be the target of relationships
            Annotation targetAnnotation = new Annotation();
            targetAnnotation.annotationclass = "TargetClass";
            targetAnnotation.setAnnotator("Annotator1");
            targetAnnotation.spanset = new SpanSetDef();
            targetAnnotation.spanset.setOnlySpan(20, 30);
            targetAnnotation.uniqueIndex = 3;

            // Add all annotations to the article
            article.annotations.add(annotation1);
            article.annotations.add(annotation2);
            article.annotations.add(targetAnnotation);

            // Set up relationships for both annotations
            annotation1.relationships = new Vector<>();
            annotation2.relationships = new Vector<>();

            // Create identical relationships for both annotations
            AnnotationRelationship rel1 = new AnnotationRelationship("has_target");
            rel1.linkedAnnotations = new Vector<>();

            AnnotationRelationshipDef relDef1 = new AnnotationRelationshipDef();
            relDef1.linkedAnnotationIndex = targetAnnotation.uniqueIndex;
            rel1.linkedAnnotations.add(relDef1);

            annotation1.relationships.add(rel1);

            AnnotationRelationship rel2 = new AnnotationRelationship("has_target");
            rel2.linkedAnnotations = new Vector<>();

            AnnotationRelationshipDef relDef2 = new AnnotationRelationshipDef();
            relDef2.linkedAnnotationIndex = targetAnnotation.uniqueIndex;
            rel2.linkedAnnotations.add(relDef2);

            annotation2.relationships.add(rel2);

            // Test with equal relationships
            assertTrue(Comparator.equalRelationships(annotation1, annotation2, article.filename, false), 
                "Annotations with same relationships should return true");

            // Test with different relationship types
            annotation2.relationships.clear();
            AnnotationRelationship relDifferent = new AnnotationRelationship("different_relation");
            relDifferent.linkedAnnotations = new Vector<>();
            relDifferent.linkedAnnotations.add(relDef2);
            annotation2.relationships.add(relDifferent);

            assertFalse(Comparator.equalRelationships(annotation1, annotation2, article.filename, false), 
                "Annotations with different relationship types should return false");

            // Test with different number of relationships
            annotation2.relationships.clear();

            assertFalse(Comparator.equalRelationships(annotation1, annotation2, article.filename, false), 
                "Annotations with different number of relationships should return false");

            // Test with null relationships
            annotation1.relationships = null;
            annotation2.relationships = null;

            assertTrue(Comparator.equalRelationships(annotation1, annotation2, article.filename, false), 
                "Annotations with null relationships should return true");

            // Test with one null and one empty relationships
            annotation1.relationships = new Vector<>();

            assertTrue(Comparator.equalRelationships(annotation1, annotation2, article.filename, false), 
                "Annotation with empty relationships and null relationships should return true");

        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }

    /**
     * Test for the equalAnnotations method
     */
    @Test
    @DisplayName("Test equalAnnotations method")
    public void testEqualAnnotations() {
        try {
            // Test with equal annotations
            assertTrue(Comparator.equalAnnotations(annotation1, annotation2), 
                "Equal annotations should return true");

            // Test with different spans
            annotation2.spanset = new SpanSetDef();
            annotation2.spanset.setOnlySpan(5, 15);
            assertFalse(Comparator.equalAnnotations(annotation1, annotation2), 
                "Annotations with different spans should return false");

            // Reset spans
            annotation2.spanset = new SpanSetDef();
            annotation2.spanset.setOnlySpan(0, 10);

            // Test with different annotators
            annotation2.setAnnotator("Annotator2");
            assertFalse(Comparator.equalAnnotations(annotation1, annotation2), 
                "Annotations with different annotators should return false");

            // Reset annotator
            annotation2.setAnnotator("Annotator1");

            // Test with different classes
            annotation2.annotationclass = "Class2";
            assertFalse(Comparator.equalAnnotations(annotation1, annotation2), 
                "Annotations with different classes should return false");
        } catch (Exception e) {
            fail("Exception occurred during test: " + e.getMessage());
        }
    }
}
