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
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for duplicate annotations appearing after
 * "reopen -&gt; continue previous adjudication -&gt; save".
 *
 * <p>See {@code docs/plans/adjudication-duplicate-annotations-analysis.md}.
 *
 * <p>The core defect: the two writers that produce
 * {@code adjudication/*.knowtator.xml} have overlapping filters.
 * <ul>
 *   <li>{@code addAnnotations(root, true)} emits {@code <annotation>} when
 *       {@code status == MATCHES_OK} <b>OR</b> {@code annotator == "ADJUDICATION"}</li>
 *   <li>{@code addAdjudicatingAnnotations(root)} emits {@code <adjudicating>} when
 *       {@code status != MATCHES_OK}</li>
 * </ul>
 * Their intersection — an annotation authored during adjudication that is not
 * yet an agreed match — is written twice into the same file, and therefore read
 * back as two distinct AdjudicationDepot entries.
 *
 * <p>Note the pre-existing tests in {@link OutputToXMLTest} only ever use
 * annotator names like {@code "a1"}/{@code "a2"}, which is why this overlap was
 * never exercised.
 */
public class AdjudicationRoundTripTest {

    /** The annotator name eHOST assigns to annotations produced by adjudication. */
    private static final String ADJUDICATION_ANNOTATOR = "ADJUDICATION";

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

        env.Parameters.WorkSpace.CurrentProject = tempDir.toFile();
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;
    }

    @AfterEach
    void tearDown() {
        GUI.reviewmode = savedReviewMode;
        env.Parameters.WorkSpace.CurrentProject = savedCurrentProject;

        new Depot().clear();
        AdjudicationDepot.clear();
        env.Parameters.corpus.RemoveAll();
    }

    // ------------------------------------------------------------------ //
    // helpers
    // ------------------------------------------------------------------ //

    private Annotation annotation(String text, String annotator, String annClass,
            int spanStart, int spanEnd, int uniqueIndex,
            Annotation.AdjudicationStatus status) {
        Annotation ann = new Annotation();
        ann.annotationText = text;
        ann.setAnnotator(annotator);
        ann.annotationclass = annClass;
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
            w.write("Sample text content used for adjudication round-trip tests.");
        }
        return txtFile;
    }

    private File adjudicationXml(String txtFilename) {
        return new File(tempDir.toFile(), "adjudication" + File.separator
                + txtFilename + ".knowtator.xml");
    }

    private Document parseXml(File xmlFile) throws Exception {
        return new SAXBuilder().build(xmlFile);
    }

    /** Seed AdjudicationDepot with one article and save it to disk. */
    private void seedAndSave(String txtFilename, Annotation... annotations) throws Exception {
        File txtFile = createCorpusFile(txtFilename);

        new Depot().articleInsurance(txtFilename);

        Article adjArticle = new Article(txtFilename);
        for (Annotation ann : annotations) {
            adjArticle.annotations.add(ann);
        }
        new AdjudicationDepot().add(adjArticle);

        new OutputToXML().directsave(txtFile);
    }

    /**
     * Simulate closing and reopening eHOST, then choosing
     * "continue previous adjudication work".
     *
     * <p>This mirrors the real startup sequence:
     * <ol>
     *   <li>all in-memory state is dropped (the process exits)</li>
     *   <li>{@code Reload} repopulates the regular Depot from {@code saved/}
     *       — replicated here via {@code ImportAnnotation.XMLImporter}, which is
     *       exactly what {@code Reload.extractAnnotation_fromXML} calls
     *       (we cannot call {@code Reload.load} directly: it requires a GUI)</li>
     *   <li>{@code AdjudicationLoader.loadWorkingState()} repopulates
     *       AdjudicationDepot from {@code adjudication/}</li>
     * </ol>
     */
    private void restartAndResumeAdjudication() {
        // (1) process exit — every static depot is gone
        new Depot().clear();
        AdjudicationDepot.clear();

        assertFalse(AdjudicationDepot.isReady(),
                "precondition: AdjudicationDepot must be empty after a simulated restart");

        // (2) project open — Reload pulls saved/ back into the regular Depot
        File savedDir = new File(tempDir.toFile(), "saved");
        Vector<File> savedXmls = new Vector<>();
        File[] files = savedDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".knowtator.xml")) {
                    savedXmls.add(f);
                }
            }
        }
        if (!savedXmls.isEmpty()) {
            new resultEditor.annotations.ImportAnnotation().XMLImporter(savedXmls);
        }

        // (3) user clicks "continue previous adjudication work"
        AdjudicationLoader.loadWorkingState();
    }

    /** Save the current depot state, as the user pressing Save would. */
    private void save(String txtFilename) {
        File txtFile = new File(new File(tempDir.toFile(), "corpus"), txtFilename);
        new Depot().articleInsurance(txtFilename);
        new OutputToXML().directsave(txtFile);
    }

    // ---- modelling real adjudication actions -------------------------- //
    // The GUI mutates AdjudicationDepot entries in place; see
    // adjudication/Adjudication.java:1395 (accept), :1463 (reject),
    // :1421 (delete). We reproduce those mutations directly.

    /** Adjudicator accepts an annotation as an agreed match. */
    private void adjudicateAccept(String txtFilename, String annotationText) {
        setStatus(txtFilename, annotationText,
                Annotation.AdjudicationStatus.MATCHES_OK);
    }

    /** Adjudicator rejects an annotation. */
    private void adjudicateReject(String txtFilename, String annotationText) {
        setStatus(txtFilename, annotationText,
                Annotation.AdjudicationStatus.NON_MATCHES);
    }

    private void setStatus(String txtFilename, String annotationText,
            Annotation.AdjudicationStatus status) {
        Article article = AdjudicationDepot.getArticleByFilename(txtFilename);
        assertNotNull(article, "no adjudication article for " + txtFilename);
        boolean found = false;
        for (Annotation ann : article.annotations) {
            if (annotationText.equals(ann.annotationText)) {
                ann.adjudicationStatus = status;
                found = true;
            }
        }
        assertTrue(found, "no annotation matching '" + annotationText + "' to adjudicate");
    }

    /**
     * Adjudicator creates a brand new annotation during adjudication.
     * Mirrors {@code AdjudicationDepot.addANewAnnotation}: annotator is
     * "ADJUDICATION" and {@code setUnProcessed()} leaves adjudicationStatus at
     * its default — it does <b>not</b> set MATCHES_OK.
     */
    private void adjudicateCreateNew(String txtFilename, String annotationText,
            String annClass, int start, int end, int uniqueIndex) {
        Article article = AdjudicationDepot.getArticleByFilename(txtFilename);
        assertNotNull(article, "no adjudication article for " + txtFilename);
        Annotation ann = annotation(annotationText, ADJUDICATION_ANNOTATOR, annClass,
                start, end, uniqueIndex, Annotation.AdjudicationStatus.EXCLUDED);
        ann.setUnProcessed();
        article.annotations.add(ann);
    }

    private int adjudicationDepotCount(String txtFilename) {
        Article article = AdjudicationDepot.getArticleByFilename(txtFilename);
        return (article == null || article.annotations == null)
                ? 0
                : article.annotations.size();
    }

    // ------------------------------------------------------------------ //
    // 1. the double-write itself
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("An ADJUDICATION-authored, unresolved annotation is serialized exactly once")
    void adjudicationAuthored_unresolved_isSerializedOnce() throws Exception {
        // status != MATCHES_OK AND annotator == ADJUDICATION -> hits BOTH writers
        seedAndSave("doc1.txt",
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 10, 1, Annotation.AdjudicationStatus.NON_MATCHES));

        Element root = parseXml(adjudicationXml("doc1.txt")).getRootElement();
        List<Element> asAnnotation = root.getChildren("annotation");
        List<Element> asAdjudicating = root.getChildren("adjudicating");

        assertEquals(1, asAnnotation.size() + asAdjudicating.size(),
                "One AdjudicationDepot annotation must produce exactly one XML element, but it was"
                        + " written as " + asAnnotation.size() + " <annotation> and "
                        + asAdjudicating.size() + " <adjudicating>. The filters in"
                        + " addAnnotations(root,true) and addAdjudicatingAnnotations(root) overlap.");
    }

    @Test
    @DisplayName("Control: a normal annotator's unresolved annotation is written once")
    void normalAnnotator_unresolved_isSerializedOnce() throws Exception {
        seedAndSave("doc2.txt",
                annotation("chest pain", "annotatorA", "SYMPTOM",
                        0, 10, 1, Annotation.AdjudicationStatus.NON_MATCHES));

        Element root = parseXml(adjudicationXml("doc2.txt")).getRootElement();

        assertEquals(0, root.getChildren("annotation").size(),
                "unresolved annotation from a normal annotator should not be a final <annotation>");
        assertEquals(1, root.getChildren("adjudicating").size(),
                "unresolved annotation from a normal annotator should be one <adjudicating>");
    }

    @Test
    @DisplayName("Control: an agreed match is written once, as <annotation>")
    void agreedMatch_isSerializedOnceAsAnnotation() throws Exception {
        seedAndSave("doc3.txt",
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 10, 1, Annotation.AdjudicationStatus.MATCHES_OK));

        Element root = parseXml(adjudicationXml("doc3.txt")).getRootElement();

        assertEquals(1, root.getChildren("annotation").size(),
                "MATCHES_OK should produce exactly one <annotation>");
        assertEquals(0, root.getChildren("adjudicating").size(),
                "MATCHES_OK should not also produce an <adjudicating>");
    }

    // ------------------------------------------------------------------ //
    // 2. the user-visible symptom: duplicates after reopen
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Reopening after save does not duplicate adjudication annotations")
    void reopenAfterSave_doesNotDuplicate() throws Exception {
        seedAndSave("doc4.txt",
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 10, 1, Annotation.AdjudicationStatus.NON_MATCHES));

        assertEquals(1, adjudicationDepotCount("doc4.txt"),
                "precondition: exactly one annotation was adjudicated");

        restartAndResumeAdjudication();

        assertEquals(1, adjudicationDepotCount("doc4.txt"),
                "Resuming adjudication must restore the same number of annotations that were saved."
                        + " More than one means the save/load round trip duplicated it.");
    }

    @Test
    @DisplayName("A mixed, realistic adjudication session survives reopen without duplication")
    void reopenAfterSave_mixedSession_doesNotDuplicate() throws Exception {
        seedAndSave("doc5.txt",
                // an agreed match
                annotation("fever", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 5, 1, Annotation.AdjudicationStatus.MATCHES_OK),
                // authored during adjudication, still unresolved -> the overlap case
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        10, 20, 2, Annotation.AdjudicationStatus.NON_MATCHES),
                // a disagreement between two original annotators
                annotation("cough", "annotatorA", "SYMPTOM",
                        25, 30, 3, Annotation.AdjudicationStatus.NON_MATCHES),
                annotation("nausea", "annotatorB", "SYMPTOM",
                        35, 41, 4, Annotation.AdjudicationStatus.UNPROCESSED));

        assertEquals(4, adjudicationDepotCount("doc5.txt"), "precondition");

        restartAndResumeAdjudication();

        assertEquals(4, adjudicationDepotCount("doc5.txt"),
                "All four annotations should come back exactly once each.");
    }

    @Test
    @DisplayName("Repeated reopen/save cycles remain stable")
    void repeatedCycles_remainStable() throws Exception {
        seedAndSave("doc6.txt",
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 10, 1, Annotation.AdjudicationStatus.NON_MATCHES));

        for (int cycle = 1; cycle <= 3; cycle++) {
            restartAndResumeAdjudication();

            assertEquals(1, adjudicationDepotCount("doc6.txt"),
                    "annotation count drifted at cycle " + cycle);

            // save again, as the user would after continuing their work
            save("doc6.txt");
        }
    }

    // ------------------------------------------------------------------ //
    // 2b. the user's full reported scenario, end to end
    // ------------------------------------------------------------------ //

    /**
     * The exact sequence reported by users:
     *
     * <pre>
     *   open → adjudicate → save → close
     *        → reopen → adjudicate again → save
     * </pre>
     *
     * The second adjudication round is the part the other tests miss. It
     * matters because after a reopen the depot has been rebuilt from XML, so
     * any duplication introduced by the first round is now the <em>input</em>
     * to the second — meaning the error compounds with every session rather
     * than staying constant.
     */
    @Test
    @DisplayName("Full lifecycle: open→adjudicate→save→close→reopen→adjudicate→save")
    void fullLifecycle_twoAdjudicationSessions_noDuplicates() throws Exception {
        final String doc = "case.txt";

        // ---------- session 1 ----------
        // open: two annotators disagree about two spans
        seedAndSave(doc,
                annotation("fever", "annotatorA", "SYMPTOM",
                        0, 5, 1, Annotation.AdjudicationStatus.UNPROCESSED),
                annotation("cough", "annotatorB", "SYMPTOM",
                        10, 15, 2, Annotation.AdjudicationStatus.UNPROCESSED));

        // adjudicate: accept one, reject the other, and author a new one
        adjudicateAccept(doc, "fever");
        adjudicateReject(doc, "cough");
        adjudicateCreateNew(doc, "chest pain", "SYMPTOM", 20, 30, 3);

        // save + close
        save(doc);
        int afterSession1 = adjudicationDepotCount(doc);
        assertEquals(3, afterSession1, "precondition: 3 annotations after session 1");

        // ---------- session 2 ----------
        restartAndResumeAdjudication();

        assertEquals(3, adjudicationDepotCount(doc),
                "Reopening must restore exactly the 3 annotations that were saved,"
                        + " not duplicates of them.");

        // adjudicate again: resolve the annotation authored last session
        adjudicateAccept(doc, "chest pain");

        save(doc);

        assertEquals(3, adjudicationDepotCount(doc),
                "A second adjudication round must not add phantom annotations.");

        // ---------- session 3: confirm it converged ----------
        restartAndResumeAdjudication();

        assertEquals(3, adjudicationDepotCount(doc),
                "Annotation count must be stable across sessions. Growth here means"
                        + " each save/reopen cycle compounds the duplication.");

        // and the XML itself must hold exactly one element per annotation
        Element root = parseXml(adjudicationXml(doc)).getRootElement();
        int elements = root.getChildren("annotation").size()
                + root.getChildren("adjudicating").size();
        assertEquals(3, elements,
                "adjudication XML should contain exactly 3 elements for 3 annotations,"
                        + " but held " + elements + ".");
    }

    // ------------------------------------------------------------------ //
    // 3. status must survive the round trip
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Adjudication status is preserved across save and reload")
    void adjudicationStatus_survivesRoundTrip() throws Exception {
        seedAndSave("doc7.txt",
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 10, 1, Annotation.AdjudicationStatus.NON_MATCHES));

        restartAndResumeAdjudication();

        Article article = AdjudicationDepot.getArticleByFilename("doc7.txt");
        assertNotNull(article, "article should be restored into AdjudicationDepot");
        assertEquals(1, article.annotations.size(), "expected exactly one annotation");

        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                article.annotations.get(0).adjudicationStatus,
                "NON_MATCHES must not be laundered into MATCHES_OK by the round trip."
                        + " <annotation> elements currently omit <AdjudicationStatus>, so"
                        + " AdjudicationLoader.loadWorkingState() has to guess.");
    }

    // ------------------------------------------------------------------ //
    // 3b. contracts the fix must not break
    // ------------------------------------------------------------------ //

    /**
     * Adjudication folders written by older builds contain only
     * {@code <annotation>} elements with no {@code <AdjudicationStatus>} child:
     * back then every element in that folder was, by definition, an agreed
     * final result. Resuming against such a folder must still work, and every
     * annotation must come back exactly once as {@code MATCHES_OK}.
     */
    @Test
    @DisplayName("Legacy adjudication XML without <AdjudicationStatus> still loads as MATCHES_OK")
    void legacyFile_withoutStatus_stillLoads() throws Exception {
        final String doc = "legacy.txt";
        createCorpusFile(doc);
        writeLegacyAdjudicationXml(doc);

        restartAndResumeAdjudication();

        Article article = AdjudicationDepot.getArticleByFilename(doc);
        assertNotNull(article, "legacy adjudication XML must still be loaded");
        assertEquals(1, article.annotations.size(),
                "a legacy file holding one <annotation> must restore exactly one annotation,"
                        + " not a duplicate pair");
        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                article.annotations.get(0).adjudicationStatus,
                "an <annotation> with no <AdjudicationStatus> is a pre-existing agreed match,"
                        + " so it must default to MATCHES_OK");
        assertEquals("fever", article.annotations.get(0).annotationText);
    }

    /**
     * The {@code saved/} folder is the normal annotation-mode deliverable and
     * is read by every other tool in the pipeline. Writing adjudication
     * bookkeeping into it would be a silent format change, so it must stay
     * free of {@code <processed>}, {@code <AdjudicationStatus>} and
     * {@code <adjudicating>} even while a save happens in adjudication mode.
     */
    @Test
    @DisplayName("The saved/ output is unaffected by the adjudication changes")
    void savedFolderOutput_isUnchanged() throws Exception {
        final String doc = "saved-shape.txt";
        File txtFile = createCorpusFile(doc);

        // regular Depot: what saved/ is written from
        new Depot().articleInsurance(doc);
        Article depotArticle = new Depot().getArticleByFilename(doc);
        depotArticle.annotations.add(annotation("fever", "annotatorA", "SYMPTOM",
                0, 5, 1, Annotation.AdjudicationStatus.UNPROCESSED));
        depotArticle.annotations.add(annotation("cough", "annotatorB", "SYMPTOM",
                10, 15, 2, Annotation.AdjudicationStatus.NON_MATCHES));

        // AdjudicationDepot: what adjudication/ is written from
        Article adjArticle = new Article(doc);
        adjArticle.annotations.add(annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                20, 30, 3, Annotation.AdjudicationStatus.NON_MATCHES));
        new AdjudicationDepot().add(adjArticle);

        new OutputToXML().directsave(txtFile);

        File savedXml = new File(tempDir.toFile(),
                "saved" + File.separator + doc + ".knowtator.xml");
        assertTrue(savedXml.isFile(), "saved/ output should exist");

        Element root = parseXml(savedXml).getRootElement();
        List<Element> annotations = root.getChildren("annotation");

        assertEquals(2, annotations.size(),
                "saved/ must hold one <annotation> per Depot annotation");
        assertEquals(0, root.getChildren("adjudicating").size(),
                "saved/ must never contain <adjudicating> elements");

        for (Element ann : annotations) {
            assertNull(ann.getChild("AdjudicationStatus"),
                    "saved/ <annotation> must not carry adjudication bookkeeping");
            assertNull(ann.getChild("processed"),
                    "saved/ <annotation> must not carry adjudication bookkeeping");
        }

        assertEquals("annotatorA", annotations.get(0).getChild("annotator").getText(),
                "saved/ must keep the original annotator names");
        assertEquals("annotatorB", annotations.get(1).getChild("annotator").getText(),
                "saved/ must keep the original annotator names");
    }

    /**
     * IAA reporting goes through {@link AdjudicationLoader#load()}, which is a
     * different path from {@code loadWorkingState()}: it drops every
     * {@code <adjudicating>} element and reports only the final
     * {@code <annotation>} results. That set must not change.
     */
    @Test
    @DisplayName("The IAA reporting path still sees exactly the final adjudicated results")
    void iaaReportPath_unaffected() throws Exception {
        final String doc = "iaa.txt";

        seedAndSave(doc,
                // final results
                annotation("fever", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 5, 1, Annotation.AdjudicationStatus.MATCHES_OK),
                annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        20, 30, 2, Annotation.AdjudicationStatus.NON_MATCHES),
                // in-progress working state, not a final result
                annotation("cough", "annotatorA", "SYMPTOM",
                        10, 15, 3, Annotation.AdjudicationStatus.NON_MATCHES));

        new Depot().clear();
        AdjudicationDepot.clear();

        assertTrue(AdjudicationLoader.load(), "IAA reporting should find adjudication results");

        Article depotArticle = new Depot().getArticleByFilename(doc);
        assertNotNull(depotArticle, "IAA load should populate the regular Depot");

        Vector<String> texts = new Vector<>();
        for (Annotation ann : depotArticle.annotations) {
            texts.add(ann.annotationText);
            assertEquals(AdjudicationLoader.ADJUDICATION_ANNOTATOR_NAME, ann.getFullAnnotator(),
                    "IAA loads adjudication results under a single synthetic annotator");
        }

        assertEquals(2, texts.size(),
                "IAA must see exactly the two final <annotation> results, not the"
                        + " in-progress <adjudicating> working state; got " + texts);
        assertTrue(texts.contains("fever"), "agreed match must reach the IAA report");
        assertTrue(texts.contains("chest pain"),
                "adjudicator-authored result must reach the IAA report");
        assertFalse(texts.contains("cough"),
                "in-progress working state must not reach the IAA report");

        AdjudicationLoader.cleanup();
        assertEquals(0, new Depot().getArticleByFilename(doc).annotations.size(),
                "cleanup() must remove exactly the annotations load() added");
    }

    /**
     * Two separate adjudicator-authored annotations that happen to look
     * identical are still two annotations. The writer used to collapse them
     * with a span+class+text key — a band-aid for the double-write this fix
     * removes properly, and one that silently deleted the user's work.
     */
    @Test
    @DisplayName("Look-alike adjudicator-authored annotations are not silently collapsed")
    void identicalLookingAnnotations_areBothPreserved() throws Exception {
        final String doc = "lookalike.txt";

        seedAndSave(doc,
                annotation("fever", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 5, 1, Annotation.AdjudicationStatus.MATCHES_OK),
                annotation("fever", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                        0, 5, 2, Annotation.AdjudicationStatus.MATCHES_OK));

        Element root = parseXml(adjudicationXml(doc)).getRootElement();
        assertEquals(2, root.getChildren("annotation").size(),
                "both annotations held in AdjudicationDepot must be written out;"
                        + " dropping one loses the adjudicator's work");

        restartAndResumeAdjudication();

        assertEquals(2, adjudicationDepotCount(doc),
                "both annotations must come back after a reopen");
    }

    /**
     * The upgrade path: a user who saved in-progress adjudication work with the
     * <em>buggy</em> build already has the duplicate pair on disk — the same
     * annotation as an {@code <annotation>} with no status <b>and</b> as an
     * {@code <adjudicating>} carrying the real one. Installing the fix must heal
     * that file, not preserve the duplicate forever.
     *
     * <p>The XML below is a verbatim capture of what the pre-fix build writes,
     * produced by running the old writer against three annotations: an
     * adjudicator-authored unresolved one (duplicated), an agreed match, and a
     * normal annotator's unresolved one.
     */
    @Test
    @DisplayName("In-flight work saved by the pre-fix build heals on resume")
    void preFixDuplicatePair_healsOnResume() throws Exception {
        final String doc = "inflight.txt";
        createCorpusFile(doc);
        writePreFixAdjudicationXml(doc);

        restartAndResumeAdjudication();

        Article article = AdjudicationDepot.getArticleByFilename(doc);
        assertNotNull(article, "pre-fix adjudication folder must still load");

        Vector<String> texts = new Vector<>();
        for (Annotation ann : article.annotations) {
            texts.add(ann.annotationText);
        }

        assertEquals(3, article.annotations.size(),
                "the pre-fix file holds 3 annotations across 4 elements ('chest pain' is"
                        + " written twice). Resuming must collapse that pair back into one"
                        + " annotation, otherwise upgrading preserves the duplicate; got " + texts);

        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                statusOf(article, "chest pain"),
                "the <adjudicating> twin carries the true in-progress status; the"
                        + " status-less <annotation> must not override it with MATCHES_OK");
        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                statusOf(article, "fever"),
                "an agreed match has no twin and must keep defaulting to MATCHES_OK");
        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                statusOf(article, "cough"),
                "a normal annotator's unresolved annotation is unaffected");

        // and saving again must now produce the healed, one-element-per-annotation file
        save(doc);
        Element root = parseXml(adjudicationXml(doc)).getRootElement();
        int elements = root.getChildren("annotation").size()
                + root.getChildren("adjudicating").size();
        assertEquals(3, elements,
                "after the healing save the file must hold exactly 3 elements");
    }

    private Annotation.AdjudicationStatus statusOf(Article article, String annotationText) {
        for (Annotation ann : article.annotations) {
            if (annotationText.equals(ann.annotationText)) {
                return ann.adjudicationStatus;
            }
        }
        throw new AssertionError("no annotation '" + annotationText + "' was restored");
    }

    /** Verbatim output of the pre-fix build; see {@link #preFixDuplicatePair_healsOnResume}. */
    private void writePreFixAdjudicationXml(String txtFilename) throws Exception {
        File adjudicationDir = new File(tempDir.toFile(), "adjudication");
        adjudicationDir.mkdirs();
        File xml = new File(adjudicationDir, txtFilename + ".knowtator.xml");
        try (FileWriter w = new FileWriter(xml)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<annotations textSource=\"" + txtFilename + "\">\n"
                + "    <annotation>\n"
                + "        <mention id=\"EHOST_Instance_50001\" />\n"
                + "        <annotator id=\"Anonymous\">ADJUDICATION</annotator>\n"
                + "        <span start=\"20\" end=\"30\" />\n"
                + "        <spannedText>chest pain</spannedText>\n"
                + "        <creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>\n"
                + "    </annotation>\n"
                + "    <classMention id=\"EHOST_Instance_50001\">\n"
                + "        <mentionClass id=\"SYMPTOM\">chest pain</mentionClass>\n"
                + "    </classMention>\n"
                + "    <annotation>\n"
                + "        <mention id=\"EHOST_Instance_50002\" />\n"
                + "        <annotator id=\"Anonymous\">ADJUDICATION</annotator>\n"
                + "        <span start=\"0\" end=\"5\" />\n"
                + "        <spannedText>fever</spannedText>\n"
                + "        <creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>\n"
                + "    </annotation>\n"
                + "    <classMention id=\"EHOST_Instance_50002\">\n"
                + "        <mentionClass id=\"SYMPTOM\">fever</mentionClass>\n"
                + "    </classMention>\n"
                + "    <adjudicating>\n"
                + "        <mention id=\"EHOST_Instance_50004\" />\n"
                + "        <annotator id=\"Anonymous\">ADJUDICATION</annotator>\n"
                + "        <span start=\"20\" end=\"30\" />\n"
                + "        <spannedText>chest pain</spannedText>\n"
                + "        <creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>\n"
                + "        <processed>false</processed>\n"
                + "        <AdjudicationStatus>NON_MATCHES</AdjudicationStatus>\n"
                + "    </adjudicating>\n"
                + "    <classMention id=\"EHOST_Instance_50004\">\n"
                + "        <mentionClass id=\"SYMPTOM\">chest pain</mentionClass>\n"
                + "    </classMention>\n"
                + "    <adjudicating>\n"
                + "        <mention id=\"EHOST_Instance_50006\" />\n"
                + "        <annotator id=\"Anonymous\">annotatorA</annotator>\n"
                + "        <span start=\"10\" end=\"15\" />\n"
                + "        <spannedText>cough</spannedText>\n"
                + "        <creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>\n"
                + "        <processed>false</processed>\n"
                + "        <AdjudicationStatus>NON_MATCHES</AdjudicationStatus>\n"
                + "    </adjudicating>\n"
                + "    <classMention id=\"EHOST_Instance_50006\">\n"
                + "        <mentionClass id=\"SYMPTOM\">cough</mentionClass>\n"
                + "    </classMention>\n"
                + "</annotations>\n");
        }
    }

    /**
     * Writes an {@code adjudication/} XML in the pre-{@code <adjudicating>}
     * format: plain {@code <annotation>} elements with no adjudication
     * bookkeeping at all.
     */
    private void writeLegacyAdjudicationXml(String txtFilename) throws Exception {
        File adjudicationDir = new File(tempDir.toFile(), "adjudication");
        adjudicationDir.mkdirs();
        File xml = new File(adjudicationDir, txtFilename + ".knowtator.xml");
        try (FileWriter w = new FileWriter(xml)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<annotations textSource=\"" + txtFilename + "\">\n"
                    + "  <annotation>\n"
                    + "    <mention id=\"EHOST_Instance_1\" />\n"
                    + "    <annotator id=\"eHOST_2010\">ADJUDICATION</annotator>\n"
                    + "    <span start=\"0\" end=\"5\" />\n"
                    + "    <spannedText>fever</spannedText>\n"
                    + "    <creationDate>Mon Jan 01 00:00:00 MST 2024</creationDate>\n"
                    + "  </annotation>\n"
                    + "  <classMention id=\"EHOST_Instance_1\">\n"
                    + "    <mentionClass id=\"SYMPTOM\">fever</mentionClass>\n"
                    + "  </classMention>\n"
                    + "</annotations>\n");
        }
    }

    // ------------------------------------------------------------------ //
    // 4. the identity/equality hazard the dedup logic depends on
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Annotation honours the equals/hashCode contract")
    void annotationEqualsHashCodeContract() {
        Annotation a = annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                0, 10, 1, Annotation.AdjudicationStatus.NON_MATCHES);
        Annotation b = annotation("chest pain", ADJUDICATION_ANNOTATOR, "SYMPTOM",
                0, 10, 2, Annotation.AdjudicationStatus.MATCHES_OK);

        assertEquals(a, b, "precondition: Annotation.equals() is value-based");

        assertEquals(a.hashCode(), b.hashCode(),
                "Annotation overrides equals() but not hashCode(), so List.removeAll (value"
                        + " semantics) and HashSet (identity semantics) disagree about what 'the"
                        + " same annotation' means. AdjudicationLoader relies on both.");
    }
}
