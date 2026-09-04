package adjudication;

import adjudication.data.AdjudicationDepot;
import adjudication.parameters.Paras;
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
import resultEditor.annotations.AnnotationIndex;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;
import resultEditor.annotations.SpanSetDef;
import resultEditor.save.OutputToXML;
import testsupport.EhostProjectFixture;
import userInterface.GUI;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end adjudication tests over two <em>real</em> annotator projects on
 * disk, driven entirely without the GUI.
 *
 * <p>Where {@code resultEditor.save.AdjudicationRoundTripTest} seeds
 * {@link AdjudicationDepot} programmatically, this test starts from the
 * artefacts a human would actually have:
 * <ul>
 *   <li>a project with a two-document corpus, annotated by {@code alice};</li>
 *   <li>a byte-for-byte copy of that project, annotated by {@code bob}.</li>
 * </ul>
 * It then runs eHOST's own pipeline over them — XML import, the real
 * {@link Adjudication#searchDifferenceinArticle} comparison engine, the real
 * {@link OutputToXML} writer and the real {@link AdjudicationLoader} reader.
 *
 * <p>The two annotator sets deliberately contain every relationship the
 * comparison engine distinguishes: exact agreement, partial span overlap, the
 * same span with a different class, and spans only one annotator marked. See
 * {@link #annotateAlice}.
 */
public class TwoAnnotatorProjectAdjudicationTest {

    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String ADJUDICATION = "ADJUDICATION";

    private static final String NOTE_1 = "note_001.txt";
    private static final String NOTE_2 = "note_002.txt";

    private static final String NOTE_1_TEXT =
            "RECORD #001\n"
          + "CHIEF COMPLAINT: Chest pain and shortness of breath.\n"
          + "\n"
          + "HISTORY OF PRESENT ILLNESS:\n"
          + "The patient is a 62-year-old male with a history of type 2 diabetes mellitus and hypertension.\n"
          + "He presents to the emergency department with substernal chest pain that began three hours ago.\n"
          + "He reports associated shortness of breath and mild nausea.\n"
          + "He denies fever, chills, or productive cough.\n"
          + "\n"
          + "ASSESSMENT AND PLAN:\n"
          + "Likely unstable angina. Rule out acute myocardial infarction.\n"
          + "Started on aspirin and nitroglycerin.\n";

    private static final String NOTE_2_TEXT =
            "RECORD #002\n"
          + "SUBJECTIVE:\n"
          + "The patient returns for follow-up of chronic obstructive pulmonary disease.\n"
          + "She reports a persistent cough productive of yellow sputum for the past two weeks.\n"
          + "She also notes increased dyspnea on exertion and intermittent wheezing at night.\n"
          + "\n"
          + "OBJECTIVE:\n"
          + "Temperature 100.8 F. Scattered wheezes bilaterally. No peripheral edema noted.\n"
          + "\n"
          + "PLAN:\n"
          + "Start azithromycin. Continue inhaled corticosteroid therapy.\n"
          + "Follow up in two weeks if symptoms persist.\n";

    private static final List<String> CLASSES =
            Arrays.asList("SYMPTOM", "DIAGNOSIS", "DRUG", "FINDING", "VITAL");

    private static final int ALICE_ANNOTATION_COUNT = 15;
    private static final int BOB_ANNOTATION_COUNT = 14;

    @TempDir
    Path tempDir;

    private EhostProjectFixture aliceProject;
    private EhostProjectFixture bobProject;

    private GUI.ReviewMode savedReviewMode;
    private File savedCurrentProject;

    // ------------------------------------------------------------------ //
    // lifecycle
    // ------------------------------------------------------------------ //

    @BeforeEach
    void setUp() {
        savedReviewMode = GUI.reviewmode;
        savedCurrentProject = env.Parameters.WorkSpace.CurrentProject;

        resetGlobalState();
        buildProjects();
    }

    @AfterEach
    void tearDown() {
        GUI.reviewmode = savedReviewMode;
        env.Parameters.WorkSpace.CurrentProject = savedCurrentProject;
        resetGlobalState();
    }

    private void resetGlobalState() {
        new Depot().clear();
        AdjudicationDepot.clear();
        env.Parameters.corpus.RemoveAll();
        env.Parameters.forceChangeLatestUsedMentionID(50000);
        Paras.removeAll();
        Paras.removeParas();
    }

    // ------------------------------------------------------------------ //
    // the fixture: two annotator projects
    // ------------------------------------------------------------------ //

    /**
     * Creates {@code alice/}, annotates it, copies the whole directory to
     * {@code bob/}, and annotates the copy.
     */
    private void buildProjects() {
        aliceProject = new EhostProjectFixture(new File(tempDir.toFile(), "alice"));
        aliceProject.addDocument(NOTE_1, NOTE_1_TEXT);
        aliceProject.addDocument(NOTE_2, NOTE_2_TEXT);
        annotateAlice(aliceProject);
        aliceProject.writeSavedAnnotations();

        File bobDir = new File(tempDir.toFile(), "bob");
        EhostProjectFixture.copyDirectory(aliceProject.dir(), bobDir);

        bobProject = new EhostProjectFixture(bobDir);
        bobProject.adoptDocuments(aliceProject);
        annotateBob(bobProject);
        // Overwrites the copied saved/ XMLs, so bob's project is alice's
        // project carrying bob's annotations — what a second annotator hands
        // back after working from the same corpus.
        bobProject.writeSavedAnnotations();
    }

    /**
     * Alice's annotations. Paired with {@link #annotateBob} they produce:
     *
     * <pre>
     * note_001
     *   "Chest pain"                  SYMPTOM   == bob                     exact agreement
     *   "shortness of breath" (#1)    SYMPTOM   == bob                     exact agreement
     *   "substernal chest pain"       SYMPTOM   ~~ bob "chest pain"        overlap
     *   "type 2 diabetes mellitus"    DIAGNOSIS -- alice only
     *   "mild nausea"                 SYMPTOM   ~~ bob "nausea"            overlap
     *   "unstable angina"             DIAGNOSIS != bob SYMPTOM             same span, class clash
     *   "acute myocardial infarction" DIAGNOSIS == bob                     exact agreement
     *   "aspirin"                     DRUG      -- alice only
     *   "nitroglycerin"               DRUG      == bob                     exact agreement
     *                                           -- bob only "hypertension"
     * note_002
     *   "chronic obstructive pulmonary disease" DIAGNOSIS == bob           exact agreement
     *   "cough productive of yellow sputum"     SYMPTOM   ~~ bob "cough"   overlap
     *   "dyspnea on exertion"                   SYMPTOM   ~~ bob "dyspnea" overlap
     *   "Temperature 100.8 F"                   VITAL     -- alice only
     *   "Scattered wheezes bilaterally"         FINDING   ~~ bob "wheezes" overlap + class clash
     *   "azithromycin"                          DRUG      == bob           exact agreement
     *                                                     -- bob only "intermittent wheezing"
     * </pre>
     */
    private void annotateAlice(EhostProjectFixture p) {
        p.annotate(NOTE_1, ALICE, "SYMPTOM", "Chest pain");
        p.annotate(NOTE_1, ALICE, "SYMPTOM", "shortness of breath", 1);
        p.annotate(NOTE_1, ALICE, "SYMPTOM", "substernal chest pain");
        p.annotate(NOTE_1, ALICE, "DIAGNOSIS", "type 2 diabetes mellitus");
        p.annotate(NOTE_1, ALICE, "SYMPTOM", "mild nausea");
        p.annotate(NOTE_1, ALICE, "DIAGNOSIS", "unstable angina");
        p.annotate(NOTE_1, ALICE, "DIAGNOSIS", "acute myocardial infarction");
        p.annotate(NOTE_1, ALICE, "DRUG", "aspirin");
        p.annotate(NOTE_1, ALICE, "DRUG", "nitroglycerin");

        p.annotate(NOTE_2, ALICE, "DIAGNOSIS", "chronic obstructive pulmonary disease");
        p.annotate(NOTE_2, ALICE, "SYMPTOM", "cough productive of yellow sputum");
        p.annotate(NOTE_2, ALICE, "SYMPTOM", "dyspnea on exertion");
        p.annotate(NOTE_2, ALICE, "VITAL", "Temperature 100.8 F");
        p.annotate(NOTE_2, ALICE, "FINDING", "Scattered wheezes bilaterally");
        p.annotate(NOTE_2, ALICE, "DRUG", "azithromycin");
    }

    /** Bob's annotations; see {@link #annotateAlice} for the pairing. */
    private void annotateBob(EhostProjectFixture p) {
        p.annotate(NOTE_1, BOB, "SYMPTOM", "Chest pain");
        p.annotate(NOTE_1, BOB, "SYMPTOM", "shortness of breath", 1);
        p.annotate(NOTE_1, BOB, "SYMPTOM", "chest pain");
        p.annotate(NOTE_1, BOB, "DIAGNOSIS", "hypertension");
        p.annotate(NOTE_1, BOB, "SYMPTOM", "nausea");
        p.annotate(NOTE_1, BOB, "SYMPTOM", "unstable angina");
        p.annotate(NOTE_1, BOB, "DIAGNOSIS", "acute myocardial infarction");
        p.annotate(NOTE_1, BOB, "DRUG", "nitroglycerin");

        p.annotate(NOTE_2, BOB, "DIAGNOSIS", "chronic obstructive pulmonary disease");
        p.annotate(NOTE_2, BOB, "SYMPTOM", "cough");
        p.annotate(NOTE_2, BOB, "SYMPTOM", "dyspnea");
        p.annotate(NOTE_2, BOB, "SYMPTOM", "intermittent wheezing");
        p.annotate(NOTE_2, BOB, "SYMPTOM", "wheezes");
        p.annotate(NOTE_2, BOB, "DRUG", "azithromycin");
    }

    // ------------------------------------------------------------------ //
    // driving eHOST headlessly
    // ------------------------------------------------------------------ //

    /**
     * Opens alice's project and merges bob's annotations into the Depot.
     *
     * <p>This is what eHOST itself does: {@code Reload} imports the current
     * project's {@code saved/} folder, and "import annotations"
     * ({@code GUI.extractAnnotation_fromXML}) imports a second annotator's
     * XMLs. Both funnel into {@link ImportAnnotation#XMLImporter}.
     */
    private void openProjectWithBothAnnotators() {
        env.Parameters.WorkSpace.CurrentProject = aliceProject.dir();
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;

        importSaved(aliceProject.dir());
        importSaved(bobProject.dir());
    }

    private void importSaved(File projectDir) {
        Vector<File> xmls = new Vector<File>(EhostProjectFixture.savedXmls(projectDir));
        assertFalse(xmls.isEmpty(), "no saved/ XMLs under " + projectDir);
        new ImportAnnotation().XMLImporter(xmls);
    }

    /**
     * Runs a fresh difference analysis, replicating
     * {@code Adjudication.checkAnnotations(true)} minus its Swing calls.
     *
     * <p>Every step below is the production method the GUI itself invokes, in
     * the GUI's order.
     */
    private void startNewAdjudication() throws Exception {
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        Paras.removeAll();
        Paras.removeParas();
        Paras.setAnnotators(new ArrayList<String>(Arrays.asList(ALICE, BOB)));
        Paras.addAnnotator(ADJUDICATION);
        Paras.setClasses(new ArrayList<String>(CLASSES));

        AdjudicationDepot adjDepot = new AdjudicationDepot();
        adjDepot.copyAnnotations(Paras.getAnnotators(), Paras.getClasses(), true);
        adjDepot.resetAnntationStatus(Paras.getAnnotators(), Paras.getClasses(),
                null, false, false);
        Adjudication.translateAnnotationStatus(null, false);

        for (Article article : adjDepot.getAllArticles()) {
            Adjudication.searchDifferenceinArticle(article, ALICE);
        }

        Paras.__adjudicated = true;
    }

    /** Saves every document, as pressing Save in the GUI does. */
    private void saveAll() {
        for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
            new Depot().articleInsurance(doc);
            new OutputToXML().directsave(aliceProject.corpusFile(doc));
        }
    }

    /**
     * Simulates quitting eHOST and reopening the project with "continue
     * previous adjudication work".
     */
    private void restartAndResumeAdjudication() {
        // the process exits: every static depot is gone
        new Depot().clear();
        AdjudicationDepot.clear();
        assertFalse(AdjudicationDepot.isReady(),
                "precondition: AdjudicationDepot must be empty after a simulated restart");

        // project open: Reload pulls saved/ back into the regular Depot
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;
        importSaved(aliceProject.dir());

        // the user chooses "continue previous adjudication work"
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;
        AdjudicationLoader.loadWorkingState();
    }

    // ------------------------------------------------------------------ //
    // adjudicator actions
    // ------------------------------------------------------------------ //

    private Annotation adjAnnotation(String doc, String text, String annotator) {
        Annotation found = findAdjAnnotation(doc, text, annotator);
        if (found == null) {
            throw new AssertionError("no adjudication annotation '" + text + "'"
                    + (annotator == null ? "" : " by " + annotator) + " in " + doc);
        }
        return found;
    }

    private Annotation findAdjAnnotation(String doc, String text, String annotator) {
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        assertNotNull(article, "no adjudication article for " + doc);
        for (Annotation ann : article.annotations) {
            if (text.equals(ann.annotationText)
                    && (annotator == null || annotator.equals(ann.getAnnotator()))) {
                return ann;
            }
        }
        return null;
    }

    /**
     * The adjudicator settles a finding on {@code text}: the first annotation
     * carrying it survives as the agreed result and every other annotation of
     * the same finding is discarded.
     *
     * <p>This is what {@code Depot.SelectedAnnotationSet.data_onlyKeepPrimaryAnnotation}
     * does when the "accept" button is pressed. The surviving annotation is
     * flipped to {@code MATCHES_OK} <em>and re-attributed to ADJUDICATION</em>
     * by {@link Depot#setAnnotationToMatchedOK_byUID}, while each partner is
     * removed by {@link Depot#deleteAnnotation_byUID_onAdjudicationMode}. Both
     * production methods are called here directly; only the surrounding Swing
     * state is left out.
     */
    private void acceptFinding(String doc, String text) {
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        assertNotNull(article, "no adjudication article for " + doc);

        List<Integer> uids = new ArrayList<Integer>();
        for (Annotation ann : article.annotations) {
            if (text.equals(ann.annotationText)) {
                uids.add(ann.uniqueIndex);
            }
        }
        assertFalse(uids.isEmpty(), "nothing to accept for '" + text + "' in " + doc);

        int keepUid = uids.get(0);
        for (int i = 1; i < uids.size(); i++) {
            Depot.deleteAnnotation_byUID_onAdjudicationMode(doc, uids.get(i));
        }
        Depot.setAnnotationToMatchedOK_byUID(doc, keepUid);
    }

    /**
     * The adjudicator rejects an annotation outright, as the "reject" button
     * does via {@code data_onlyDeletePrimaryAnnotation}.
     */
    private void reject(String doc, String text, String annotator) {
        int uid = adjAnnotation(doc, text, annotator).uniqueIndex;
        AdjudicationDepot.deleteAnnotation_byUID(doc, uid);
    }

    /**
     * The adjudicator resolves a disagreement by creating an annotation of
     * their own, which eHOST attributes to the {@code ADJUDICATION} annotator.
     *
     * <p>This is the case at the heart of the duplicate defect: an
     * {@code ADJUDICATION}-authored annotation that is not yet
     * {@code MATCHES_OK}, which both XML writers used to claim.
     */
    private Annotation createAdjudicatorAnnotation(String doc, String phrase,
            String annClass, Annotation.AdjudicationStatus status) {
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        assertNotNull(article, "no adjudication article for " + doc);

        int start = textOf(doc).indexOf(phrase);
        assertTrue(start >= 0, "\"" + phrase + "\" does not occur in " + doc);

        Annotation ann = new Annotation();
        ann.annotationText = phrase;
        ann.setAnnotator(ADJUDICATION);
        ann.annotationclass = annClass;
        ann.spanset = new SpanSetDef();
        ann.spanset.setOnlySpan(start, start + phrase.length());
        ann.uniqueIndex = AnnotationIndex.newAnnotationIndex();
        ann.mentionid = "EHOST_Instance_ADJ_" + ann.uniqueIndex;
        ann.creationDate = "Mon Jan 01 00:00:00 MST 2024";
        ann.adjudicationStatus = status;
        article.annotations.add(ann);
        return ann;
    }

    private String textOf(String doc) {
        return NOTE_1.equals(doc) ? NOTE_1_TEXT : NOTE_2_TEXT;
    }

    // ------------------------------------------------------------------ //
    // XML inspection
    // ------------------------------------------------------------------ //

    private Document parse(File xml) throws Exception {
        return new SAXBuilder().build(xml);
    }

    /**
     * The multiset of annotations an adjudication XML holds, keyed by the
     * fields that identify a finding: its spans, its text and its class.
     *
     * <p>Both {@code <annotation>} and {@code <adjudicating>} entries count —
     * the defect under test was one in-memory annotation being emitted as one
     * of each.
     *
     * <p>Annotator is deliberately excluded from the key: the writer relabels
     * every final {@code <annotation>} in this folder as {@code ADJUDICATION}
     * regardless of who authored it, so it cannot distinguish entries.
     */
    private Map<String, Integer> xmlMultiset(File xml) throws Exception {
        Element root = parse(xml).getRootElement();

        Map<String, String> classByMention = new HashMap<String, String>();
        for (Object o : root.getChildren("classMention")) {
            Element cm = (Element) o;
            Element mc = cm.getChild("mentionClass");
            if (mc != null) {
                classByMention.put(cm.getAttributeValue("id"), mc.getAttributeValue("id"));
            }
        }

        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String tag : Arrays.asList("annotation", "adjudicating")) {
            for (Object o : root.getChildren(tag)) {
                Element e = (Element) o;
                StringBuilder spans = new StringBuilder();
                for (Object s : e.getChildren("span")) {
                    Element span = (Element) s;
                    spans.append(span.getAttributeValue("start")).append('-')
                         .append(span.getAttributeValue("end")).append(';');
                }
                String mentionId = e.getChild("mention").getAttributeValue("id");
                String key = spans + "|" + e.getChildText("spannedText")
                        + "|" + classByMention.get(mentionId);
                Integer prev = counts.get(key);
                counts.put(key, prev == null ? 1 : prev + 1);
            }
        }
        return counts;
    }

    /** The same multiset, taken from the in-memory adjudication working set. */
    private Map<String, Integer> memoryMultiset(String doc) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        if (article == null) {
            return counts;
        }
        for (Annotation ann : article.annotations) {
            StringBuilder spans = new StringBuilder();
            if (ann.spanset != null) {
                for (int i = 0; i < ann.spanset.size(); i++) {
                    spans.append(ann.spanset.getSpanAt(i).start).append('-')
                         .append(ann.spanset.getSpanAt(i).end).append(';');
                }
            }
            String key = spans + "|" + ann.annotationText + "|" + ann.annotationclass;
            Integer prev = counts.get(key);
            counts.put(key, prev == null ? 1 : prev + 1);
        }
        return counts;
    }

    /**
     * Asserts the file on disk is an exact image of the working set: one entry
     * per in-memory annotation, no more. Any double-write shows up as a count
     * of 2 on the XML side against 1 in memory.
     */
    private void assertXmlMirrorsMemory(String doc, String when) throws Exception {
        File xml = aliceProject.adjudicationXml(doc);
        assertTrue(xml.isFile(), "no adjudication XML written for " + doc + " " + when);
        assertEquals(memoryMultiset(doc), xmlMultiset(xml),
                "the adjudication XML for " + doc + " does not mirror memory " + when);
        assertEquals(adjudicationDepotCount(doc), totalEntries(xml),
                "wrong number of entries in " + doc + " " + when);
    }

    private int totalEntries(File xml) throws Exception {
        Element root = parse(xml).getRootElement();
        return root.getChildren("annotation").size() + root.getChildren("adjudicating").size();
    }

    private int adjudicationDepotCount(String doc) {
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        return article == null ? 0 : article.annotations.size();
    }

    // ================================================================== //
    // 1. the fixture itself must be trustworthy
    // ================================================================== //

    @Test
    @DisplayName("Fixture: two real projects, identical corpora, differing annotations")
    void fixture_isRealisticAndConsistent() {
        for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
            assertTrue(aliceProject.corpusFile(doc).isFile(), "alice corpus missing " + doc);
            assertTrue(bobProject.corpusFile(doc).isFile(), "bob corpus missing " + doc);
            assertEquals(
                    EhostProjectFixture.read(aliceProject.corpusFile(doc)),
                    EhostProjectFixture.read(bobProject.corpusFile(doc)),
                    "the two annotators must be working from identical text");
            assertTrue(aliceProject.savedXml(doc).isFile(), "alice saved/ missing " + doc);
            assertTrue(bobProject.savedXml(doc).isFile(), "bob saved/ missing " + doc);
            assertNotEquals(
                    EhostProjectFixture.read(aliceProject.savedXml(doc)),
                    EhostProjectFixture.read(bobProject.savedXml(doc)),
                    "the two annotators must disagree somewhere in " + doc);
        }
    }

    @Test
    @DisplayName("Fixture: every span in the XML matches the corpus text it points at")
    void fixture_spansAgreeWithCorpusText() throws Exception {
        for (EhostProjectFixture project : Arrays.asList(aliceProject, bobProject)) {
            for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
                String text = EhostProjectFixture.read(project.corpusFile(doc));
                Element root = parse(project.savedXml(doc)).getRootElement();
                for (Object o : root.getChildren("annotation")) {
                    Element e = (Element) o;
                    Element span = e.getChild("span");
                    int start = Integer.parseInt(span.getAttributeValue("start"));
                    int end = Integer.parseInt(span.getAttributeValue("end"));
                    assertEquals(e.getChildText("spannedText"), text.substring(start, end),
                            "span " + start + "-" + end + " in " + doc
                                    + " does not cover its spannedText");
                }
            }
        }
    }

    @Test
    @DisplayName("Both annotators load into one Depot, keeping their identities")
    void bothAnnotators_loadIntoDepot() {
        openProjectWithBothAnnotators();

        int alice = 0;
        int bob = 0;
        for (Article article : new Depot().getAllArticles()) {
            for (Annotation ann : article.annotations) {
                if (ALICE.equals(ann.getAnnotator())) {
                    alice++;
                } else if (BOB.equals(ann.getAnnotator())) {
                    bob++;
                }
            }
        }
        assertEquals(ALICE_ANNOTATION_COUNT, alice, "alice's annotations");
        assertEquals(BOB_ANNOTATION_COUNT, bob, "bob's annotations");
    }

    @Test
    @DisplayName("The comparison engine classifies agreements and disagreements")
    void comparisonEngine_classifiesTheFixture() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        assertTrue(AdjudicationDepot.isReady(), "adjudication working set was not built");
        assertEquals(ALICE_ANNOTATION_COUNT + BOB_ANNOTATION_COUNT,
                adjudicationDepotCount(NOTE_1) + adjudicationDepotCount(NOTE_2),
                "every annotation from both annotators should enter adjudication");

        int matched = 0;
        int unmatched = 0;
        for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
            for (Annotation ann : AdjudicationDepot.getArticleByFilename(doc).annotations) {
                if (ann.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK) {
                    matched++;
                } else {
                    unmatched++;
                }
            }
        }
        assertTrue(matched > 0, "the exact-agreement pairs should have matched");
        assertTrue(unmatched > 0,
                "the single-annotator and class-clash annotations should stay unresolved");
    }

    // ================================================================== //
    // 2. the duplicate defect, end to end
    // ================================================================== //

    /**
     * The headline scenario from the bug report, over real projects:
     * adjudicate, save, quit, reopen, continue, save.
     */
    @Test
    @DisplayName("Adjudicate -> save -> reopen -> continue -> save duplicates nothing")
    void fullAdjudicationLifecycle_producesNoDuplicates() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        // --- session 1: a realistic set of adjudicator decisions ---------
        // both annotators found "Chest pain": keep one as the agreed result
        acceptFinding(NOTE_1, "Chest pain");
        // only bob marked hypertension: reject it
        reject(NOTE_1, "hypertension", BOB);
        // resolve the substernal/chest-pain overlap with the adjudicator's own
        // span, discarding both annotators' versions
        reject(NOTE_1, "substernal chest pain", ALICE);
        reject(NOTE_1, "chest pain", BOB);
        createAdjudicatorAnnotation(NOTE_1, "substernal chest pain", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);

        acceptFinding(NOTE_2, "chronic obstructive pulmonary disease");
        reject(NOTE_2, "cough productive of yellow sputum", ALICE);
        reject(NOTE_2, "cough", BOB);
        createAdjudicatorAnnotation(NOTE_2, "cough productive of yellow sputum", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);

        Map<String, Integer> afterSession1 = new HashMap<String, Integer>();
        afterSession1.put(NOTE_1, adjudicationDepotCount(NOTE_1));
        afterSession1.put(NOTE_2, adjudicationDepotCount(NOTE_2));

        saveAll();

        for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
            assertXmlMirrorsMemory(doc, "after the first save");
        }

        // --- restart, continue previous adjudication ---------------------
        restartAndResumeAdjudication();

        for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
            assertEquals(afterSession1.get(doc).intValue(), adjudicationDepotCount(doc),
                    "resuming " + doc + " changed the number of adjudication annotations");
        }

        // --- session 2: save again, nothing should change ----------------
        saveAll();

        for (String doc : Arrays.asList(NOTE_1, NOTE_2)) {
            assertXmlMirrorsMemory(doc, "after the second save");
            assertEquals(afterSession1.get(doc).intValue(),
                    totalEntries(aliceProject.adjudicationXml(doc)),
                    "the second save changed the entry count for " + doc);
        }
    }

    @Test
    @DisplayName("Five reopen/save cycles leave the adjudication folder stable")
    void repeatedCycles_areStable() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        acceptFinding(NOTE_1, "Chest pain");
        reject(NOTE_1, "hypertension", BOB);
        reject(NOTE_1, "substernal chest pain", ALICE);
        createAdjudicatorAnnotation(NOTE_1, "substernal chest pain", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);
        acceptFinding(NOTE_2, "azithromycin");
        createAdjudicatorAnnotation(NOTE_2, "dyspnea on exertion", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);

        saveAll();

        int baseline1 = totalEntries(aliceProject.adjudicationXml(NOTE_1));
        int baseline2 = totalEntries(aliceProject.adjudicationXml(NOTE_2));
        Map<String, Integer> expected1 = xmlMultiset(aliceProject.adjudicationXml(NOTE_1));
        Map<String, Integer> expected2 = xmlMultiset(aliceProject.adjudicationXml(NOTE_2));

        for (int cycle = 1; cycle <= 5; cycle++) {
            restartAndResumeAdjudication();
            saveAll();

            assertEquals(baseline1, totalEntries(aliceProject.adjudicationXml(NOTE_1)),
                    NOTE_1 + " changed size on cycle " + cycle);
            assertEquals(baseline2, totalEntries(aliceProject.adjudicationXml(NOTE_2)),
                    NOTE_2 + " changed size on cycle " + cycle);
            assertEquals(expected1, xmlMultiset(aliceProject.adjudicationXml(NOTE_1)),
                    NOTE_1 + " changed content on cycle " + cycle);
            assertEquals(expected2, xmlMultiset(aliceProject.adjudicationXml(NOTE_2)),
                    NOTE_2 + " changed content on cycle " + cycle);
            assertXmlMirrorsMemory(NOTE_1, "on cycle " + cycle);
            assertXmlMirrorsMemory(NOTE_2, "on cycle " + cycle);
        }
    }

    @Test
    @DisplayName("Adjudication decisions survive a restart intact")
    void adjudicationDecisions_surviveRestart() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        // "aspirin" is alice's alone, so the engine leaves it unresolved and
        // the adjudicator has a real decision to make.
        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                adjAnnotation(NOTE_1, "aspirin", ALICE).adjudicationStatus,
                "precondition: a single-annotator finding starts unresolved");
        acceptFinding(NOTE_1, "aspirin");

        createAdjudicatorAnnotation(NOTE_1, "unstable angina", "DIAGNOSIS",
                Annotation.AdjudicationStatus.NON_MATCHES);

        // left untouched by the adjudicator, so still unresolved
        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                adjAnnotation(NOTE_1, "type 2 diabetes mellitus", ALICE).adjudicationStatus,
                "precondition: alice's unique finding should be unresolved");

        Map<String, Integer> before = statusHistogram(NOTE_1);

        saveAll();
        restartAndResumeAdjudication();

        assertEquals(before, statusHistogram(NOTE_1),
                "the mix of adjudication statuses changed across the restart");

        // An accepted annotation is re-attributed to ADJUDICATION by
        // Depot.setAnnotationToMatchedOK_byUID, so it is looked up by text.
        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                adjAnnotation(NOTE_1, "aspirin", null).adjudicationStatus,
                "an accepted annotation came back with the wrong status");
        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                adjAnnotation(NOTE_1, "unstable angina", ADJUDICATION).adjudicationStatus,
                "the adjudicator's own unresolved annotation came back with the wrong status");

        // An unresolved annotation round-trips as <adjudicating>, which does
        // preserve its author.
        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                adjAnnotation(NOTE_1, "type 2 diabetes mellitus", ALICE).adjudicationStatus,
                "an unresolved annotation came back with the wrong status");
    }

    /**
     * An exact agreement needs no adjudicator input: the comparison engine
     * settles it, keeping one copy as {@code MATCHES_OK} and marking the
     * redundant partner {@code MATCHES_DLETED}. Both must survive a restart
     * with those statuses, or resuming would re-open a settled difference.
     */
    @Test
    @DisplayName("An auto-resolved exact agreement keeps both statuses across a restart")
    void autoResolvedMatch_survivesRestart() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        List<Annotation.AdjudicationStatus> before =
                statusesOf(NOTE_1, "acute myocardial infarction");
        assertTrue(before.contains(Annotation.AdjudicationStatus.MATCHES_OK),
                "the engine should have accepted one copy of the agreed finding");
        assertTrue(before.contains(Annotation.AdjudicationStatus.MATCHES_DLETED),
                "the engine should have retired the redundant copy");

        saveAll();
        restartAndResumeAdjudication();

        assertEquals(before, statusesOf(NOTE_1, "acute myocardial infarction"),
                "an auto-resolved agreement changed after the restart");
    }

    private List<Annotation.AdjudicationStatus> statusesOf(String doc, String text) {
        List<Annotation.AdjudicationStatus> statuses =
                new ArrayList<Annotation.AdjudicationStatus>();
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        assertNotNull(article, "no adjudication article for " + doc);
        for (Annotation ann : article.annotations) {
            if (text.equals(ann.annotationText)) {
                statuses.add(ann.adjudicationStatus);
            }
        }
        java.util.Collections.sort(statuses);
        return statuses;
    }

    private Map<String, Integer> statusHistogram(String doc) {
        Map<String, Integer> counts = new java.util.TreeMap<String, Integer>();
        Article article = AdjudicationDepot.getArticleByFilename(doc);
        if (article != null) {
            for (Annotation ann : article.annotations) {
                String key = String.valueOf(ann.adjudicationStatus);
                Integer prev = counts.get(key);
                counts.put(key, prev == null ? 1 : prev + 1);
            }
        }
        return counts;
    }

    /**
     * The exact intersection the two writers used to both claim: authored by
     * {@code ADJUDICATION} <em>and</em> not {@code MATCHES_OK}.
     */
    @Test
    @DisplayName("An ADJUDICATION-authored unresolved annotation is written exactly once")
    void adjudicatorAuthoredUnresolved_isWrittenOnce() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        createAdjudicatorAnnotation(NOTE_1, "productive cough", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);

        saveAll();

        Element root = parse(aliceProject.adjudicationXml(NOTE_1)).getRootElement();
        int occurrences = 0;
        for (String tag : Arrays.asList("annotation", "adjudicating")) {
            for (Object o : root.getChildren(tag)) {
                if ("productive cough".equals(((Element) o).getChildText("spannedText"))) {
                    occurrences++;
                }
            }
        }
        assertEquals(1, occurrences,
                "the adjudicator's unresolved annotation was serialized "
                        + occurrences + " times");
    }

    @Test
    @DisplayName("The saved/ deliverable keeps both annotators and no adjudication bookkeeping")
    void savedFolder_isUnpolluted() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();
        acceptFinding(NOTE_1, "Chest pain");
        saveAll();

        Element root = parse(aliceProject.savedXml(NOTE_1)).getRootElement();

        assertTrue(root.getChildren("adjudicating").isEmpty(),
                "saved/ must never contain <adjudicating> elements");

        boolean sawAlice = false;
        boolean sawBob = false;
        for (Object o : root.getChildren("annotation")) {
            Element e = (Element) o;
            assertNull(e.getChild("AdjudicationStatus"),
                    "saved/ must not carry adjudication bookkeeping");
            assertNull(e.getChild("processed"),
                    "saved/ must not carry adjudication bookkeeping");
            if (ALICE.equals(e.getChildText("annotator"))) {
                sawAlice = true;
            }
            if (BOB.equals(e.getChildText("annotator"))) {
                sawBob = true;
            }
        }
        assertTrue(sawAlice && sawBob, "saved/ should still hold both annotators' work");
    }

    @Test
    @DisplayName("The IAA reporting path sees each adjudicated result exactly once")
    void iaaReportingPath_seesNoDuplicates() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();

        acceptFinding(NOTE_1, "Chest pain");
        acceptFinding(NOTE_2, "azithromycin");
        reject(NOTE_1, "substernal chest pain", ALICE);
        reject(NOTE_1, "chest pain", BOB);
        createAdjudicatorAnnotation(NOTE_1, "substernal chest pain", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);
        saveAll();

        // a report run starts from a clean Depot holding only the annotators
        new Depot().clear();
        AdjudicationDepot.clear();
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;
        importSaved(aliceProject.dir());

        assertTrue(AdjudicationLoader.isAdjudicationAvailable(),
                "the adjudication folder should be discoverable");
        assertTrue(AdjudicationLoader.load(), "adjudication results failed to load");

        Map<String, Integer> perFinding = new LinkedHashMap<String, Integer>();
        int adjudicationTotal = 0;
        for (Article article : new Depot().getAllArticles()) {
            for (Annotation ann : article.annotations) {
                if (AdjudicationLoader.ADJUDICATION_ANNOTATOR_NAME.equals(ann.getAnnotator())) {
                    adjudicationTotal++;
                    String key = article.filename + "|" + ann.annotationText
                            + "|" + ann.annotationclass;
                    Integer prev = perFinding.get(key);
                    perFinding.put(key, prev == null ? 1 : prev + 1);
                }
            }
        }

        assertTrue(adjudicationTotal > 0, "no adjudication results reached the report");
        for (Map.Entry<String, Integer> e : perFinding.entrySet()) {
            assertEquals(1, e.getValue().intValue(),
                    "the report saw " + e.getKey() + " " + e.getValue() + " times");
        }

        AdjudicationLoader.cleanup();
        for (Article article : new Depot().getAllArticles()) {
            for (Annotation ann : article.annotations) {
                assertNotEquals(AdjudicationLoader.ADJUDICATION_ANNOTATOR_NAME,
                        ann.getAnnotator(),
                        "cleanup() left adjudication annotations behind");
            }
        }
    }

    /**
     * Guards the upgrade path: a project whose {@code adjudication/} folder was
     * written by the pre-fix build, opened for the first time by the fixed one.
     */
    @Test
    @DisplayName("A duplicate pair left by the pre-fix build heals on the first resume")
    void preFixDuplicates_healOnResume() throws Exception {
        openProjectWithBothAnnotators();
        startNewAdjudication();
        reject(NOTE_1, "substernal chest pain", ALICE);
        reject(NOTE_1, "chest pain", BOB);
        createAdjudicatorAnnotation(NOTE_1, "substernal chest pain", "SYMPTOM",
                Annotation.AdjudicationStatus.NON_MATCHES);
        saveAll();

        File xml = aliceProject.adjudicationXml(NOTE_1);
        int healthyCount = totalEntries(xml);
        Map<String, Integer> healthyContent = xmlMultiset(xml);

        injectPreFixDuplicate(xml, "substernal chest pain");
        assertEquals(healthyCount + 1, totalEntries(xml),
                "precondition: the pre-fix duplicate should have been injected");

        restartAndResumeAdjudication();

        assertEquals(healthyCount, adjudicationDepotCount(NOTE_1),
                "the legacy duplicate was not healed on resume");

        saveAll();
        assertEquals(healthyCount, totalEntries(xml),
                "the healed file grew again when saved");
        assertEquals(healthyContent, xmlMultiset(xml),
                "the healed file no longer matches the pre-duplication content");
    }

    /**
     * Rewrites an adjudication XML into the exact shape the pre-fix build
     * produced for an adjudicator-authored, unresolved annotation: the
     * {@code <annotation>} copy carries no adjudication bookkeeping (that build
     * only wrote it for mirror-memory nodes), and an {@code <adjudicating>}
     * twin beside it carries the real status.
     */
    private void injectPreFixDuplicate(File xml, String spannedText) throws Exception {
        Document doc = parse(xml);
        Element root = doc.getRootElement();

        Element original = null;
        for (Object o : root.getChildren("annotation")) {
            Element e = (Element) o;
            if (spannedText.equals(e.getChildText("spannedText"))) {
                original = e;
                break;
            }
        }
        assertNotNull(original, "no <annotation> for " + spannedText + " to duplicate");

        Element twin = (Element) original.clone();
        twin.setName("adjudicating");
        String twinId = "EHOST_Instance_LEGACY_TWIN";
        twin.getChild("mention").setAttribute("id", twinId);

        // the pre-fix <annotation> copy had no status of its own
        original.removeChild("AdjudicationStatus");
        original.removeChild("processed");

        root.addContent(twin);

        Element classMention = new Element("classMention");
        classMention.setAttribute("id", twinId);
        Element mentionClass = new Element("mentionClass");
        mentionClass.setAttribute("id", "SYMPTOM");
        mentionClass.addContent(spannedText);
        classMention.addContent(mentionClass);
        root.addContent(classMention);

        try (FileWriter w = new FileWriter(xml)) {
            new org.jdom.output.XMLOutputter().output(doc, w);
        }
    }
}
