package report.iaaReport.genHtml;

import imports.ImportXML;
import imports.importedXML.eXMLFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import report.iaaReport.IAA;
import report.iaaReport.analysis.detailsNonMatches.AnalyzedAnnotation;
import report.iaaReport.analysis.detailsNonMatches.AnalyzedAnnotator;
import report.iaaReport.analysis.detailsNonMatches.AnalyzedArticle;
import report.iaaReport.analysis.detailsNonMatches.AnalyzedResult;
import report.iaaReport.analysis.detailsNonMatches.Do;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that loads real annotation XML data from data/proj2,
 * runs the IAA analysis pipeline, generates HTML reports, and validates
 * that overlapping annotations are correctly displayed in single tables.
 */
public class HtmlReportIntegrationTest {

    private boolean origCheckClass;
    private boolean origCheckAttributes;
    private boolean origCheckRelationship;
    private boolean origCheckOverlappedSpans;
    private boolean origCheckComment;
    private File origCurrentProject;

    private File proj2Dir;
    private File reportDir;

    @BeforeEach
    public void setUp() throws Exception {
        // Save original IAA flags
        origCheckClass = IAA.CHECK_CLASS;
        origCheckAttributes = IAA.CHECK_ATTRIBUTES;
        origCheckRelationship = IAA.CHECK_RELATIONSHIP;
        origCheckOverlappedSpans = IAA.CHECK_OVERLAPPED_SPANS;
        origCheckComment = IAA.CHECK_COMMENT;
        origCurrentProject = env.Parameters.WorkSpace.CurrentProject;

        // Set IAA flags
        IAA.CHECK_CLASS = true;
        IAA.CHECK_ATTRIBUTES = false;
        IAA.CHECK_RELATIONSHIP = false;
        IAA.CHECK_OVERLAPPED_SPANS = false;
        IAA.CHECK_COMMENT = false;

        // Clear static state
        new Depot().clear();
        AnalyzedResult.clear();

        // Find the project root and proj2 data directory
        proj2Dir = findProj2Dir();
        assertNotNull(proj2Dir, "Could not find data/proj2 directory");
        assertTrue(proj2Dir.exists(), "data/proj2 directory does not exist: " + proj2Dir);

        // Set workspace so PreLoadDocumentContents can find corpus files
        env.Parameters.WorkSpace.CurrentProject = proj2Dir;

        // Create a temp report directory
        reportDir = new File("target/test-reports/iaa-integration-" + System.currentTimeMillis());
        reportDir.mkdirs();
    }

    @AfterEach
    public void tearDown() {
        // Restore IAA flags
        IAA.CHECK_CLASS = origCheckClass;
        IAA.CHECK_ATTRIBUTES = origCheckAttributes;
        IAA.CHECK_RELATIONSHIP = origCheckRelationship;
        IAA.CHECK_OVERLAPPED_SPANS = origCheckOverlappedSpans;
        IAA.CHECK_COMMENT = origCheckComment;
        env.Parameters.WorkSpace.CurrentProject = origCurrentProject;

        // Clear static state
        new Depot().clear();
        AnalyzedResult.clear();

        // Clean up report directory
        if (reportDir != null && reportDir.exists()) {
            deleteDir(reportDir);
        }
    }

    /**
     * Loads all annotation XML files from proj2/saved and proj2/adjudication into the Depot.
     */
    private void loadAnnotationsFromProj2() {
        ImportAnnotation importer = new ImportAnnotation();

        // Load regular annotator annotations from saved/
        File savedDir = new File(proj2Dir, "saved");
        assertTrue(savedDir.exists(), "saved directory not found: " + savedDir);

        File[] xmlFiles = savedDir.listFiles((dir, name) -> name.endsWith(".knowtator.xml"));
        assertNotNull(xmlFiles, "No XML files found in saved directory");
        assertTrue(xmlFiles.length > 0, "No XML files found in saved directory");

        for (File xmlFile : xmlFiles) {
            eXMLFile exml = ImportXML.readXMLContents(xmlFile);
            assertNotNull(exml, "Failed to parse XML file: " + xmlFile.getName());
            exml = importer.assignateAnnotationIndex(exml);
            importer.XMLExtractor(exml);
        }

        // Load adjudication annotations from adjudication/
        File adjDir = new File(proj2Dir, "adjudication");
        if (adjDir.exists()) {
            File[] adjFiles = adjDir.listFiles((dir, name) -> name.endsWith(".knowtator.xml"));
            if (adjFiles != null) {
                for (File xmlFile : adjFiles) {
                    eXMLFile exml = ImportXML.readXMLContents(xmlFile);
                    if (exml != null) {
                        exml = importer.assignateAnnotationIndex(exml);
                        importer.XMLExtractor(exml);
                    }
                }
            }
        }
    }

    /**
     * Runs the Do analysis for the given annotators and classes.
     */
    private void runAnalysis(ArrayList<String> annotators, ArrayList<String> classes) throws Exception {
        IAA.setClasses(classes);
        Do doAnalysis = new Do(annotators, classes);
        doAnalysis.run(annotators);
    }

    // ==================== ANALYSIS-LEVEL TESTS ====================

    @Test
    @DisplayName("doc3: ADJUDICATION CONCEPT+CON2 at overlapping span should be grouped in one row")
    public void testDoc3AdjOverlappingGroupedInOneRow() throws Exception {
        loadAnnotationsFromProj2();

        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("ADJUDICATION");
        annotators.add("a1");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        AnalyzedAnnotator adjAnnotator = findAnnotator("ADJUDICATION");
        assertNotNull(adjAnnotator, "ADJUDICATION annotator not found in results");

        AnalyzedArticle doc3 = findArticle(adjAnnotator, "doc3.txt");
        assertNotNull(doc3, "doc3.txt not found in ADJUDICATION results");

        // ADJUDICATION has CONCEPT@(746,754) and CON2@(746,760) — overlapping, should be ONE row
        assertEquals(1, doc3.rows.size(),
                "ADJUDICATION's overlapping CONCEPT+CON2 should be in 1 row, got " +
                        doc3.rows.size());

        AnalyzedAnnotation row = doc3.rows.get(0);
        assertEquals(2, row.mainAnnotations.size(),
                "Row should have 2 mainAnnotations (CONCEPT and CON2)");

        Set<String> mainClasses = new HashSet<>();
        for (Annotation ann : row.mainAnnotations) {
            mainClasses.add(ann.annotationclass.trim());
        }
        assertTrue(mainClasses.contains("CONCEPT"), "CONCEPT missing from mainAnnotations");
        assertTrue(mainClasses.contains("CON2"), "CON2 missing from mainAnnotations");
    }

    @Test
    @DisplayName("doc3: a1 CONCEPT+CON2 at same span should be grouped in one row")
    public void testDoc3SjlOverlappingGroupedInOneRow() throws Exception {
        loadAnnotationsFromProj2();

        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("a1");
        annotators.add("ADJUDICATION");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        AnalyzedAnnotator a1Annotator = findAnnotator("a1");
        assertNotNull(a1Annotator, "a1 annotator not found in results");

        AnalyzedArticle doc3 = findArticle(a1Annotator, "doc3.txt");
        assertNotNull(doc3, "doc3.txt not found in a1 results");

        assertEquals(1, doc3.rows.size(),
                "a1's overlapping CONCEPT+CON2 should be in 1 row, got " + doc3.rows.size());

        AnalyzedAnnotation row = doc3.rows.get(0);
        assertEquals(2, row.mainAnnotations.size(),
                "Row should have 2 mainAnnotations (CONCEPT and CON2)");
    }

    @Test
    @DisplayName("doc1: overlapping CONCEPT+CON2 at same span should be grouped in one row")
    public void testDoc1OverlappingAnnotationsGrouped() throws Exception {
        loadAnnotationsFromProj2();

        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("ADJUDICATION");
        annotators.add("a2");
        annotators.add("a1");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        AnalyzedAnnotator adjAnnotator = findAnnotator("ADJUDICATION");
        assertNotNull(adjAnnotator, "ADJUDICATION annotator not found");

        AnalyzedArticle doc1 = findArticle(adjAnnotator, "doc1.txt");
        assertNotNull(doc1, "doc1.txt not found");

        // Find rows where both CONCEPT and CON2 exist (overlapping annotations grouped together)
        int rowsWithBothClasses = 0;
        for (AnalyzedAnnotation row : doc1.rows) {
            if (row.mainAnnotations.size() >= 2) {
                boolean hasConcept = false;
                boolean hasCon2 = false;
                for (Annotation ann : row.mainAnnotations) {
                    if ("CONCEPT".equals(ann.annotationclass)) hasConcept = true;
                    if ("CON2".equals(ann.annotationclass)) hasCon2 = true;
                }
                if (hasConcept && hasCon2) {
                    rowsWithBothClasses++;
                }
            }
        }

        assertEquals(1, rowsWithBothClasses,
                "Expected 1 row grouping CONCEPT+CON2 in doc1, found " + rowsWithBothClasses);
    }

    // ==================== HTML GENERATION TESTS ====================

    @Test
    @DisplayName("HTML: ADJUDICATION report should show CONCEPT and CON2 in one table for doc3")
    public void testHtmlAdjDoc3OverlappingInSameTable() throws Exception {
        loadAnnotationsFromProj2();

        // doc3 has: a1(CONCEPT+CON2 at 746,754) and ADJUDICATION(CONCEPT@746,754+CON2@746,760)
        // When we compare ADJUDICATION vs a2, ADJUDICATION's doc3 annotations are unmatched
        // because a2 has matching annotations — but overlapping test is key.
        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("ADJUDICATION");
        annotators.add("a1");
        annotators.add("a2");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        GenHtmlForNonMatches htmlGen = new GenHtmlForNonMatches();
        htmlGen.genHtml(reportDir);

        File adjReport = new File(reportDir, "ADJUDICATION-UNMATCHED-SUMMARY.html");

        if (!adjReport.exists()) {
            // If ADJUDICATION has no unmatched annotations, that's valid — verify at analysis level
            AnalyzedAnnotator adjAnnotator = findAnnotator("ADJUDICATION");
            assertNotNull(adjAnnotator, "ADJUDICATION annotator not found");
            AnalyzedArticle doc3 = findArticle(adjAnnotator, "doc3.txt");
            if (doc3 != null) {
                // If doc3 exists, verify grouping at analysis level
                assertEquals(1, doc3.rows.size(),
                        "ADJUDICATION doc3 should have 1 row for overlapping CONCEPT+CON2");
                assertEquals(2, doc3.rows.get(0).mainAnnotations.size(),
                        "Row should have 2 mainAnnotations (CONCEPT+CON2)");
            }
            return;
        }

        String html = new String(Files.readAllBytes(adjReport.toPath()));
        String[] doc3Sections = extractDocSections(html, "doc3.txt");

        if (doc3Sections.length > 0) {
            // If doc3 appears in the report, it should be in ONE table (not split)
            assertEquals(1, doc3Sections.length,
                    "Overlapping CONCEPT+CON2 should be in 1 table, found " + doc3Sections.length);

            String table = doc3Sections[0];
            assertTrue(table.contains("CONCEPT"), "Table should contain CONCEPT");
            assertTrue(table.contains("CON2"), "Table should contain CON2");
        }
    }

    @Test
    @DisplayName("HTML: a1 report should show CONCEPT and CON2 in one table for doc3")
    public void testHtmlSjlDoc3OverlappingInSameTable() throws Exception {
        loadAnnotationsFromProj2();

        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("a1");
        annotators.add("ADJUDICATION");
        annotators.add("a2");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        GenHtmlForNonMatches htmlGen = new GenHtmlForNonMatches();
        htmlGen.genHtml(reportDir);

        File a1Report = new File(reportDir, "a1-UNMATCHED-SUMMARY.html");
        if (!a1Report.exists()) {
            // No unmatched — verify at analysis level
            AnalyzedAnnotator a1Annotator = findAnnotator("a1");
            assertNotNull(a1Annotator, "a1 annotator not found");
            AnalyzedArticle doc3 = findArticle(a1Annotator, "doc3.txt");
            if (doc3 != null) {
                assertEquals(1, doc3.rows.size(), "a1 doc3 should have 1 row");
                assertEquals(2, doc3.rows.get(0).mainAnnotations.size(), "Should have 2 mainAnnotations");
            }
            return;
        }

        String html = new String(Files.readAllBytes(a1Report.toPath()));
        String[] doc3Sections = extractDocSections(html, "doc3.txt");

        if (doc3Sections.length > 0) {
            assertEquals(1, doc3Sections.length,
                    "Expected 1 table for doc3 in a1 report, found " + doc3Sections.length);
            assertTrue(doc3Sections[0].contains("CONCEPT"), "Table should contain CONCEPT");
            assertTrue(doc3Sections[0].contains("CON2"), "Table should contain CON2");
        }
    }

    @Test
    @DisplayName("HTML: Class labels should be correctly paired (CONCEPT-CONCEPT, CON2-CON2)")
    public void testHtmlClassPairingCorrectness() throws Exception {
        loadAnnotationsFromProj2();

        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("ADJUDICATION");
        annotators.add("a1");
        annotators.add("a2");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        GenHtmlForNonMatches htmlGen = new GenHtmlForNonMatches();
        htmlGen.genHtml(reportDir);

        File adjReport = new File(reportDir, "ADJUDICATION-UNMATCHED-SUMMARY.html");
        if (!adjReport.exists()) return; // No unmatched annotations to validate

        String html = new String(Files.readAllBytes(adjReport.toPath()));

        // Verify class pairing in ALL tables (not just doc3)
        Pattern classRowPattern = Pattern.compile(
                "<tr>\\s*<td>Class</td>(.*?)</tr>", Pattern.DOTALL);
        Matcher matcher = classRowPattern.matcher(html);

        while (matcher.find()) {
            String rowContent = matcher.group(1);

            // Extract non-empty cell values
            Pattern cellPattern = Pattern.compile("<td[^>]*>\\s*(\\w+)\\s*</td>");
            Matcher cellMatcher = cellPattern.matcher(rowContent);

            ArrayList<String> cellValues = new ArrayList<>();
            while (cellMatcher.find()) {
                String val = cellMatcher.group(1).trim();
                if (!val.isEmpty()) {
                    cellValues.add(val);
                }
            }

            // If main and other annotator both have class values, they should match
            if (cellValues.size() >= 2) {
                String mainClass = cellValues.get(0);
                for (int i = 1; i < cellValues.size(); i++) {
                    assertEquals(mainClass, cellValues.get(i),
                            "Class pairing mismatch: main='" + mainClass +
                                    "' paired with '" + cellValues.get(i) + "'");
                }
            }
        }
    }

    @Test
    @DisplayName("HTML: GenHtmlForNonMatches2 should also group overlapping in one table")
    public void testGenHtmlForNonMatches2OverlappingInSameTable() throws Exception {
        loadAnnotationsFromProj2();

        ArrayList<String> annotators = new ArrayList<>();
        annotators.add("ADJUDICATION");
        annotators.add("a1");
        annotators.add("a2");

        ArrayList<String> classes = new ArrayList<>();
        classes.add("CONCEPT");
        classes.add("CON2");

        runAnalysis(annotators, classes);

        File reportDir2 = new File("target/test-reports/iaa-v2-" + System.currentTimeMillis());
        reportDir2.mkdirs();

        try {
            GenHtmlForNonMatches2 htmlGen2 = new GenHtmlForNonMatches2();
            htmlGen2.genHtml(reportDir2);

            File adjReport = new File(reportDir2, "ADJUDICATION-UNMATCHED-SUMMARY.html");
            if (!adjReport.exists()) {
                // Verify at analysis level
                AnalyzedAnnotator adjAnnotator = findAnnotator("ADJUDICATION");
                assertNotNull(adjAnnotator, "ADJUDICATION annotator not found");
                AnalyzedArticle doc3 = findArticle(adjAnnotator, "doc3.txt");
                if (doc3 != null) {
                    assertEquals(1, doc3.rows.size(), "ADJUDICATION doc3 should have 1 row");
                }
                return;
            }

            String html = new String(Files.readAllBytes(adjReport.toPath()));
            String[] doc3Sections = extractDocSections(html, "doc3.txt");

            if (doc3Sections.length > 0) {
                assertEquals(1, doc3Sections.length,
                        "GenHtmlForNonMatches2: Expected 1 table for doc3, found " + doc3Sections.length);
                assertTrue(doc3Sections[0].contains("CONCEPT"), "Table should contain CONCEPT");
                assertTrue(doc3Sections[0].contains("CON2"), "Table should contain CON2");
            }
        } finally {
            deleteDir(reportDir2);
        }
    }

    // ==================== HELPERS ====================

    private AnalyzedAnnotator findAnnotator(String name) {
        AnalyzedResult result = new AnalyzedResult();
        Vector<AnalyzedAnnotator> all = result.getAll();
        for (AnalyzedAnnotator aa : all) {
            if (name.equals(aa.mainAnnotator.trim())) {
                return aa;
            }
        }
        return null;
    }

    /**
     * Find an article by filename within an annotator's analyzed articles.
     * Uses the public analyzedArticles field since getArticle() is package-private.
     */
    private AnalyzedArticle findArticle(AnalyzedAnnotator annotator, String filename) {
        for (AnalyzedArticle article : annotator.analyzedArticles) {
            if (article != null && filename.equals(article.filename.trim())) {
                return article;
            }
        }
        return null;
    }

    /**
     * Extracts table sections for a given document filename from the HTML report.
     * Each section is the snippet div + table block.
     */
    private String[] extractDocSections(String html, String docFilename) {
        ArrayList<String> sections = new ArrayList<>();
        String escaped = docFilename.replace(".", "\\.");
        Pattern sectionPattern = Pattern.compile(
                "(<div><a[^>]*>File: " + escaped + "</a></div>.*?</table>)", Pattern.DOTALL);
        Matcher matcher = sectionPattern.matcher(html);
        while (matcher.find()) {
            sections.add(matcher.group(1));
        }
        return sections.toArray(new String[0]);
    }

    private File findProj2Dir() {
        // Prefer test resources (stable copy) over data/proj2 (may be edited)
        File testRes = new File("src/test/resources/proj2");
        if (testRes.exists()) return testRes;

        File proj2 = new File("data/proj2");
        if (proj2.exists()) return proj2;

        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            File pom = new File(dir, "pom.xml");
            if (pom.exists()) {
                File candidate = new File(dir, "src/test/resources/proj2");
                if (candidate.exists()) return candidate;
                candidate = new File(dir, "data/proj2");
                if (candidate.exists()) return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}
