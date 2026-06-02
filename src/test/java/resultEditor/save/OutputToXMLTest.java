package resultEditor.save;

import adjudication.data.AdjudicationDepot;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import report.iaaReport.AdjudicationLoader;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.SpanSetDef;
import userInterface.GUI;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OutputToXMLTest {

    @TempDir
    Path tempDir;

    private GUI.ReviewMode savedReviewMode;
    private File savedCurrentProject;

    @BeforeEach
    void setUp() {
        savedReviewMode = GUI.reviewmode;
        savedCurrentProject = env.Parameters.WorkSpace.CurrentProject;

        new Depot().clear();
        AdjudicationDepot.clear();
        env.Parameters.corpus.RemoveAll();
        env.Parameters.forceChangeLatestUsedMentionID(50000);
    }

    @AfterEach
    void tearDown() {
        GUI.reviewmode = savedReviewMode;
        env.Parameters.WorkSpace.CurrentProject = savedCurrentProject;

        new Depot().clear();
        AdjudicationDepot.clear();
        env.Parameters.corpus.RemoveAll();
    }

    private Annotation createAnnotation(String text, String annotator,
            String annotationClass, int spanStart, int spanEnd,
            int uniqueIndex, Annotation.AdjudicationStatus status) {
        Annotation ann = new Annotation();
        ann.annotationText = text;
        ann.setAnnotator(annotator);
        ann.annotationclass = annotationClass;
        ann.spanset = new SpanSetDef();
        ann.spanset.setOnlySpan(spanStart, spanEnd);
        ann.uniqueIndex = uniqueIndex;
        ann.mentionid = "TEST_" + uniqueIndex;
        ann.creationDate = "Mon Jan 01 00:00:00 MST 2024";
        ann.adjudicationStatus = status;
        return ann;
    }

    private File createCorpusFile(String filename) throws Exception {
        File corpusDir = new File(tempDir.toFile(), "corpus");
        corpusDir.mkdirs();
        File txtFile = new File(corpusDir, filename);
        try (FileWriter w = new FileWriter(txtFile)) {
            w.write("Sample text content for testing.");
        }
        return txtFile;
    }

    private void setupWorkspace() {
        env.Parameters.WorkSpace.CurrentProject = tempDir.toFile();
    }

    private Document parseXml(File xmlFile) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(xmlFile);
    }

    private File getAdjudicationXml(String txtFilename) {
        return new File(tempDir.toFile(), "adjudication" + File.separator
                + txtFilename + ".knowtator.xml");
    }

    private File getSavedXml(String txtFilename) {
        return new File(tempDir.toFile(), "saved" + File.separator
                + txtFilename + ".knowtator.xml");
    }

    // ---- Original constructor tests ----

    @Test
    @DisplayName("Test OutputToXML default constructor")
    public void testDefaultConstructor() {
        OutputToXML outputToXML = new OutputToXML();
        assertNotNull(outputToXML);
    }

    @Test
    @DisplayName("Test OutputToXML constructor with parameters")
    public void testConstructorWithParameters() {
        Article article = new Article("test.txt");
        OutputToXML outputToXML = new OutputToXML("test.txt", "/tmp", article);
        assertNotNull(outputToXML);
    }

    @Test
    @DisplayName("Test Article creation")
    public void testArticleCreation() {
        Article article = new Article("test.txt");
        assertNotNull(article);
        assertEquals("test.txt", article.filename);
        article.annotations.add(new Annotation());
        assertEquals(1, article.annotations.size());
    }

    // ---- Adjudication save tests ----

    @Test
    @DisplayName("Adjudication save: file NOT in AdjudicationDepot preserves existing XML")
    void adjudicationSave_fileNotInDepot_preservesExistingXML() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("fileB.txt");

        // Pre-write known adjudication XML
        File adjDir = new File(tempDir.toFile(), "adjudication");
        adjDir.mkdirs();
        File existingXml = getAdjudicationXml("fileB.txt");
        String originalContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<annotations textSource=\"fileB.txt\">"
                + "<annotation><mention id=\"EXISTING_1\" />"
                + "<annotator id=\"a1\">a1</annotator>"
                + "<span start=\"0\" end=\"5\" />"
                + "<spannedText>hello</spannedText>"
                + "<creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>"
                + "</annotation></annotations>";
        Files.write(existingXml.toPath(), originalContent.getBytes("UTF-8"));

        // Add a DIFFERENT file to AdjudicationDepot (not fileB.txt)
        Article articleA = new Article("fileA.txt");
        articleA.annotations.add(createAnnotation("word", "a1", "CONCEPT",
                10, 14, 1, Annotation.AdjudicationStatus.MATCHES_OK));
        new AdjudicationDepot().add(articleA);

        // Also add fileB.txt to regular Depot so saved/ folder write works
        Depot depot = new Depot();
        depot.articleInsurance("fileB.txt");

        // Act
        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        // Assert: adjudication XML for fileB should be UNCHANGED
        String afterContent = new String(Files.readAllBytes(existingXml.toPath()), "UTF-8");
        assertEquals(originalContent, afterContent,
                "Adjudication XML for file NOT in AdjudicationDepot should be preserved");
    }

    @Test
    @DisplayName("Adjudication save: file IN AdjudicationDepot writes correctly")
    void adjudicationSave_fileInDepot_writesCorrectly() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("fileA.txt");

        // Add to both depots
        Depot depot = new Depot();
        depot.articleInsurance("fileA.txt");

        Article adjArticle = new Article("fileA.txt");
        adjArticle.annotations.add(createAnnotation("matched", "a1", "CONCEPT",
                10, 17, 1, Annotation.AdjudicationStatus.MATCHES_OK));
        adjArticle.annotations.add(createAnnotation("nonmatch", "a2", "CON2",
                30, 38, 2, Annotation.AdjudicationStatus.NON_MATCHES));
        new AdjudicationDepot().add(adjArticle);

        // Act
        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        // Assert
        File adjXml = getAdjudicationXml("fileA.txt");
        assertTrue(adjXml.exists(), "Adjudication XML should be created");

        Document doc = parseXml(adjXml);
        Element root = doc.getRootElement();

        List<Element> annotations = root.getChildren("annotation");
        List<Element> adjudicating = root.getChildren("adjudicating");

        assertEquals(1, annotations.size(),
                "Should have 1 <annotation> (MATCHES_OK)");
        assertEquals(1, adjudicating.size(),
                "Should have 1 <adjudicating> (NON_MATCHES)");

        // MATCHES_OK annotation should have annotator "ADJUDICATION"
        Element annElem = annotations.get(0);
        assertEquals("ADJUDICATION", annElem.getChildText("annotator"));
    }

    @Test
    @DisplayName("Adjudication save: MATCHES_OK written as <annotation>, not <adjudicating>")
    void adjudicationSave_matchesOK_writtenAsAnnotation() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("test.txt");

        Depot depot = new Depot();
        depot.articleInsurance("test.txt");

        Article adjArticle = new Article("test.txt");
        adjArticle.annotations.add(createAnnotation("word", "a1", "CONCEPT",
                0, 4, 1, Annotation.AdjudicationStatus.MATCHES_OK));
        new AdjudicationDepot().add(adjArticle);

        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        Document doc = parseXml(getAdjudicationXml("test.txt"));
        Element root = doc.getRootElement();

        assertEquals(1, root.getChildren("annotation").size(),
                "MATCHES_OK should produce exactly 1 <annotation>");
        assertEquals(0, root.getChildren("adjudicating").size(),
                "MATCHES_OK should NOT produce any <adjudicating>");
    }

    @Test
    @DisplayName("Adjudication save: NON_MATCHES and UNPROCESSED written as <adjudicating>")
    void adjudicationSave_nonMatches_writtenAsAdjudicating() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("test.txt");

        Depot depot = new Depot();
        depot.articleInsurance("test.txt");

        Article adjArticle = new Article("test.txt");
        adjArticle.annotations.add(createAnnotation("wordA", "a1", "CONCEPT",
                0, 5, 1, Annotation.AdjudicationStatus.NON_MATCHES));
        adjArticle.annotations.add(createAnnotation("wordB", "a2", "CONCEPT",
                10, 15, 2, Annotation.AdjudicationStatus.UNPROCESSED));
        new AdjudicationDepot().add(adjArticle);

        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        Document doc = parseXml(getAdjudicationXml("test.txt"));
        Element root = doc.getRootElement();

        assertEquals(0, root.getChildren("annotation").size(),
                "NON_MATCHES/UNPROCESSED should NOT produce <annotation>");

        List<Element> adjElements = root.getChildren("adjudicating");
        assertEquals(2, adjElements.size(),
                "Should have 2 <adjudicating> elements");

        // Verify AdjudicationStatus is preserved
        for (Element adj : adjElements) {
            String status = adj.getChildText("AdjudicationStatus");
            assertNotNull(status, "<adjudicating> should contain <AdjudicationStatus>");
            assertTrue(status.equals("NON_MATCHES") || status.equals("UNPROCESSED"),
                    "Status should be NON_MATCHES or UNPROCESSED, got: " + status);
        }
    }

    @Test
    @DisplayName("Adjudication save: annotator=ADJUDICATION passes filter regardless of status")
    void adjudicationSave_adjudicatorAnnotation_writtenAsAnnotation() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("test.txt");

        Depot depot = new Depot();
        depot.articleInsurance("test.txt");

        Article adjArticle = new Article("test.txt");
        // An annotation created by adjudicator with NON_MATCHES status
        adjArticle.annotations.add(createAnnotation("newword", "ADJUDICATION", "CONCEPT",
                20, 27, 1, Annotation.AdjudicationStatus.NON_MATCHES));
        new AdjudicationDepot().add(adjArticle);

        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        Document doc = parseXml(getAdjudicationXml("test.txt"));
        Element root = doc.getRootElement();

        List<Element> annotations = root.getChildren("annotation");
        assertTrue(annotations.size() >= 1,
                "ADJUDICATION annotator should pass filter into <annotation>");
        assertEquals("ADJUDICATION", annotations.get(0).getChildText("annotator"));
    }

    @Test
    @DisplayName("Annotation mode: no adjudication XML written")
    void annotationMode_noAdjudicationXMLWritten() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;

        File txtFile = createCorpusFile("test.txt");

        Depot depot = new Depot();
        depot.articleInsurance("test.txt");
        Article depotArticle = depot.getArticleByFilename("test.txt");
        depotArticle.annotations.add(createAnnotation("word", "annotator1", "CONCEPT",
                0, 4, 1, Annotation.AdjudicationStatus.EXCLUDED));

        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        // saved/ XML should exist
        assertTrue(getSavedXml("test.txt").exists(),
                "saved/ XML should be created");

        // adjudication/ XML should NOT exist
        assertFalse(getAdjudicationXml("test.txt").exists(),
                "adjudication/ XML should NOT be created in annotation mode");
    }

    @Test
    @DisplayName("Adjudication save: MATCHES_DLETED written as <adjudicating> with preserved status")
    void adjudicationSave_matchesDeleted_writtenAsAdjudicating() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("test.txt");

        Depot depot = new Depot();
        depot.articleInsurance("test.txt");

        Article adjArticle = new Article("test.txt");
        adjArticle.annotations.add(createAnnotation("deleted", "a1", "CONCEPT",
                0, 7, 1, Annotation.AdjudicationStatus.MATCHES_DLETED));
        adjArticle.annotations.add(createAnnotation("kept", "a2", "CONCEPT",
                10, 14, 2, Annotation.AdjudicationStatus.MATCHES_OK));
        new AdjudicationDepot().add(adjArticle);

        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        Document doc = parseXml(getAdjudicationXml("test.txt"));
        Element root = doc.getRootElement();

        List<Element> annotations = root.getChildren("annotation");
        List<Element> adjudicating = root.getChildren("adjudicating");

        assertEquals(1, annotations.size(),
                "MATCHES_OK should produce 1 <annotation>");
        assertEquals(1, adjudicating.size(),
                "MATCHES_DLETED should produce 1 <adjudicating>");
        assertEquals("MATCHES_DLETED", adjudicating.get(0).getChildText("AdjudicationStatus"),
                "AdjudicationStatus should be MATCHES_DLETED");
    }

    @Test
    @DisplayName("Adjudication save: bad annotation doesn't abort remaining annotations")
    void adjudicationSave_badAnnotation_doesNotAbortRest() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("test.txt");

        Depot depot = new Depot();
        depot.articleInsurance("test.txt");

        Article adjArticle = new Article("test.txt");

        // Good annotation first
        adjArticle.annotations.add(createAnnotation("good1", "a1", "CONCEPT",
                0, 5, 1, Annotation.AdjudicationStatus.NON_MATCHES));

        // Bad annotation: null classname will be skipped by buildAnnotationNode
        // but should not abort the loop
        Annotation badAnn = createAnnotation("bad", "a2", null,
                10, 13, 2, Annotation.AdjudicationStatus.NON_MATCHES);
        adjArticle.annotations.add(badAnn);

        // Good annotation after the bad one — should still be saved
        adjArticle.annotations.add(createAnnotation("good2", "a3", "CON2",
                20, 25, 3, Annotation.AdjudicationStatus.NON_MATCHES));

        new AdjudicationDepot().add(adjArticle);

        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        Document doc = parseXml(getAdjudicationXml("test.txt"));
        Element root = doc.getRootElement();

        List<Element> adjudicating = root.getChildren("adjudicating");
        assertTrue(adjudicating.size() >= 2,
                "Both good annotations should be saved even though a bad one is in between. Got: "
                + adjudicating.size());
    }

    @Test
    @DisplayName("quickXMLSaving: multiple files — only updates file in AdjudicationDepot")
    void quickXMLSaving_multipleFiles_preservesUntouchedAdjudications() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        // Create 3 corpus files and register them
        File txt1 = createCorpusFile("doc1.txt");
        File txt2 = createCorpusFile("doc2.txt");
        File txt3 = createCorpusFile("doc3.txt");
        env.Parameters.corpus.addTextFile(txt1);
        env.Parameters.corpus.addTextFile(txt2);
        env.Parameters.corpus.addTextFile(txt3);

        // Ensure all files have depot entries
        Depot depot = new Depot();
        depot.articleInsurance("doc1.txt");
        depot.articleInsurance("doc2.txt");
        depot.articleInsurance("doc3.txt");

        // Pre-write adjudication XMLs for doc2 and doc3
        File adjDir = new File(tempDir.toFile(), "adjudication");
        adjDir.mkdirs();

        String doc2Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<annotations textSource=\"doc2.txt\">"
                + "<annotation><mention id=\"KEEP_1\" />"
                + "<annotator id=\"a1\">a1</annotator>"
                + "<span start=\"0\" end=\"5\" />"
                + "<spannedText>kept</spannedText>"
                + "<creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>"
                + "</annotation></annotations>";
        File doc2Xml = getAdjudicationXml("doc2.txt");
        Files.write(doc2Xml.toPath(), doc2Content.getBytes("UTF-8"));

        String doc3Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<annotations textSource=\"doc3.txt\">"
                + "<annotation><mention id=\"KEEP_2\" />"
                + "<annotator id=\"a2\">a2</annotator>"
                + "<span start=\"10\" end=\"15\" />"
                + "<spannedText>also kept</spannedText>"
                + "<creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>"
                + "</annotation></annotations>";
        File doc3Xml = getAdjudicationXml("doc3.txt");
        Files.write(doc3Xml.toPath(), doc3Content.getBytes("UTF-8"));

        // Only add doc1 to AdjudicationDepot
        Article adjArticle = new Article("doc1.txt");
        adjArticle.annotations.add(createAnnotation("word", "a1", "CONCEPT",
                0, 4, 1, Annotation.AdjudicationStatus.MATCHES_OK));
        new AdjudicationDepot().add(adjArticle);

        // Act: save all files
        Save save = new Save();
        save.quickXMLSaving();

        // Assert: doc1 adjudication XML was written (updated)
        File doc1Xml = getAdjudicationXml("doc1.txt");
        assertTrue(doc1Xml.exists(), "doc1 adjudication XML should be created");
        Document doc1Doc = parseXml(doc1Xml);
        assertTrue(doc1Doc.getRootElement().getChildren("annotation").size() > 0,
                "doc1 should have MATCHES_OK annotation written");

        // Assert: doc2 and doc3 adjudication XMLs are UNCHANGED
        String doc2After = new String(Files.readAllBytes(doc2Xml.toPath()), "UTF-8");
        assertEquals(doc2Content, doc2After,
                "doc2 adjudication XML should be preserved (not in AdjudicationDepot)");

        String doc3After = new String(Files.readAllBytes(doc3Xml.toPath()), "UTF-8");
        assertEquals(doc3Content, doc3After,
                "doc3 adjudication XML should be preserved (not in AdjudicationDepot)");
    }

    @Test
    @DisplayName("User scenario: restart eHOST, work on other file, save-all preserves untouched adjudications")
    void userScenario_restartWorkOnOtherFile_saveAll_preservesUntouched() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        // ========== SESSION 1: Initial adjudication work on 3 files ==========

        File noteA = createCorpusFile("noteA.txt");
        File noteB = createCorpusFile("noteB.txt");
        File noteC = createCorpusFile("noteC.txt");

        Depot depot = new Depot();
        depot.articleInsurance("noteA.txt");
        depot.articleInsurance("noteB.txt");
        depot.articleInsurance("noteC.txt");

        // noteA: 3 MATCHES_OK, 1 NON_MATCHES
        Article adjA = new Article("noteA.txt");
        adjA.annotations.add(createAnnotation("alpha", "a1", "CONCEPT",
                0, 5, 1, Annotation.AdjudicationStatus.MATCHES_OK));
        adjA.annotations.add(createAnnotation("bravo", "a1", "CONCEPT",
                6, 11, 2, Annotation.AdjudicationStatus.MATCHES_OK));
        adjA.annotations.add(createAnnotation("charlie", "a1", "TYPE_B",
                12, 19, 3, Annotation.AdjudicationStatus.MATCHES_OK));
        adjA.annotations.add(createAnnotation("delta", "a2", "TYPE_B",
                20, 25, 4, Annotation.AdjudicationStatus.NON_MATCHES));
        new AdjudicationDepot().add(adjA);

        // noteB: 2 MATCHES_OK, 2 NON_MATCHES, 1 UNPROCESSED
        Article adjB = new Article("noteB.txt");
        adjB.annotations.add(createAnnotation("echo", "a1", "CONCEPT",
                0, 4, 5, Annotation.AdjudicationStatus.MATCHES_OK));
        adjB.annotations.add(createAnnotation("foxtrot", "a2", "CONCEPT",
                5, 12, 6, Annotation.AdjudicationStatus.MATCHES_OK));
        adjB.annotations.add(createAnnotation("golf", "a1", "TYPE_B",
                13, 17, 7, Annotation.AdjudicationStatus.NON_MATCHES));
        adjB.annotations.add(createAnnotation("hotel", "a2", "TYPE_B",
                18, 23, 8, Annotation.AdjudicationStatus.NON_MATCHES));
        adjB.annotations.add(createAnnotation("india", "a3", "CON3",
                24, 29, 9, Annotation.AdjudicationStatus.UNPROCESSED));
        new AdjudicationDepot().add(adjB);

        // noteC: 1 MATCHES_OK, 1 MATCHES_DLETED
        Article adjC = new Article("noteC.txt");
        adjC.annotations.add(createAnnotation("juliet", "a1", "CONCEPT",
                0, 6, 10, Annotation.AdjudicationStatus.MATCHES_OK));
        adjC.annotations.add(createAnnotation("kilo", "a2", "CONCEPT",
                7, 11, 11, Annotation.AdjudicationStatus.MATCHES_DLETED));
        new AdjudicationDepot().add(adjC);

        // Save all 3 files (end of session 1)
        env.Parameters.corpus.addTextFile(noteA);
        env.Parameters.corpus.addTextFile(noteB);
        env.Parameters.corpus.addTextFile(noteC);
        new Save().quickXMLSaving();

        // Verify session 1 save
        File xmlA = getAdjudicationXml("noteA.txt");
        File xmlB = getAdjudicationXml("noteB.txt");
        File xmlC = getAdjudicationXml("noteC.txt");
        assertTrue(xmlA.exists() && xmlB.exists() && xmlC.exists(),
                "All 3 adjudication XMLs should exist after session 1");

        Document docA1 = parseXml(xmlA);
        assertEquals(3, docA1.getRootElement().getChildren("annotation").size(),
                "noteA session 1: 3 <annotation> (MATCHES_OK)");
        assertEquals(1, docA1.getRootElement().getChildren("adjudicating").size(),
                "noteA session 1: 1 <adjudicating> (NON_MATCHES)");

        Document docB1 = parseXml(xmlB);
        assertEquals(2, docB1.getRootElement().getChildren("annotation").size(),
                "noteB session 1: 2 <annotation> (MATCHES_OK)");
        assertEquals(3, docB1.getRootElement().getChildren("adjudicating").size(),
                "noteB session 1: 3 <adjudicating> (2 NON_MATCHES + 1 UNPROCESSED)");

        Document docC1 = parseXml(xmlC);
        assertEquals(1, docC1.getRootElement().getChildren("annotation").size(),
                "noteC session 1: 1 <annotation> (MATCHES_OK)");
        assertEquals(1, docC1.getRootElement().getChildren("adjudicating").size(),
                "noteC session 1: 1 <adjudicating> (MATCHES_DLETED)");

        // ========== RESTART: clear all in-memory state ==========

        AdjudicationDepot.clear();
        new Depot().clear();
        env.Parameters.corpus.RemoveAll();

        // Re-register corpus files and depot entries (simulates project load)
        env.Parameters.corpus.addTextFile(noteA);
        env.Parameters.corpus.addTextFile(noteB);
        env.Parameters.corpus.addTextFile(noteC);
        depot = new Depot();
        depot.articleInsurance("noteA.txt");
        depot.articleInsurance("noteB.txt");
        depot.articleInsurance("noteC.txt");

        // ========== SESSION 2: Resume adjudication (loadWorkingState) ==========

        AdjudicationLoader.loadWorkingState();

        // User works ONLY on noteA — adds a new adjudication annotation
        Article reloadedA = AdjudicationDepot.getArticleByFilename("noteA.txt");
        assertNotNull(reloadedA, "noteA should be in AdjudicationDepot after reload");
        reloadedA.annotations.add(createAnnotation("lima", "ADJUDICATION", "NEW_CLASS",
                26, 30, 12, Annotation.AdjudicationStatus.MATCHES_OK));

        // User does NOT touch noteB or noteC — just switches among files via REST API

        // ========== SAVE ALL (user clicks save button) ==========

        new Save().quickXMLSaving();

        // ========== VERIFY: noteA updated, noteB and noteC fully preserved ==========

        // --- noteA: should have the new annotation added ---
        Document docA2 = parseXml(xmlA);
        List<Element> noteA_annotations = docA2.getRootElement().getChildren("annotation");
        List<Element> noteA_adjudicating = docA2.getRootElement().getChildren("adjudicating");
        assertEquals(4, noteA_annotations.size(),
                "noteA session 2: should have 4 <annotation> (3 original MATCHES_OK + 1 new)");
        assertEquals(1, noteA_adjudicating.size(),
                "noteA session 2: should still have 1 <adjudicating> (NON_MATCHES)");

        // --- noteB: ALL annotations must survive (2 MATCHES_OK, 2 NON_MATCHES, 1 UNPROCESSED) ---
        Document docB2 = parseXml(xmlB);
        List<Element> noteB_annotations = docB2.getRootElement().getChildren("annotation");
        List<Element> noteB_adjudicating = docB2.getRootElement().getChildren("adjudicating");
        assertEquals(2, noteB_annotations.size(),
                "noteB session 2: MATCHES_OK annotations must survive restart+save. "
                + "Got " + noteB_annotations.size() + " <annotation>, expected 2");
        assertEquals(3, noteB_adjudicating.size(),
                "noteB session 2: NON_MATCHES/UNPROCESSED must survive restart+save. "
                + "Got " + noteB_adjudicating.size() + " <adjudicating>, expected 3");

        // --- noteC: ALL annotations must survive (1 MATCHES_OK, 1 MATCHES_DLETED) ---
        Document docC2 = parseXml(xmlC);
        List<Element> noteC_annotations = docC2.getRootElement().getChildren("annotation");
        List<Element> noteC_adjudicating = docC2.getRootElement().getChildren("adjudicating");
        assertEquals(1, noteC_annotations.size(),
                "noteC session 2: MATCHES_OK must survive restart+save. "
                + "Got " + noteC_annotations.size() + " <annotation>, expected 1");
        assertEquals(1, noteC_adjudicating.size(),
                "noteC session 2: MATCHES_DLETED must survive restart+save. "
                + "Got " + noteC_adjudicating.size() + " <adjudicating>, expected 1");
        assertEquals("MATCHES_DLETED",
                noteC_adjudicating.get(0).getChildText("AdjudicationStatus"),
                "noteC: MATCHES_DLETED status must be preserved after restart");
    }

    @Test
    @DisplayName("Round-trip: save → loadWorkingState → save preserves MATCHES_OK annotations")
    void roundTrip_saveAndReload_preservesMatchesOK() throws Exception {
        setupWorkspace();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        File txtFile = createCorpusFile("roundtrip.txt");

        // Initial adjudication state with both MATCHES_OK and NON_MATCHES
        Depot depot = new Depot();
        depot.articleInsurance("roundtrip.txt");

        Article adjArticle = new Article("roundtrip.txt");
        adjArticle.annotations.add(createAnnotation("matched1", "a1", "CONCEPT",
                0, 8, 1, Annotation.AdjudicationStatus.MATCHES_OK));
        adjArticle.annotations.add(createAnnotation("matched2", "a2", "TYPE_B",
                10, 18, 2, Annotation.AdjudicationStatus.MATCHES_OK));
        adjArticle.annotations.add(createAnnotation("nonmatch", "a3", "CON2",
                20, 28, 3, Annotation.AdjudicationStatus.NON_MATCHES));
        new AdjudicationDepot().add(adjArticle);

        // --- First save ---
        OutputToXML toxml = new OutputToXML();
        toxml.directsave(txtFile);

        File adjXml = getAdjudicationXml("roundtrip.txt");
        assertTrue(adjXml.exists(), "Adjudication XML should be created");

        Document doc1 = parseXml(adjXml);
        assertEquals(2, doc1.getRootElement().getChildren("annotation").size(),
                "First save: 2 MATCHES_OK → 2 <annotation>");
        assertEquals(1, doc1.getRootElement().getChildren("adjudicating").size(),
                "First save: 1 NON_MATCHES → 1 <adjudicating>");

        // --- Simulate restart: clear in-memory state ---
        AdjudicationDepot.clear();
        new Depot().clear();
        depot = new Depot();
        depot.articleInsurance("roundtrip.txt");

        // --- Reload from saved XML (simulates mode_continuePreviousAdjudicationWork) ---
        AdjudicationLoader.loadWorkingState();

        // Verify MATCHES_OK annotations were loaded into AdjudicationDepot
        Article reloadedAdj = AdjudicationDepot.getArticleByFilename("roundtrip.txt");
        assertNotNull(reloadedAdj, "AdjudicationDepot should have roundtrip.txt after reload");

        long matchesOKCount = reloadedAdj.annotations.stream()
                .filter(a -> a.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK)
                .count();
        long nonMatchesCount = reloadedAdj.annotations.stream()
                .filter(a -> a.adjudicationStatus == Annotation.AdjudicationStatus.NON_MATCHES)
                .count();
        assertEquals(2, matchesOKCount,
                "After reload: AdjudicationDepot should have 2 MATCHES_OK annotations");
        assertEquals(1, nonMatchesCount,
                "After reload: AdjudicationDepot should have 1 NON_MATCHES annotation");

        // --- Second save (should reproduce the same XML) ---
        OutputToXML toxml2 = new OutputToXML();
        toxml2.directsave(txtFile);

        Document doc2 = parseXml(adjXml);
        assertEquals(2, doc2.getRootElement().getChildren("annotation").size(),
                "Second save after reload: should still have 2 <annotation> (MATCHES_OK)");
        assertEquals(1, doc2.getRootElement().getChildren("adjudicating").size(),
                "Second save after reload: should still have 1 <adjudicating> (NON_MATCHES)");
    }
}
