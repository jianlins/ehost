package report.iaaReport;

import imports.ImportXML;
import imports.importedXML.eXMLFile;
import org.junit.jupiter.api.*;
import report.iaaReport.analysis.Analysis;
import report.iaaReport.analysis.DiffAnalysisResult;
import report.iaaReport.analysis.detailsNonMatches.AnalyzedResult;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify the correctness of IAA (Inter-Annotator Agreement)
 * calculations against the proj2 test data.
 *
 * <p>Each annotator (a1, a2) is compared pairwise against ADJUDICATION
 * under both strict-match and overlapped-match modes. The expected
 * TP / precision / recall / F-score values were computed by hand from
 * the knowtator XML files in src/test/resources/proj2/.
 *
 * <h3>Annotation inventory (CHECK_ATTRIBUTES=false, CHECK_CLASS=true)</h3>
 * <pre>
 * ── a1 (6 annotations) ──────────────────────────────
 *   doc1: CONCEPT@(578,587)  CONCEPT@(332,386)  CON2@(345,354)
 *   doc2: CONCEPT@(628,635)
 *   doc3: CONCEPT@(746,754)  CON2@(746,754)
 *
 * ── a2 (4 annotations) ──────────────────────────────
 *   doc1: CON2@(352,362)  CONCEPT@(338,380)
 *   doc3: CONCEPT@(746,754)  CON2@(746,754)
 *
 * ── ADJUDICATION (7 annotations) ────────────────────
 *   doc1: CONCEPT@(578,587)  CON2@(352,362)  CONCEPT@(338,380)
 *   doc2: CONCEPT@(628,635)  CONCEPT@(611,618)
 *   doc3: CONCEPT@(746,754)  CON2@(746,754)
 * </pre>
 */
public class IAACalculationTest {

    // ─── saved / restored IAA flags ───
    private boolean origCheckClass;
    private boolean origCheckAttributes;
    private boolean origCheckRelationship;
    private boolean origCheckOverlappedSpans;
    private boolean origCheckComment;
    private File origCurrentProject;

    private File proj2Dir;

    /** Floating-point tolerance for precision / recall / F comparisons. */
    private static final float DELTA = 0.001f;

    // ─────────────────────────── setup / teardown ───

    @BeforeEach
    public void setUp() {
        origCheckClass = IAA.CHECK_CLASS;
        origCheckAttributes = IAA.CHECK_ATTRIBUTES;
        origCheckRelationship = IAA.CHECK_RELATIONSHIP;
        origCheckOverlappedSpans = IAA.CHECK_OVERLAPPED_SPANS;
        origCheckComment = IAA.CHECK_COMMENT;
        origCurrentProject = env.Parameters.WorkSpace.CurrentProject;

        // Clear all static state
        new Depot().clear();
        PairWiseDepot.removeAll();
        DiffAnalysisResult.removeAll();
        ClassAgreementDepot.clear();
        AnalyzedResult.clear();

        proj2Dir = findProj2Dir();
        assertNotNull(proj2Dir, "Could not find src/test/resources/proj2");
        env.Parameters.WorkSpace.CurrentProject = proj2Dir;
    }

    @AfterEach
    public void tearDown() {
        IAA.CHECK_CLASS = origCheckClass;
        IAA.CHECK_ATTRIBUTES = origCheckAttributes;
        IAA.CHECK_RELATIONSHIP = origCheckRelationship;
        IAA.CHECK_OVERLAPPED_SPANS = origCheckOverlappedSpans;
        IAA.CHECK_COMMENT = origCheckComment;
        env.Parameters.WorkSpace.CurrentProject = origCurrentProject;

        new Depot().clear();
        PairWiseDepot.removeAll();
        DiffAnalysisResult.removeAll();
        ClassAgreementDepot.clear();
        AnalyzedResult.clear();
    }

    // ═══════════════════════════════════════════════════
    //  Strict match  (CHECK_OVERLAPPED_SPANS = false)
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Strict match: a1(gold) vs Adjudication — TP=4, P=4/7, R=4/6")
    public void testStrictMatch_a1Gold_AdjCompare() throws Exception {
        loadAnnotations();
        setFlags(false);  // strict

        ArrayList<String> annotators = buildList("a1", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("a1", "ADJUDICATION");
        assertNotNull(rec, "PairWise record a1→ADJUDICATION not found");

        assertAll("a1(gold) vs ADJUDICATION strict",
                () -> assertEquals(6, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(7, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(2, rec.false_negatives, "FN"),
                () -> assertEquals(3, rec.false_positives, "FP"),
                () -> assertEquals(4.0f / 7, rec.precision, DELTA, "precision"),
                () -> assertEquals(4.0f / 6, rec.recall, DELTA, "recall"),
                () -> assertEquals(2 * (4.0f / 7) * (4.0f / 6) / ((4.0f / 7) + (4.0f / 6)),
                        rec.f_score, DELTA, "F-score")
        );
    }

    @Test
    @DisplayName("Strict match: Adjudication(gold) vs a1 — TP=4, P=4/6, R=4/7")
    public void testStrictMatch_AdjGold_a1Compare() throws Exception {
        loadAnnotations();
        setFlags(false);

        ArrayList<String> annotators = buildList("a1", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("ADJUDICATION", "a1");
        assertNotNull(rec, "PairWise record ADJUDICATION→a1 not found");

        assertAll("ADJUDICATION(gold) vs a1 strict",
                () -> assertEquals(7, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(6, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(3, rec.false_negatives, "FN"),
                () -> assertEquals(2, rec.false_positives, "FP"),
                () -> assertEquals(4.0f / 6, rec.precision, DELTA, "precision"),
                () -> assertEquals(4.0f / 7, rec.recall, DELTA, "recall")
        );
    }

    @Test
    @DisplayName("Strict match: a2(gold) vs Adjudication — TP=4, P=4/7, R=1.0")
    public void testStrictMatch_a2Gold_AdjCompare() throws Exception {
        loadAnnotations();
        setFlags(false);

        ArrayList<String> annotators = buildList("a2", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("a2", "ADJUDICATION");
        assertNotNull(rec, "PairWise record a2→ADJUDICATION not found");

        assertAll("a2(gold) vs ADJUDICATION strict",
                () -> assertEquals(4, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(7, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(0, rec.false_negatives, "FN"),
                () -> assertEquals(3, rec.false_positives, "FP"),
                () -> assertEquals(4.0f / 7, rec.precision, DELTA, "precision"),
                () -> assertEquals(1.0f, rec.recall, DELTA, "recall")
        );
    }

    @Test
    @DisplayName("Strict match: Adjudication(gold) vs a2 — TP=4, P=1.0, R=4/7")
    public void testStrictMatch_AdjGold_a2Compare() throws Exception {
        loadAnnotations();
        setFlags(false);

        ArrayList<String> annotators = buildList("a2", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("ADJUDICATION", "a2");
        assertNotNull(rec, "PairWise record ADJUDICATION→a2 not found");

        assertAll("ADJUDICATION(gold) vs a2 strict",
                () -> assertEquals(7, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(4, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(3, rec.false_negatives, "FN"),
                () -> assertEquals(0, rec.false_positives, "FP"),
                () -> assertEquals(1.0f, rec.precision, DELTA, "precision"),
                () -> assertEquals(4.0f / 7, rec.recall, DELTA, "recall")
        );
    }

    // ═══════════════════════════════════════════════════
    //  Overlapped match  (CHECK_OVERLAPPED_SPANS = true)
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Overlap match: a1(gold) vs Adjudication — TP=6, P=6/7, R=1.0")
    public void testOverlapMatch_a1Gold_AdjCompare() throws Exception {
        loadAnnotations();
        setFlags(true);  // overlap

        ArrayList<String> annotators = buildList("a1", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("a1", "ADJUDICATION");
        assertNotNull(rec, "PairWise record a1→ADJUDICATION not found");

        assertAll("a1(gold) vs ADJUDICATION overlap",
                () -> assertEquals(6, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(7, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(6, rec.true_positive, "TP"),
                () -> assertEquals(0, rec.false_negatives, "FN"),
                () -> assertEquals(1, rec.false_positives, "FP"),
                () -> assertEquals(6.0f / 7, rec.precision, DELTA, "precision"),
                () -> assertEquals(1.0f, rec.recall, DELTA, "recall"),
                () -> assertEquals(12.0f / 13, rec.f_score, DELTA, "F-score")
        );
    }

    @Test
    @DisplayName("Overlap match: Adjudication(gold) vs a1 — TP=6, P=1.0, R=6/7")
    public void testOverlapMatch_AdjGold_a1Compare() throws Exception {
        loadAnnotations();
        setFlags(true);

        ArrayList<String> annotators = buildList("a1", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("ADJUDICATION", "a1");
        assertNotNull(rec, "PairWise record ADJUDICATION→a1 not found");

        assertAll("ADJUDICATION(gold) vs a1 overlap",
                () -> assertEquals(7, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(6, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(6, rec.true_positive, "TP"),
                () -> assertEquals(1, rec.false_negatives, "FN"),
                () -> assertEquals(0, rec.false_positives, "FP"),
                () -> assertEquals(1.0f, rec.precision, DELTA, "precision"),
                () -> assertEquals(6.0f / 7, rec.recall, DELTA, "recall"),
                () -> assertEquals(12.0f / 13, rec.f_score, DELTA, "F-score")
        );
    }

    @Test
    @DisplayName("Overlap match: a2(gold) vs Adjudication — same as strict (TP=4)")
    public void testOverlapMatch_a2Gold_AdjCompare() throws Exception {
        loadAnnotations();
        setFlags(true);

        ArrayList<String> annotators = buildList("a2", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("a2", "ADJUDICATION");
        assertNotNull(rec, "PairWise record a2→ADJUDICATION not found");

        // a2's annotations already have exact span matches with ADJUDICATION,
        // so overlap mode produces the same result as strict.
        assertAll("a2(gold) vs ADJUDICATION overlap",
                () -> assertEquals(4, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(7, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(4.0f / 7, rec.precision, DELTA, "precision"),
                () -> assertEquals(1.0f, rec.recall, DELTA, "recall")
        );
    }

    @Test
    @DisplayName("Overlap match: Adjudication(gold) vs a2 — same as strict (TP=4)")
    public void testOverlapMatch_AdjGold_a2Compare() throws Exception {
        loadAnnotations();
        setFlags(true);

        ArrayList<String> annotators = buildList("a2", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("ADJUDICATION", "a2");
        assertNotNull(rec, "PairWise record ADJUDICATION→a2 not found");

        assertAll("ADJUDICATION(gold) vs a2 overlap",
                () -> assertEquals(7, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(4, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(1.0f, rec.precision, DELTA, "precision"),
                () -> assertEquals(4.0f / 7, rec.recall, DELTA, "recall")
        );
    }

    // ═══════════════════════════════════════════════════
    //  With CHECK_ATTRIBUTES = true
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Strict + attrs: a1(gold) vs Adjudication — TP=3 (CON2@doc3 attrs differ)")
    public void testStrictWithAttrs_a1Gold_AdjCompare() throws Exception {
        loadAnnotations();
        setFlags(false);
        IAA.CHECK_ATTRIBUTES = true;

        ArrayList<String> annotators = buildList("a1", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("a1", "ADJUDICATION");
        assertNotNull(rec, "PairWise record a1→ADJUDICATION not found");

        // doc3 CON2@(746,754): a1 has NO attrs, ADJUDICATION has att1=1,att2=c → mismatch
        assertAll("a1(gold) vs ADJUDICATION strict+attrs",
                () -> assertEquals(6, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(7, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(3, rec.true_positive, "TP"),
                () -> assertEquals(3, rec.false_negatives, "FN"),
                () -> assertEquals(4, rec.false_positives, "FP")
        );
    }

    @Test
    @DisplayName("Overlap + attrs: a1(gold) vs Adjudication — TP=4 (CON2 spans overlap but attrs differ)")
    public void testOverlapWithAttrs_a1Gold_AdjCompare() throws Exception {
        loadAnnotations();
        setFlags(true);
        IAA.CHECK_ATTRIBUTES = true;

        ArrayList<String> annotators = buildList("a1", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        PairWiseAgreementRecord rec = findRecord("a1", "ADJUDICATION");
        assertNotNull(rec, "PairWise record a1→ADJUDICATION not found");

        // Overlap matches the two CONCEPT spans in doc1 (TP ×2 more than strict),
        // but CON2@(345,354) vs CON2@(352,362) overlap matches on span+class,
        // yet attrs differ (att1=2,att2=b vs att1=1,att2=a) → still no match.
        // And doc3 CON2 attrs still differ. So TP=4.
        assertAll("a1(gold) vs ADJUDICATION overlap+attrs",
                () -> assertEquals(6, rec.subTotal_GoldStandard, "gold total"),
                () -> assertEquals(7, rec.subTotal_Compare, "compare total"),
                () -> assertEquals(4, rec.true_positive, "TP"),
                () -> assertEquals(2, rec.false_negatives, "FN"),
                () -> assertEquals(3, rec.false_positives, "FP")
        );
    }

    // ═══════════════════════════════════════════════════
    //  Three-way comparison (a1, a2, ADJUDICATION)
    // ═══════════════════════════════════════════════════

    @Test
    @DisplayName("Three-way strict: all 4 directional records computed correctly")
    public void testThreeWay_StrictMatch() throws Exception {
        loadAnnotations();
        setFlags(false);

        ArrayList<String> annotators = buildList("a1", "a2", "ADJUDICATION");
        ArrayList<String> classes = buildList("CONCEPT", "CON2");

        runPipeline(annotators, classes);

        // Verify all 6 pairwise records exist
        // (3 annotators → 3×2 = 6 directed pairs)
        assertEquals(6, PairWiseDepot.depot_SameAll.size(),
                "Should have 6 PairWise records for 3 annotators");

        // Spot-check: a1→ADJUDICATION
        PairWiseAgreementRecord a1Adj = findRecord("a1", "ADJUDICATION");
        assertNotNull(a1Adj);
        assertEquals(4, a1Adj.true_positive, "a1→ADJ TP");

        // Spot-check: a2→ADJUDICATION
        PairWiseAgreementRecord a2Adj = findRecord("a2", "ADJUDICATION");
        assertNotNull(a2Adj);
        assertEquals(4, a2Adj.true_positive, "a2→ADJ TP");
    }

    // ═══════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════

    /**
     * Loads all annotation XMLs from proj2/saved and proj2/adjudication
     * into the global Depot, exactly the way the integration test does.
     */
    private void loadAnnotations() {
        ImportAnnotation importer = new ImportAnnotation();

        // saved/
        File savedDir = new File(proj2Dir, "saved");
        assertTrue(savedDir.exists(), "saved/ directory not found");
        for (File xmlFile : listXmls(savedDir)) {
            eXMLFile exml = ImportXML.readXMLContents(xmlFile);
            assertNotNull(exml, "Failed to parse " + xmlFile.getName());
            exml = importer.assignateAnnotationIndex(exml);
            importer.XMLExtractor(exml);
        }

        // adjudication/
        File adjDir = new File(proj2Dir, "adjudication");
        if (adjDir.exists()) {
            for (File xmlFile : listXmls(adjDir)) {
                eXMLFile exml = ImportXML.readXMLContents(xmlFile);
                if (exml != null) {
                    exml = importer.assignateAnnotationIndex(exml);
                    importer.XMLExtractor(exml);
                }
            }
        }
    }

    /**
     * Sets IAA flags for the test scenario.
     *
     * @param overlapped true → CHECK_OVERLAPPED_SPANS=true
     */
    private void setFlags(boolean overlapped) {
        IAA.CHECK_CLASS = true;
        IAA.CHECK_ATTRIBUTES = false;
        IAA.CHECK_RELATIONSHIP = false;
        IAA.CHECK_OVERLAPPED_SPANS = overlapped;
        IAA.CHECK_COMMENT = false;
    }

    /** Runs the full Analysis pipeline (multi-way + pairwise). */
    private void runPipeline(ArrayList<String> annotators, ArrayList<String> classes) throws Exception {
        IAA.setClasses(classes);
        Analysis analysis = new Analysis(annotators, classes);
        analysis.startAnalysis();
    }

    /** Finds a specific PairWiseAgreementRecord by gold/compare annotator names. */
    private PairWiseAgreementRecord findRecord(String gold, String compare) {
        for (PairWiseAgreementRecord r : PairWiseDepot.depot_SameAll) {
            if (r != null
                    && r.gold_standard_set.trim().equals(gold.trim())
                    && r.compared_set.trim().equals(compare.trim())) {
                return r;
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> ArrayList<T> buildList(T... items) {
        ArrayList<T> list = new ArrayList<>();
        for (T item : items) list.add(item);
        return list;
    }

    private File[] listXmls(File dir) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".knowtator.xml"));
        return files != null ? files : new File[0];
    }

    private File findProj2Dir() {
        File testRes = new File("src/test/resources/proj2");
        if (testRes.exists()) return testRes;
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            File pom = new File(dir, "pom.xml");
            if (pom.exists()) {
                File candidate = new File(dir, "src/test/resources/proj2");
                if (candidate.exists()) return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }
}
