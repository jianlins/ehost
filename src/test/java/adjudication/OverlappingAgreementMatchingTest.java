package adjudication;

import adjudication.data.AdjudicationDepot;
import adjudication.parameters.Paras;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;
import resultEditor.save.OutputToXML;
import testsupport.EhostProjectFixture;
import userInterface.GUI;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers how {@link Adjudication#searchDifferenceinArticle} treats several
 * annotations that share or overlap one span.
 *
 * <p>Annotators routinely tag one span more than once — most often the same
 * text under two classes. The comparison engine used to treat any overlapping
 * annotation that failed the class/attribute comparison as proof that the
 * annotation under consideration was <em>disputed</em>, and marked the whole
 * group {@code NON_MATCHES}. So when both annotators agreed twice on one span,
 * one agreement was resolved and the other was shown to the adjudicator as a
 * disagreement, even though the two annotators had written exactly the same
 * thing.
 *
 * <p>An annotation that overlaps but compares differently is simply a
 * <em>different</em> annotation. Whether the annotators actually agreed is
 * decided by {@code checkAnnotators()}, which requires every selected annotator
 * to be represented among the matches. These tests hold both directions: real
 * agreements resolve, and real disagreements are still reported.
 */
public class OverlappingAgreementMatchingTest {

    private static final String A1 = "a1";
    private static final String A2 = "a2";
    private static final String DOC = "doc.txt";

    private static final String TEXT =
            "The patient continues on Eltroxin which was started last year.\n";

    private static final String CONCEPT = "CONCEPT";
    private static final String CON2 = "CON2";

    @TempDir
    Path tempDir;

    private EhostProjectFixture project;

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

        project = new EhostProjectFixture(new File(tempDir.toFile(), "proj"));
        project.addDocument(DOC, TEXT);
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
    // driving eHOST headlessly
    // ------------------------------------------------------------------ //

    /** Writes the fixture, opens it, and runs a fresh difference analysis. */
    private void adjudicate() throws Exception {
        project.writeSavedAnnotations();

        env.Parameters.WorkSpace.CurrentProject = project.dir();
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;
        new ImportAnnotation().XMLImporter(
                new Vector<File>(EhostProjectFixture.savedXmls(project.dir())));

        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;
        Paras.removeAll();
        Paras.removeParas();
        Paras.setAnnotators(new ArrayList<String>(Arrays.asList(A1, A2)));
        Paras.addAnnotator("ADJUDICATION");
        Paras.setClasses(new ArrayList<String>(Arrays.asList(CONCEPT, CON2)));

        AdjudicationDepot adj = new AdjudicationDepot();
        adj.copyAnnotations(Paras.getAnnotators(), Paras.getClasses(), true);
        adj.resetAnntationStatus(Paras.getAnnotators(), Paras.getClasses(), null, false, false);
        Adjudication.translateAnnotationStatus(null, false);
        for (Article article : adj.getAllArticles()) {
            Adjudication.searchDifferenceinArticle(article, A1);
        }
    }

    private Article article() {
        Article article = AdjudicationDepot.getArticleByFilename(DOC);
        assertNotNull(article, "no adjudication article for " + DOC);
        return article;
    }

    /** Status counts across the working set, e.g. {MATCHES_OK=2, ...}. */
    private Map<String, Integer> statusHistogram() {
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        for (Annotation ann : article().annotations) {
            String key = String.valueOf(ann.adjudicationStatus);
            Integer prev = counts.get(key);
            counts.put(key, prev == null ? 1 : prev + 1);
        }
        return counts;
    }

    private Annotation annotationOf(String annotator, String annClass) {
        for (Annotation ann : article().annotations) {
            if (annotator.equals(ann.getAnnotator()) && annClass.equals(ann.annotationclass)) {
                return ann;
            }
        }
        throw new AssertionError("no " + annClass + " annotation by " + annotator);
    }

    private static Map<String, Integer> histogram(String... pairs) {
        Map<String, Integer> expected = new TreeMap<String, Integer>();
        for (int i = 0; i < pairs.length; i += 2) {
            expected.put(pairs[i], Integer.valueOf(pairs[i + 1]));
        }
        return expected;
    }

    // ================================================================== //
    // agreements that must resolve
    // ================================================================== //

    @Test
    @DisplayName("One agreed pair on a span resolves")
    void singleAgreedPair_resolves() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin");
        project.annotate(DOC, A2, CONCEPT, "Eltroxin");

        adjudicate();

        assertEquals(histogram("MATCHES_OK", "1", "MATCHES_DLETED", "1"), statusHistogram(),
                "a plain agreement should be settled by the engine");
    }

    @Test
    @DisplayName("Two agreed pairs on the same span both resolve")
    void twoAgreedPairs_identicalSpans_bothResolve() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin");
        project.annotate(DOC, A1, CON2, "Eltroxin");
        project.annotate(DOC, A2, CONCEPT, "Eltroxin");
        project.annotate(DOC, A2, CON2, "Eltroxin");

        adjudicate();

        assertEquals(histogram("MATCHES_OK", "2", "MATCHES_DLETED", "2"), statusHistogram(),
                "both annotators agreed twice, so neither pair is a disagreement");
        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                annotationOf(A1, CONCEPT).adjudicationStatus);
        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                annotationOf(A1, CON2).adjudicationStatus);
    }

    @Test
    @DisplayName("Two agreed pairs on overlapping spans both resolve")
    void twoAgreedPairs_overlappingSpans_bothResolve() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin which");
        project.annotate(DOC, A1, CON2, "Eltroxin");
        project.annotate(DOC, A2, CONCEPT, "Eltroxin which");
        project.annotate(DOC, A2, CON2, "Eltroxin");

        adjudicate();

        assertEquals(histogram("MATCHES_OK", "2", "MATCHES_DLETED", "2"), statusHistogram(),
                "a wider agreed span overlapping a narrower one is not a disagreement");
    }

    // ================================================================== //
    // disagreements that must still be reported
    // ================================================================== //

    @Test
    @DisplayName("The same span under different classes is still a disagreement")
    void classDisagreement_isStillReported() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin");
        project.annotate(DOC, A2, CON2, "Eltroxin");

        adjudicate();

        assertEquals(histogram("NON_MATCHES", "2"), statusHistogram(),
                "the annotators disagreed about the class, which needs adjudication");
    }

    @Test
    @DisplayName("Annotations only one annotator made are still disagreements")
    void singleAnnotatorFindings_areStillReported() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin");
        project.annotate(DOC, A2, CONCEPT, "started");

        adjudicate();

        assertEquals(histogram("NON_MATCHES", "2"), statusHistogram(),
                "each annotator's unique finding needs adjudication");
    }

    @Test
    @DisplayName("An extra class only one annotator used stays a disagreement")
    void partialAgreement_reportsOnlyTheUnmatchedClass() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin");
        project.annotate(DOC, A1, CON2, "Eltroxin");
        project.annotate(DOC, A2, CONCEPT, "Eltroxin");

        adjudicate();

        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                annotationOf(A1, CONCEPT).adjudicationStatus,
                "the class both annotators used should resolve");
        assertEquals(Annotation.AdjudicationStatus.MATCHES_DLETED,
                annotationOf(A2, CONCEPT).adjudicationStatus);
        assertEquals(Annotation.AdjudicationStatus.NON_MATCHES,
                annotationOf(A1, CON2).adjudicationStatus,
                "the class only a1 used should still need adjudication");
    }

    // ================================================================== //
    // the reported end-to-end flow
    // ================================================================== //

    /**
     * The flow from the report: adjudicate a span carrying two agreed classes,
     * drag one span boundary wider, save. The file must hold just the two
     * accepted results — what the editor shows.
     */
    @Test
    @DisplayName("Adjusting a span boundary and saving writes only the accepted results")
    void spanBoundaryEditThenSave_writesOnlyAcceptedResults() throws Exception {
        project.annotate(DOC, A1, CONCEPT, "Eltroxin");
        project.annotate(DOC, A1, CON2, "Eltroxin");
        project.annotate(DOC, A2, CONCEPT, "Eltroxin");
        project.annotate(DOC, A2, CON2, "Eltroxin");

        adjudicate();

        Annotation concept = annotationOf(A1, CONCEPT);
        int start = concept.spanset.getSpanAt(0).start;
        concept.spanset.setOnlySpan(start, start + "Eltroxin which".length());
        concept.annotationText = "Eltroxin which";

        new Depot().articleInsurance(DOC);
        new OutputToXML().directsave(project.corpusFile(DOC));

        Element root = new SAXBuilder().build(project.adjudicationXml(DOC)).getRootElement();

        assertEquals(2, root.getChildren("annotation").size(),
                "both accepted results should be written");
        assertEquals(0, root.getChildren("adjudicating").size(),
                "the partners those results absorbed carry no decision to persist");

        List<String> spannedTexts = new ArrayList<String>();
        for (Object o : root.getChildren("annotation")) {
            spannedTexts.add(((Element) o).getChildText("spannedText"));
        }
        java.util.Collections.sort(spannedTexts);
        assertEquals(Arrays.asList("Eltroxin", "Eltroxin which"), spannedTexts,
                "the widened span should have been persisted");
    }
}
