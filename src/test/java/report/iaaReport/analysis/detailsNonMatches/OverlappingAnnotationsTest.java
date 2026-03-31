package report.iaaReport.analysis.detailsNonMatches;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Vector;

import report.iaaReport.IAA;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.SpanSetDef;

/**
 * Tests for overlapping annotations at the same span with different classes.
 * Verifies that when multiple annotations exist at the same span (e.g., "CONCEPT" and "CON2"),
 * they are correctly grouped in the same row and properly compared.
 */
public class OverlappingAnnotationsTest {

    private boolean originalCheckClass;
    private boolean originalCheckOverlappedSpans;
    private boolean originalCheckAttributes;
    private boolean originalCheckRelationship;
    private boolean originalCheckComment;

    @BeforeEach
    public void setUp() {
        // Save original IAA flags
        originalCheckClass = IAA.CHECK_CLASS;
        originalCheckOverlappedSpans = IAA.CHECK_OVERLAPPED_SPANS;
        originalCheckAttributes = IAA.CHECK_ATTRIBUTES;
        originalCheckRelationship = IAA.CHECK_RELATIONSHIP;
        originalCheckComment = IAA.CHECK_COMMENT;

        // Set default flags for tests
        IAA.CHECK_CLASS = true;
        IAA.CHECK_OVERLAPPED_SPANS = false;
        IAA.CHECK_ATTRIBUTES = false;
        IAA.CHECK_RELATIONSHIP = false;
        IAA.CHECK_COMMENT = false;

        // Clear the static depot
        new Depot().clear();
    }

    @AfterEach
    public void tearDown() {
        // Restore original IAA flags
        IAA.CHECK_CLASS = originalCheckClass;
        IAA.CHECK_OVERLAPPED_SPANS = originalCheckOverlappedSpans;
        IAA.CHECK_ATTRIBUTES = originalCheckAttributes;
        IAA.CHECK_RELATIONSHIP = originalCheckRelationship;
        IAA.CHECK_COMMENT = originalCheckComment;

        // Clear depot
        new Depot().clear();
        AnalyzedResult.clear();
    }

    private Annotation createAnnotation(String text, String annotator, String annotationClass,
                                         int spanStart, int spanEnd, int uniqueIndex) {
        Annotation ann = new Annotation();
        ann.annotationText = text;
        ann.setAnnotator(annotator);
        ann.annotationclass = annotationClass;
        ann.spanset = new SpanSetDef();
        ann.spanset.setOnlySpan(spanStart, spanEnd);
        ann.spanstart = spanStart;
        ann.spanend = spanEnd;
        ann.uniqueIndex = uniqueIndex;
        ann.mentionid = "TEST_" + uniqueIndex;
        return ann;
    }

    // ========== AnalyzedArticle.addToExistingRow tests ==========

    @Test
    @DisplayName("addToExistingRow should add annotation with same span but different class")
    public void testAddToExistingRow_sameSpanDifferentClass() throws Exception {
        String[] annotators = {"a1", "a2"};

        // Create first annotation
        Annotation concept = createAnnotation("test text", "a1", "CONCEPT", 10, 20, 1);

        // Create second annotation at same span but different class
        Annotation con2 = createAnnotation("test text", "a1", "CON2", 10, 20, 2);

        // Create article and initialize with first annotation
        AnalyzedArticle article = new AnalyzedArticle("doc1.txt");
        article.initRow(concept, annotators);

        // Verify initial state
        assertEquals(1, article.rows.size(), "Should have 1 row");
        assertEquals(1, article.rows.get(0).mainAnnotations.size(), "Row should have 1 main annotation");

        // Add second annotation to existing row
        article.addToExistingRow(con2);

        // Verify both annotations are in the same row
        assertEquals(1, article.rows.size(), "Should still have 1 row");
        assertEquals(2, article.rows.get(0).mainAnnotations.size(),
                "Row should now have 2 main annotations");
        assertEquals("CONCEPT", article.rows.get(0).mainAnnotations.get(0).annotationclass);
        assertEquals("CON2", article.rows.get(0).mainAnnotations.get(1).annotationclass);
    }

    @Test
    @DisplayName("addToExistingRow should not duplicate annotation with same uniqueIndex")
    public void testAddToExistingRow_duplicateUniqueIndex() throws Exception {
        String[] annotators = {"a1", "a2"};

        Annotation concept = createAnnotation("test text", "a1", "CONCEPT", 10, 20, 1);

        AnalyzedArticle article = new AnalyzedArticle("doc1.txt");
        article.initRow(concept, annotators);

        // Try to add same annotation again
        article.addToExistingRow(concept);

        // Should still have 1 annotation
        assertEquals(1, article.rows.get(0).mainAnnotations.size(),
                "Should not duplicate annotation with same uniqueIndex");
    }

    @Test
    @DisplayName("addToExistingRow should not duplicate when row has multiple overlapping entries")
    public void testAddToExistingRow_noDuplicateInMultiEntryRow() throws Exception {
        String[] annotators = {"a1", "a2", "a3"};

        // CONCEPT at (332,386) and CON2 at (345,362) — overlapping spans
        Annotation concept = createAnnotation("which showed spread", "a1", "CONCEPT", 332, 386, 10);
        Annotation con2 = createAnnotation("spread of dye", "a1", "CON2", 345, 362, 11);

        AnalyzedArticle article = new AnalyzedArticle("doc1.txt");
        article.initRow(concept, annotators);
        article.addToExistingRow(con2);

        // Row should have [CONCEPT, CON2]
        assertEquals(1, article.rows.size());
        assertEquals(2, article.rows.get(0).mainAnnotations.size(),
                "Row should have 2 entries after first addToExistingRow");

        // Simulate second comparison round: try to add both again
        article.addToExistingRow(concept);
        article.addToExistingRow(con2);

        // Should still have exactly 2 (no duplicates)
        assertEquals(2, article.rows.get(0).mainAnnotations.size(),
                "Should not duplicate: still 2 after re-adding both annotations");

        // Simulate third comparison round
        article.addToExistingRow(concept);
        article.addToExistingRow(con2);

        assertEquals(2, article.rows.get(0).mainAnnotations.size(),
                "Should not duplicate: still 2 after third round");
    }

    @Test
    @DisplayName("addToExistingRow should not add annotation with different span")
    public void testAddToExistingRow_differentSpan() throws Exception {
        String[] annotators = {"a1", "a2"};

        Annotation concept = createAnnotation("test text", "a1", "CONCEPT", 10, 20, 1);
        Annotation other = createAnnotation("other text", "a1", "CONCEPT", 30, 40, 2);

        AnalyzedArticle article = new AnalyzedArticle("doc1.txt");
        article.initRow(concept, annotators);

        // Try to add annotation with different span
        article.addToExistingRow(other);

        // Should still have 1 annotation (different span doesn't match)
        assertEquals(1, article.rows.get(0).mainAnnotations.size(),
                "Should not add annotation with different span to existing row");
    }

    // ========== initMainAnnotator integration test ==========

    @Test
    @DisplayName("Do.run should group same-span annotations from main annotator into one row")
    public void testDoRunGroupsSameSpanAnnotations() throws Exception {
        // Set up test data with overlapping annotations at same span
        Article article = new Article("doc_test.txt");

        // a1 has two annotations at same span: CONCEPT and CON2
        Annotation a1_concept = createAnnotation("spread of dye", "a1", "CONCEPT", 100, 115, 1);
        Annotation a1_con2 = createAnnotation("spread of dye", "a1", "CON2", 100, 115, 2);

        // a2 has two annotations at same span: CONCEPT and CON2
        Annotation a2_concept = createAnnotation("spread of dye", "a2", "CONCEPT", 100, 115, 3);
        Annotation a2_con2 = createAnnotation("spread of dye", "a2", "CON2", 100, 115, 4);

        article.addAnnotation(a1_concept);
        article.addAnnotation(a1_con2);
        article.addAnnotation(a2_concept);
        article.addAnnotation(a2_con2);

        new Depot().add(article);

        // Set up IAA classes
        ArrayList<String> selectedClasses = new ArrayList<>();
        selectedClasses.add("CONCEPT");
        selectedClasses.add("CON2");
        IAA.setClasses(selectedClasses);

        ArrayList<String> selectedAnnotators = new ArrayList<>();
        selectedAnnotators.add("a1");
        selectedAnnotators.add("a2");

        Do doAnalysis = new Do(selectedAnnotators, selectedClasses);
        doAnalysis.run(selectedAnnotators);

        // Get results
        AnalyzedResult result = new AnalyzedResult();
        Vector<AnalyzedAnnotator> analyzedAnnotators = result.getAll();

        assertNotNull(analyzedAnnotators, "Should have analysis results");
        assertEquals(2, analyzedAnnotators.size(), "Should have 2 analyzed annotators");

        // Check a1's analysis
        AnalyzedAnnotator a1Annotator = null;
        AnalyzedAnnotator a2Annotator = null;
        for (AnalyzedAnnotator aa : analyzedAnnotators) {
            if ("a1".equals(aa.mainAnnotator.trim())) a1Annotator = aa;
            if ("a2".equals(aa.mainAnnotator.trim())) a2Annotator = aa;
        }

        assertNotNull(a1Annotator, "Should have analysis for a1");
        assertNotNull(a2Annotator, "Should have analysis for a2");

        // For a1 as main annotator:
        // Both CONCEPT and CON2 should be in the same row's mainAnnotations
        AnalyzedArticle a1Article = a1Annotator.getArticle("doc_test.txt");
        assertNotNull(a1Article, "a1 should have analyzed article");
        assertEquals(1, a1Article.rows.size(),
                "a1 should have 1 row (both annotations at same span grouped together)");

        AnalyzedAnnotation a1Row = a1Article.rows.get(0);
        assertEquals(2, a1Row.mainAnnotations.size(),
                "a1's row should have 2 main annotations (CONCEPT and CON2)");

        // Verify both classes are present
        boolean hasConcept = false, hasCon2 = false;
        for (Annotation ann : a1Row.mainAnnotations) {
            if ("CONCEPT".equals(ann.annotationclass)) hasConcept = true;
            if ("CON2".equals(ann.annotationclass)) hasCon2 = true;
        }
        assertTrue(hasConcept, "a1's mainAnnotations should contain CONCEPT");
        assertTrue(hasCon2, "a1's mainAnnotations should contain CON2");

        // For a2 as main annotator: same verification
        AnalyzedArticle a2Article = a2Annotator.getArticle("doc_test.txt");
        assertNotNull(a2Article, "a2 should have analyzed article");
        assertEquals(1, a2Article.rows.size(),
                "a2 should have 1 row (both annotations at same span grouped together)");

        AnalyzedAnnotation a2Row = a2Article.rows.get(0);
        assertEquals(2, a2Row.mainAnnotations.size(),
                "a2's row should have 2 main annotations (CONCEPT and CON2)");
    }

    @Test
    @DisplayName("Other annotator annotations should be matched against best class match")
    public void testBestClassMatching() throws Exception {
        Article article = new Article("doc_test.txt");

        // Adjudication has two annotations at same span: CONCEPT and CON2
        Annotation adj_concept = createAnnotation("spread of dye", "ADJ", "CONCEPT", 100, 115, 1);
        Annotation adj_con2 = createAnnotation("spread of dye", "ADJ", "CON2", 100, 115, 2);

        // a1 has two annotations at same span: CONCEPT and CON2
        Annotation a1_concept = createAnnotation("spread of dye", "a1", "CONCEPT", 100, 115, 3);
        Annotation a1_con2 = createAnnotation("spread of dye", "a1", "CON2", 100, 115, 4);

        article.addAnnotation(adj_concept);
        article.addAnnotation(adj_con2);
        article.addAnnotation(a1_concept);
        article.addAnnotation(a1_con2);

        new Depot().add(article);

        ArrayList<String> selectedClasses = new ArrayList<>();
        selectedClasses.add("CONCEPT");
        selectedClasses.add("CON2");
        IAA.setClasses(selectedClasses);

        ArrayList<String> selectedAnnotators = new ArrayList<>();
        selectedAnnotators.add("ADJ");
        selectedAnnotators.add("a1");

        Do doAnalysis = new Do(selectedAnnotators, selectedClasses);
        doAnalysis.run(selectedAnnotators);

        AnalyzedResult result = new AnalyzedResult();
        Vector<AnalyzedAnnotator> analyzedAnnotators = result.getAll();

        // Find ADJ's analysis
        AnalyzedAnnotator adjAnnotator = null;
        for (AnalyzedAnnotator aa : analyzedAnnotators) {
            if ("ADJ".equals(aa.mainAnnotator.trim())) adjAnnotator = aa;
        }
        assertNotNull(adjAnnotator, "Should have analysis for ADJ");

        AnalyzedArticle adjArticle = adjAnnotator.getArticle("doc_test.txt");
        assertNotNull(adjArticle, "ADJ should have analyzed article");

        // ADJ should have 1 row with both CONCEPT and CON2 as main annotations
        assertEquals(1, adjArticle.rows.size(),
                "ADJ should have 1 row (both annotations in same table)");

        AnalyzedAnnotation adjRow = adjArticle.rows.get(0);
        assertEquals(2, adjRow.mainAnnotations.size(),
                "ADJ's row should have 2 main annotations (CONCEPT and CON2)");

        // Check that a1's annotations appear in othersAnnotations
        assertNotNull(adjRow.othersAnnotations, "Should have others annotations");
        assertTrue(adjRow.othersAnnotations.length > 0, "Should have at least 1 other annotator");

        OthersAnnotations a1Others = null;
        for (OthersAnnotations oa : adjRow.othersAnnotations) {
            if ("a1".equals(oa.annotator.trim())) a1Others = oa;
        }
        assertNotNull(a1Others, "Should have a1's annotations in others");
        assertEquals(2, a1Others.annotationsDiffs.size(),
                "a1 should have 2 annotations compared (CONCEPT and CON2)");

        // Verify that a1's annotations have correct classes
        boolean foundConceptMatch = false, foundCon2Match = false;
        for (AnalyzedAnnotationDifference diff : a1Others.annotationsDiffs) {
            if ("CONCEPT".equals(diff.annotation.annotationclass)) foundConceptMatch = true;
            if ("CON2".equals(diff.annotation.annotationclass)) foundCon2Match = true;
        }
        assertTrue(foundConceptMatch, "a1's CONCEPT should be in the comparison");
        assertTrue(foundCon2Match, "a1's CON2 should be in the comparison");
    }

    @Test
    @DisplayName("Three annotators with overlapping labels should all show correct labels")
    public void testThreeAnnotatorsOverlapping() throws Exception {
        Article article = new Article("doc_test.txt");

        // Each annotator has CONCEPT and CON2 at the same span
        Annotation a1_concept = createAnnotation("test text", "a1", "CONCEPT", 50, 60, 1);
        Annotation a1_con2 = createAnnotation("test text", "a1", "CON2", 50, 60, 2);
        Annotation a2_concept = createAnnotation("test text", "a2", "CONCEPT", 50, 60, 3);
        Annotation a2_con2 = createAnnotation("test text", "a2", "CON2", 50, 60, 4);
        Annotation adj_concept = createAnnotation("test text", "ADJ", "CONCEPT", 50, 60, 5);
        Annotation adj_con2 = createAnnotation("test text", "ADJ", "CON2", 50, 60, 6);

        article.addAnnotation(a1_concept);
        article.addAnnotation(a1_con2);
        article.addAnnotation(a2_concept);
        article.addAnnotation(a2_con2);
        article.addAnnotation(adj_concept);
        article.addAnnotation(adj_con2);

        new Depot().add(article);

        ArrayList<String> selectedClasses = new ArrayList<>();
        selectedClasses.add("CONCEPT");
        selectedClasses.add("CON2");
        IAA.setClasses(selectedClasses);

        ArrayList<String> selectedAnnotators = new ArrayList<>();
        selectedAnnotators.add("a1");
        selectedAnnotators.add("a2");
        selectedAnnotators.add("ADJ");

        Do doAnalysis = new Do(selectedAnnotators, selectedClasses);
        doAnalysis.run(selectedAnnotators);

        AnalyzedResult result = new AnalyzedResult();
        Vector<AnalyzedAnnotator> analyzedAnnotators = result.getAll();

        // Check each annotator
        for (AnalyzedAnnotator aa : analyzedAnnotators) {
            AnalyzedArticle article1 = aa.getArticle("doc_test.txt");
            assertNotNull(article1, aa.mainAnnotator + " should have analyzed article");

            // Each annotator should have exactly 1 row with 2 main annotations
            assertEquals(1, article1.rows.size(),
                    aa.mainAnnotator + " should have 1 row (both annotations at same span)");

            AnalyzedAnnotation row = article1.rows.get(0);
            assertEquals(2, row.mainAnnotations.size(),
                    aa.mainAnnotator + "'s row should have 2 main annotations");

            // Each other annotator should have 2 annotations in the comparison
            for (OthersAnnotations oa : row.othersAnnotations) {
                assertEquals(2, oa.annotationsDiffs.size(),
                        aa.mainAnnotator + " vs " + oa.annotator +
                                " should have 2 annotations (CONCEPT and CON2)");
            }
        }
    }
}
