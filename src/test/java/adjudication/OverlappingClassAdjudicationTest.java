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
import report.iaaReport.AdjudicationLoader;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;
import resultEditor.save.OutputToXML;
import testsupport.EhostProjectFixture;
import userInterface.GUI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
 * Covers the adjudication file layout that a manual GUI run produced and that
 * looked like a duplicate: two {@code <annotation>} and two
 * {@code <adjudicating>} elements in one document, while the editor showed only
 * two annotations.
 *
 * <p>It arises whenever both annotators tag the <em>same span</em> under
 * <em>two different classes</em>. Adjudication then carries four annotations —
 * two accepted results and the two partners they absorbed — and the two writers
 * in {@link OutputToXML} split them on complementary conditions:
 *
 * <pre>
 *   &lt;annotation&gt;    status == MATCHES_OK || annotator == ADJUDICATION
 *   &lt;adjudicating&gt;  everything else
 * </pre>
 *
 * <p>Four annotations therefore must yield exactly four elements. The editor
 * paints only two because {@code GUI.reloadAnnotationsToScreen} skips
 * {@code *_DLETED} in adjudication mode. These tests hold that invariant and
 * prove the layout round-trips without growing.
 *
 * <p>The fixture is built from scratch in a temporary directory rather than
 * read from {@code src/test/resources}, so no shared fixture that other tests
 * pin their expectations to is involved.
 */
public class OverlappingClassAdjudicationTest {

    private static final String A1 = "a1";
    private static final String A2 = "a2";
    private static final String ADJUDICATION = "ADJUDICATION";

    private static final String DOC = "doc3.txt";

    /** The phrase both annotators tagged, and the wider span one was edited to. */
    private static final String SHORT_PHRASE = "Eltroxin";
    private static final String EDITED_PHRASE = "Eltroxin which";

    private static final String DOC_TEXT =
            "RECORD #003\n"
          + "MEDICATIONS:\n"
          + "The patient continues on Eltroxin which was started last year for hypothyroidism.\n"
          + "No adverse reactions have been reported since the last visit.\n";

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
        project.addDocument(DOC, DOC_TEXT);

        // Both annotators tagged the same span, once per class.
        project.annotate(DOC, A1, CONCEPT, SHORT_PHRASE);
        project.annotate(DOC, A1, CON2, SHORT_PHRASE);
        project.annotate(DOC, A2, CONCEPT, SHORT_PHRASE);
        project.annotate(DOC, A2, CON2, SHORT_PHRASE);
        project.writeSavedAnnotations();

        writeAdjudicationState();
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
    // the fixture: a stored adjudication session in the reported shape
    // ------------------------------------------------------------------ //

    /**
     * Writes the adjudication working state left by a session in which the
     * adjudicator widened the CONCEPT span and accepted both classes: two
     * finals attributed to ADJUDICATION, and the two partners they absorbed.
     */
    private void writeAdjudicationState() {
        int shortStart = DOC_TEXT.indexOf(SHORT_PHRASE);
        int editedStart = DOC_TEXT.indexOf(EDITED_PHRASE);
        assertTrue(shortStart >= 0 && editedStart >= 0, "fixture phrases must occur in " + DOC);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<annotations textSource=\"").append(DOC).append("\">\n");

        // the accepted results
        appendEntry(xml, "annotation", "EHOST_Instance_55", ADJUDICATION,
                editedStart, editedStart + EDITED_PHRASE.length(), EDITED_PHRASE,
                CONCEPT, "MATCHES_OK");
        appendEntry(xml, "annotation", "EHOST_Instance_56", ADJUDICATION,
                shortStart, shortStart + SHORT_PHRASE.length(), SHORT_PHRASE,
                CON2, "MATCHES_OK");
        // the partners those results absorbed
        appendEntry(xml, "adjudicating", "EHOST_Instance_57", A2,
                shortStart, shortStart + SHORT_PHRASE.length(), SHORT_PHRASE,
                CONCEPT, "MATCHES_DLETED");
        appendEntry(xml, "adjudicating", "EHOST_Instance_58", A2,
                shortStart, shortStart + SHORT_PHRASE.length(), SHORT_PHRASE,
                CON2, "MATCHES_DLETED");

        xml.append("</annotations>\n");

        File dir = project.adjudicationDir();
        assertTrue(dir.exists() || dir.mkdirs(), "failed to create " + dir);
        write(adjudicationXml(), xml.toString());
    }

    private static void appendEntry(StringBuilder xml, String tag, String mentionId,
            String annotator, int start, int end, String text,
            String annClass, String status) {
        xml.append("    <").append(tag).append(">\n");
        xml.append("        <mention id=\"").append(mentionId).append("\" />\n");
        xml.append("        <annotator id=\"eHOST_2010\">").append(annotator).append("</annotator>\n");
        xml.append("        <span start=\"").append(start).append("\" end=\"").append(end).append("\" />\n");
        xml.append("        <spannedText>").append(text).append("</spannedText>\n");
        xml.append("        <creationDate>Sat Apr 19 00:02:18 MDT 2025</creationDate>\n");
        xml.append("        <processed>true</processed>\n");
        xml.append("        <AdjudicationStatus>").append(status).append("</AdjudicationStatus>\n");
        xml.append("    </").append(tag).append(">\n");
        xml.append("    <classMention id=\"").append(mentionId).append("\">\n");
        xml.append("        <mentionClass id=\"").append(annClass).append("\">")
           .append(text).append("</mentionClass>\n");
        xml.append("    </classMention>\n");
    }

    private static void write(File file, String content) {
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        } catch (IOException ex) {
            throw new RuntimeException("failed to write " + file, ex);
        }
    }

    private File adjudicationXml() {
        return project.adjudicationXml(DOC);
    }

    // ------------------------------------------------------------------ //
    // driving eHOST headlessly
    // ------------------------------------------------------------------ //

    /** Opens the project and resumes the stored adjudication session. */
    private void openAndResume() {
        env.Parameters.WorkSpace.CurrentProject = project.dir();

        new Depot().clear();
        AdjudicationDepot.clear();

        GUI.reviewmode = GUI.ReviewMode.ANNOTATION_MODE;
        Vector<File> xmls = new Vector<File>(EhostProjectFixture.savedXmls(project.dir()));
        assertFalse(xmls.isEmpty(), "no saved/ XMLs under " + project.dir());
        new ImportAnnotation().XMLImporter(xmls);

        GUI.reviewmode = GUI.ReviewMode.adjudicationMode;
        assertTrue(AdjudicationLoader.loadWorkingState(),
                "the stored adjudication session failed to load");
    }

    private void save() {
        new Depot().articleInsurance(DOC);
        new OutputToXML().directsave(project.corpusFile(DOC));
    }

    // ------------------------------------------------------------------ //
    // working-set inspection
    // ------------------------------------------------------------------ //

    private Article article() {
        Article article = AdjudicationDepot.getArticleByFilename(DOC);
        assertNotNull(article, "no adjudication article for " + DOC);
        return article;
    }

    private static boolean isTombstone(Annotation ann) {
        return ann.adjudicationStatus == Annotation.AdjudicationStatus.MATCHES_DLETED
                || ann.adjudicationStatus == Annotation.AdjudicationStatus.NONMATCHES_DLETED;
    }

    /** What the editor actually paints: everything except the tombstones. */
    private List<Annotation> visibleInEditor() {
        List<Annotation> visible = new ArrayList<Annotation>();
        for (Annotation ann : article().annotations) {
            if (!isTombstone(ann)) {
                visible.add(ann);
            }
        }
        return visible;
    }

    private Annotation visibleOfClass(String annClass) {
        for (Annotation ann : visibleInEditor()) {
            if (annClass.equals(ann.annotationclass)) {
                return ann;
            }
        }
        throw new AssertionError("no visible " + annClass + " annotation in " + DOC);
    }

    // ------------------------------------------------------------------ //
    // XML inspection
    // ------------------------------------------------------------------ //

    private Element root() throws Exception {
        File xml = adjudicationXml();
        assertTrue(xml.isFile(), "no adjudication XML for " + DOC);
        return new SAXBuilder().build(xml).getRootElement();
    }

    private Map<String, String> classByMentionId(Element root) {
        Map<String, String> classes = new HashMap<String, String>();
        for (Object o : root.getChildren("classMention")) {
            Element cm = (Element) o;
            Element mc = cm.getChild("mentionClass");
            if (mc != null) {
                classes.put(cm.getAttributeValue("id"), mc.getAttributeValue("id"));
            }
        }
        return classes;
    }

    /** (span, text, class, status) -> count, across both element types. */
    private Map<String, Integer> xmlMultiset() throws Exception {
        Element root = root();
        Map<String, String> classes = classByMentionId(root);
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String tag : Arrays.asList("annotation", "adjudicating")) {
            for (Object o : root.getChildren(tag)) {
                Element e = (Element) o;
                Element span = e.getChild("span");
                String key = span.getAttributeValue("start") + "-" + span.getAttributeValue("end")
                        + "|" + e.getChildText("spannedText")
                        + "|" + classes.get(mentionId(e))
                        + "|" + e.getChildText("AdjudicationStatus");
                Integer prev = counts.get(key);
                counts.put(key, prev == null ? 1 : prev + 1);
            }
        }
        return counts;
    }

    /** The same multiset taken from the in-memory working set. */
    private Map<String, Integer> memoryMultiset() {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Annotation ann : article().annotations) {
            String key = ann.spanset.getSpanAt(0).start + "-" + ann.spanset.getSpanAt(0).end
                    + "|" + ann.annotationText + "|" + ann.annotationclass
                    + "|" + ann.adjudicationStatus;
            Integer prev = counts.get(key);
            counts.put(key, prev == null ? 1 : prev + 1);
        }
        return counts;
    }

    private int totalEntries() throws Exception {
        Element root = root();
        return root.getChildren("annotation").size() + root.getChildren("adjudicating").size();
    }

    private static String mentionId(Element annotationElement) {
        return annotationElement.getChild("mention").getAttributeValue("id");
    }

    // ================================================================== //
    // tests
    // ================================================================== //

    @Test
    @DisplayName("Two <annotation> plus two <adjudicating> is four annotations, not a duplicate")
    void fourElements_areFourDistinctAnnotations() throws Exception {
        openAndResume();

        assertEquals(4, article().annotations.size(),
                "the working set should hold all four annotations");
        for (Map.Entry<String, Integer> entry : memoryMultiset().entrySet()) {
            assertEquals(1, entry.getValue().intValue(),
                    "the working set holds a genuine duplicate: " + entry.getKey());
        }

        Element root = root();
        Map<String, String> classes = classByMentionId(root);

        List<String> finalClasses = new ArrayList<String>();
        for (Object o : root.getChildren("annotation")) {
            Element e = (Element) o;
            assertEquals(ADJUDICATION, e.getChildText("annotator"),
                    "a final result is attributed to ADJUDICATION");
            assertEquals("MATCHES_OK", e.getChildText("AdjudicationStatus"));
            finalClasses.add(classes.get(mentionId(e)));
        }
        assertEquals(Arrays.asList(CONCEPT, CON2), finalClasses,
                "the two finals should be the two different classes");

        List<String> tombstoneClasses = new ArrayList<String>();
        for (Object o : root.getChildren("adjudicating")) {
            Element e = (Element) o;
            assertEquals("MATCHES_DLETED", e.getChildText("AdjudicationStatus"));
            assertNotEquals(ADJUDICATION, e.getChildText("annotator"),
                    "a tombstone keeps its real author");
            tombstoneClasses.add(classes.get(mentionId(e)));
        }
        assertEquals(Arrays.asList(CONCEPT, CON2), tombstoneClasses,
                "one absorbed partner per class");
    }

    @Test
    @DisplayName("The editor shows only the two finals, though the file holds four entries")
    void editorShowsTwo_whileFileHoldsFour() throws Exception {
        openAndResume();

        assertEquals(4, article().annotations.size());
        assertEquals(4, totalEntries());
        assertEquals(2, visibleInEditor().size(),
                "the editor should paint exactly the two accepted annotations");
        assertEquals(EDITED_PHRASE, visibleOfClass(CONCEPT).annotationText,
                "the edited CONCEPT span should be the visible one");
        assertEquals(SHORT_PHRASE, visibleOfClass(CON2).annotationText);
    }

    @Test
    @DisplayName("The re-saved file mirrors the working set exactly")
    void savedFileMirrorsMemory() throws Exception {
        openAndResume();
        save();
        assertEquals(memoryMultiset(), xmlMultiset(),
                "the adjudication XML is not an exact image of the working set");
    }

    @Test
    @DisplayName("Resuming and re-saving this layout changes nothing")
    void layoutIsStableAcrossResume() throws Exception {
        openAndResume();
        save();

        Map<String, Integer> baseline = xmlMultiset();
        assertEquals(4, totalEntries(), "the first re-save changed the entry count");

        for (int cycle = 1; cycle <= 3; cycle++) {
            openAndResume();

            assertEquals(4, article().annotations.size(),
                    "resume on cycle " + cycle + " changed the working set size");
            assertEquals(2, visibleInEditor().size(),
                    "resume on cycle " + cycle + " changed what the editor shows");

            save();

            assertEquals(baseline, xmlMultiset(),
                    "cycle " + cycle + " changed the adjudication file");
            assertEquals(2, root().getChildren("annotation").size(),
                    "cycle " + cycle + " changed the number of finals");
            assertEquals(2, root().getChildren("adjudicating").size(),
                    "cycle " + cycle + " changed the number of tombstones");
        }
    }

    @Test
    @DisplayName("Editing an overlapping annotation again still writes four entries")
    void furtherEdit_doesNotGrowTheFile() throws Exception {
        openAndResume();

        Annotation con2 = visibleOfClass(CON2);
        int start = con2.spanset.getSpanAt(0).start;
        con2.spanset.setOnlySpan(start, start + EDITED_PHRASE.length());
        con2.annotationText = EDITED_PHRASE;

        save();

        assertEquals(4, totalEntries(), "editing a span changed the entry count");
        assertEquals(2, root().getChildren("annotation").size());
        assertEquals(2, root().getChildren("adjudicating").size());
        assertEquals(memoryMultiset(), xmlMultiset());

        openAndResume();
        assertEquals(EDITED_PHRASE, visibleOfClass(CON2).annotationText,
                "the second edit was not persisted");
        assertEquals(4, article().annotations.size());
    }

    /**
     * Guards the legacy healer's twin key against a false positive that this
     * document's shape makes possible.
     *
     * <p>The healer drops a status-less {@code <annotation>} when an
     * {@code <adjudicating>} twin exists, because pre-fix builds wrote one
     * annotation as both. Its identity key was span + text + annotator +
     * {@code creationDate}, none of which separates two annotations that share
     * a span and differ only by class — and {@code creationDate} has only
     * one-second resolution, so tagging one span twice in quick succession
     * produces exactly that collision.
     *
     * <p>The file below is degraded to that worst case: a genuine legacy final
     * of class CONCEPT with no twin at all, beside an unrelated CON2 tombstone
     * matching it on every other field. The final must survive and default to
     * {@code MATCHES_OK}; keying without the class silently drops it.
     */
    @Test
    @DisplayName("A legacy final is not dropped by a same-span tombstone of another class")
    void legacyHealer_keepsDifferentClassesOnTheSameSpan() throws Exception {
        File xml = adjudicationXml();
        org.jdom.Document doc = new SAXBuilder().build(xml);
        Element root = doc.getRootElement();
        Map<String, String> classes = classByMentionId(root);

        List<Element> discard = new ArrayList<Element>();
        for (Object o : root.getChildren("annotation")) {
            Element e = (Element) o;
            if (CON2.equals(classes.get(mentionId(e)))) {
                discard.add(e);
            } else {
                // a legacy final: no status was recorded by the old writer
                e.removeChild("AdjudicationStatus");
                makeIndistinguishable(e);
            }
        }
        for (Object o : root.getChildren("adjudicating")) {
            Element e = (Element) o;
            if (CONCEPT.equals(classes.get(mentionId(e)))) {
                discard.add(e);
            } else {
                // an unrelated tombstone colliding on every field but class
                makeIndistinguishable(e);
            }
        }
        for (Element e : discard) {
            root.removeContent(e);
        }
        try (FileWriter w = new FileWriter(xml)) {
            new org.jdom.output.XMLOutputter().output(doc, w);
        }

        openAndResume();

        assertEquals(2, article().annotations.size(),
                "the healer dropped a legacy final that merely shared a span"
                        + " with a tombstone of another class");
        assertEquals(Annotation.AdjudicationStatus.MATCHES_OK,
                visibleOfClass(CONCEPT).adjudicationStatus,
                "a status-less legacy final should default to MATCHES_OK");
    }

    /** Aligns every field the twin key uses, leaving only the class to differ. */
    private void makeIndistinguishable(Element e) {
        int start = DOC_TEXT.indexOf(SHORT_PHRASE);
        e.getChild("span").setAttribute("start", String.valueOf(start));
        e.getChild("span").setAttribute("end", String.valueOf(start + SHORT_PHRASE.length()));
        e.getChild("spannedText").setText(SHORT_PHRASE);
        e.getChild("annotator").setText(ADJUDICATION);
        e.getChild("creationDate").setText("Sat Apr 19 00:02:18 MDT 2025");
    }
}
