package adjudication;

import adjudication.data.AdjudicationDepot;
import adjudication.parameters.Paras;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import report.iaaReport.AdjudicationLoader;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;
import testsupport.EhostProjectFixture;
import userInterface.GUI;
import userInterface.NavigationManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Switching between two copies of a project that share document filenames, when
 * both carry their own adjudication work.
 *
 * <p>{@link AdjudicationDepot} and {@link Paras} are process-wide static stores
 * keyed by nothing but the document filename — they hold no reference to the
 * project the data came from. Opening a project resets the regular {@link Depot}
 * (via {@code Reload.clearAnnotationDepot()}) but used to leave the adjudication
 * side untouched.
 *
 * <p>That mattered because {@code GUI.mode_continuePreviousAdjudicationWork()}
 * decides whether to read the current project's {@code adjudication/} folder
 * from disk by asking {@link AdjudicationDepot#isReady()}, which reports nothing
 * more than "the static vector is non-empty":
 *
 * <pre>
 *     if (!AdjudicationDepot.isReady()) {
 *         if (!AdjudicationLoader.loadWorkingState()) { ... }
 *     }
 * </pre>
 *
 * <p>So when project B's adjudication session had left the depot populated,
 * opening project A and choosing "continue your previous adjudication work"
 * skipped A's own on-disk state and resumed on B's — and because the two
 * projects are copies, every filename matched and nothing downstream noticed.
 *
 * <p>{@link NavigationManager#discardAdjudicationState()} now runs on both
 * project-switching boundaries, so the depot can only ever hold the open
 * project's work. These tests call that production method at exactly the points
 * the navigation code calls it.
 *
 * <p>The projects here are deliberately built as a copy pair, exactly as the
 * reported scenario describes: same corpus, same annotator annotations,
 * different adjudication progress.
 *
 * @see <a href="https://github.com/jianlins/ehost/issues/33">issue #33</a>
 */
public class CrossProjectAdjudicationDepotLeakTest {

    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final String ADJUDICATION = "ADJUDICATION";

    private static final String DOC = "note_001.txt";

    private static final String DOC_TEXT =
            "RECORD #001\n"
          + "CHIEF COMPLAINT: chest pain.\n"
          + "\n"
          + "HISTORY OF PRESENT ILLNESS:\n"
          + "The patient is a 62-year-old male with a history of hypertension.\n"
          + "He was started on aspirin earlier this year.\n";

    /** Both annotators marked all three findings, in both projects. */
    private static final String CHEST_PAIN = "chest pain";
    private static final String HYPERTENSION = "hypertension";
    private static final String ASPIRIN = "aspirin";

    private static final String SYMPTOM = "SYMPTOM";
    private static final String DIAGNOSIS = "DIAGNOSIS";
    private static final String DRUG = "DRUG";
    private static final String FINDING = "FINDING";

    private static final List<String> CLASSES =
            Arrays.asList(SYMPTOM, DIAGNOSIS, DRUG, FINDING);

    @TempDir
    Path tempDir;

    /** Adjudicated two of three findings. */
    private EhostProjectFixture projectA;

    /** A copy of A, adjudicated one of three — and one of them differently. */
    private EhostProjectFixture projectB;

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
    // the fixture: a project and its copy, adjudicated to different extents
    // ------------------------------------------------------------------ //

    /**
     * Builds {@code A/}, copies the whole directory to {@code B/} — so the two
     * share corpus text, filenames and annotator annotations — then gives each a
     * different {@code adjudication/} folder.
     *
     * <pre>
     *   finding        A's adjudication            B's adjudication
     *   -----------------------------------------------------------------
     *   chest pain     MATCHES_OK as SYMPTOM       MATCHES_OK as FINDING
     *   hypertension   MATCHES_OK as DIAGNOSIS     (not yet adjudicated)
     *   aspirin        (not yet adjudicated)       MATCHES_OK as DRUG
     * </pre>
     */
    private void buildProjects() {
        projectA = new EhostProjectFixture(new File(tempDir.toFile(), "A"));
        projectA.addDocument(DOC, DOC_TEXT);
        annotateBothAnnotators(projectA);
        projectA.writeSavedAnnotations();

        File bDir = new File(tempDir.toFile(), "B");
        EhostProjectFixture.copyDirectory(projectA.dir(), bDir);
        projectB = new EhostProjectFixture(bDir);
        projectB.adoptDocuments(projectA);

        // A: two of the three findings settled.
        writeAdjudication(projectA,
                new Settled(CHEST_PAIN, SYMPTOM),
                new Settled(HYPERTENSION, DIAGNOSIS));

        // B: one settled, and the shared finding settled under another class.
        writeAdjudication(projectB,
                new Settled(CHEST_PAIN, FINDING),
                new Settled(ASPIRIN, DRUG));
    }

    private void annotateBothAnnotators(EhostProjectFixture p) {
        for (String annotator : Arrays.asList(ALICE, BOB)) {
            p.annotate(DOC, annotator, SYMPTOM, CHEST_PAIN);
            p.annotate(DOC, annotator, DIAGNOSIS, HYPERTENSION);
            p.annotate(DOC, annotator, DRUG, ASPIRIN);
        }
    }

    /**
     * Writes an {@code adjudication/<doc>.knowtator.xml} in the shape
     * {@link resultEditor.save.OutputToXML} produces for settled findings: an
     * {@code <annotation>} attributed to ADJUDICATION carrying
     * {@code MATCHES_OK}.
     */
    private void writeAdjudication(EhostProjectFixture p, Settled... settled) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<annotations textSource=\"").append(DOC).append("\">\n");

        int n = 70;
        for (Settled s : settled) {
            int start = DOC_TEXT.indexOf(s.phrase);
            assertTrue(start >= 0, "fixture phrase must occur in " + DOC + ": " + s.phrase);
            String mentionId = "EHOST_Instance_" + (n++);

            xml.append("    <annotation>\n");
            xml.append("        <mention id=\"").append(mentionId).append("\" />\n");
            xml.append("        <annotator id=\"eHOST_2010\">").append(ADJUDICATION)
               .append("</annotator>\n");
            xml.append("        <span start=\"").append(start)
               .append("\" end=\"").append(start + s.phrase.length()).append("\" />\n");
            xml.append("        <spannedText>").append(s.phrase).append("</spannedText>\n");
            xml.append("        <creationDate>Sat Apr 19 00:02:18 MDT 2025</creationDate>\n");
            xml.append("        <processed>true</processed>\n");
            xml.append("        <AdjudicationStatus>MATCHES_OK</AdjudicationStatus>\n");
            xml.append("    </annotation>\n");
            xml.append("    <classMention id=\"").append(mentionId).append("\">\n");
            xml.append("        <mentionClass id=\"").append(s.annotationClass).append("\">")
               .append(s.phrase).append("</mentionClass>\n");
            xml.append("    </classMention>\n");
        }

        xml.append("</annotations>\n");

        File dir = p.adjudicationDir();
        assertTrue(dir.exists() || dir.mkdirs(), "failed to create " + dir);
        write(p.adjudicationXml(DOC), xml.toString());
    }

    private static void write(File file, String content) {
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        } catch (IOException ex) {
            throw new RuntimeException("failed to write " + file, ex);
        }
    }

    /** A finding the adjudicator has already settled. */
    private static final class Settled {
        final String phrase;
        final String annotationClass;

        Settled(String phrase, String annotationClass) {
            this.phrase = phrase;
            this.annotationClass = annotationClass;
        }
    }

    // ------------------------------------------------------------------ //
    // driving eHOST headlessly
    // ------------------------------------------------------------------ //

    /**
     * Opens a project, as {@code NavigationManager.selectProject()} does.
     *
     * <p>{@code selectProject} sets {@code CurrentProject}, auto-saves any
     * pending edits, calls {@link NavigationManager#discardAdjudicationState()},
     * and finally calls {@code Reload.load()}, whose first act is
     * {@code clearAnnotationDepot()} — {@code new Depot().clear()} — before
     * importing the project's own {@code saved/} folder.
     */
    private void openProject(EhostProjectFixture project) {
        env.Parameters.WorkSpace.CurrentProject = project.dir();
        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;

        NavigationManager.discardAdjudicationState();
        new Depot().clear();

        Vector<File> xmls = new Vector<File>(EhostProjectFixture.savedXmls(project.dir()));
        assertFalse(xmls.isEmpty(), "no saved/ XMLs under " + project.dir());
        new ImportAnnotation().XMLImporter(xmls);
    }

    /**
     * Leaves the open project, as {@code NavigationManager.goBackToProjectList()}
     * does.
     *
     * <p>Its full body prompts to save, releases the project lock, hides the
     * status buttons, switches the navigator tab and review mode, calls
     * {@link NavigationManager#discardAdjudicationState()}, saves the project
     * config and nulls {@code CurrentProject}. Only the last two of those and
     * the reset are depot-visible.
     */
    private void goBackToProjectList() {
        GUI.reviewmode = GUI.ReviewMode.OTHERS;
        NavigationManager.discardAdjudicationState();
        env.Parameters.WorkSpace.CurrentProject = null;
    }

    /**
     * Enters adjudication mode and answers "Yes, please" to the resume prompt.
     *
     * <p>Mirrors the {@code adjudicationMode} branch of
     * {@code ContentRenderer.setReviewMode()} followed by
     * {@code GUI.mode_continuePreviousAdjudicationWork()}. Every decision the
     * production code makes is made here by calling the same production
     * methods; only the Swing dialogs and the tree refresh are left out.
     */
    private void enterAdjudicationModeAndResume() throws Exception {
        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;

        // ContentRenderer: the resume prompt appears only when the *current*
        // project has adjudication files on disk.
        assertTrue(AdjudicationLoader.isAdjudicationAvailable(),
                "precondition: the open project must offer to resume");

        // GUI.mode_continuePreviousAdjudicationWork(), verbatim in structure.
        if (!Paras.isReadyForAdjudication()) {
            // production calls rebuildParasFromAnnotations(); seeded directly here
            seedParas();
        }
        if (!AdjudicationDepot.isReady()) {
            if (!AdjudicationLoader.loadWorkingState()) {
                new AdjudicationDepot().copyAnnotations(
                        Paras.getAnnotators(), Paras.getClasses(), true);
            }
        }

        // production then runs new Adjudication(this).checkAnnotations(false);
        // its depot step is copyAnnotations(..., false), which returns at once.
        new AdjudicationDepot().copyAnnotations(
                Paras.getAnnotators(), Paras.getClasses(), false);
    }

    private void seedParas() {
        Paras.removeAll();
        Paras.removeParas();
        Paras.setAnnotators(new ArrayList<String>(Arrays.asList(ALICE, BOB)));
        Paras.addAnnotator(ADJUDICATION);
        Paras.setClasses(new ArrayList<String>(CLASSES));
    }

    // ------------------------------------------------------------------ //
    // inspection
    // ------------------------------------------------------------------ //

    /** The settled findings the adjudication screen would show, as "phrase/CLASS". */
    private Set<String> settledFindings() {
        Article article = AdjudicationDepot.getArticleByFilename(DOC);
        assertNotNull(article, "no adjudication article for " + DOC);

        Set<String> found = new LinkedHashSet<String>();
        for (Annotation ann : article.annotations) {
            if (ann.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_OK) {
                found.add(ann.annotationText + "/" + ann.annotationclass);
            }
        }
        return found;
    }

    private Set<String> expectedForA() {
        return new LinkedHashSet<String>(Arrays.asList(
                CHEST_PAIN + "/" + SYMPTOM,
                HYPERTENSION + "/" + DIAGNOSIS));
    }

    private Set<String> expectedForB() {
        return new LinkedHashSet<String>(Arrays.asList(
                CHEST_PAIN + "/" + FINDING,
                ASPIRIN + "/" + DRUG));
    }

    // ------------------------------------------------------------------ //
    // tests
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("Fixture: each project offers its own adjudication state on disk")
    void eachProjectHasItsOwnAdjudicationOnDisk() throws Exception {
        openProject(projectA);
        enterAdjudicationModeAndResume();
        assertEquals(expectedForA(), settledFindings(),
                "project A's own adjudication state must load when opened first");

        resetGlobalState();

        openProject(projectB);
        enterAdjudicationModeAndResume();
        assertEquals(expectedForB(), settledFindings(),
                "project B's own adjudication state must load when opened first");
    }

    @Test
    @DisplayName("Opening B, returning to the project list, then resuming A shows A's adjudication")
    void resumingAfterAnotherProjectShowsTheOpenProjectsAdjudication() throws Exception {
        // 1. the user works on B
        openProject(projectB);
        enterAdjudicationModeAndResume();
        assertEquals(expectedForB(), settledFindings(),
                "precondition: B resumes on its own state");

        // 2. "Go back to project list ..."
        goBackToProjectList();

        // 3. the user opens A instead
        openProject(projectA);

        // The regular Depot was rebuilt from A's saved/ folder, so the
        // annotator layer is correct...
        assertEquals(6, new Depot().getArticleByFilename(DOC).annotations.size(),
                "A's own annotator annotations must be reloaded");

        // 4. ...and A's own adjudication work is still on disk, untouched.
        env.Parameters.WorkSpace.CurrentProject = projectA.dir();
        assertTrue(AdjudicationLoader.isAdjudicationAvailable(),
                "A's adjudication/ folder must still be there");

        // 5. the user resumes adjudication on A
        enterAdjudicationModeAndResume();

        assertEquals(expectedForA(), settledFindings(),
                "resuming adjudication on A must show A's adjudication work, "
                        + "not the state left behind by B");
    }

    @Test
    @DisplayName("Leaving a project discards its adjudication state")
    void leavingAProjectDiscardsItsAdjudicationState() throws Exception {
        openProject(projectB);
        enterAdjudicationModeAndResume();
        assertTrue(AdjudicationDepot.isReady(), "precondition: B's state is loaded");

        goBackToProjectList();

        assertFalse(AdjudicationDepot.isReady(),
                "no adjudication state may outlive the project it belongs to");
        assertFalse(Paras.isReadyForAdjudication(),
                "the adjudication parameters are project-scoped too");
    }

    @Test
    @DisplayName("Switching projects directly, without the project list, also discards it")
    void switchingProjectsDirectlyDiscardsTheState() throws Exception {
        // the REST API switches project without ever passing through
        // goBackToProjectList(), so selectProject() has to reset as well
        openProject(projectB);
        enterAdjudicationModeAndResume();
        assertEquals(expectedForB(), settledFindings(), "precondition: B resumes on its own state");

        openProject(projectA);
        enterAdjudicationModeAndResume();

        assertEquals(expectedForA(), settledFindings(),
                "a direct project switch must resume on the newly opened project's work");
    }
}
