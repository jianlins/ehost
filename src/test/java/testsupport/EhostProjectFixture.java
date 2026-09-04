package testsupport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a real eHOST project directory on disk so adjudication can be driven
 * end-to-end without the GUI.
 *
 * <p>The layout mirrors what {@code workSpace.NewProject} creates:
 * <pre>
 *   &lt;project&gt;/
 *       config/
 *       corpus/&lt;doc&gt;.txt
 *       saved/&lt;doc&gt;.txt.knowtator.xml
 *       adjudication/            (created by eHOST on the first adjudication save)
 * </pre>
 *
 * <p>Spans are never hard-coded. {@link #annotate} locates the phrase in the
 * document text, so every {@code <span>} written here is guaranteed to agree
 * with the {@code <spannedText>} beside it — the same invariant real eHOST
 * output has, and the thing that makes overlap comparison meaningful.
 */
public class EhostProjectFixture {

    /** eHOST writes this as the {@code id} attribute of {@code <annotator>}. */
    private static final String ANNOTATOR_ID = "eHOST_2010";

    private static final String CREATION_DATE = "Mon Jan 01 00:00:00 MST 2024";

    private final File projectDir;

    /** Document text, keyed by corpus filename, so spans can be resolved. */
    private final Map<String, String> documents = new LinkedHashMap<String, String>();

    /** Pending annotations per document, in insertion order. */
    private final Map<String, List<Ann>> pending = new LinkedHashMap<String, List<Ann>>();

    public EhostProjectFixture(File projectDir) {
        this.projectDir = projectDir;
        mkdirs(new File(projectDir, "config"));
        mkdirs(new File(projectDir, "corpus"));
        mkdirs(new File(projectDir, "saved"));
    }

    public File dir() {
        return projectDir;
    }

    public File corpusDir() {
        return new File(projectDir, "corpus");
    }

    public File savedDir() {
        return new File(projectDir, "saved");
    }

    public File adjudicationDir() {
        return new File(projectDir, "adjudication");
    }

    public File corpusFile(String docFilename) {
        return new File(corpusDir(), docFilename);
    }

    public File savedXml(String docFilename) {
        return new File(savedDir(), docFilename + ".knowtator.xml");
    }

    public File adjudicationXml(String docFilename) {
        return new File(adjudicationDir(), docFilename + ".knowtator.xml");
    }

    public String textOf(String docFilename) {
        return documents.get(docFilename);
    }

    public List<String> documentNames() {
        return new ArrayList<String>(documents.keySet());
    }

    // ------------------------------------------------------------------ //
    // authoring
    // ------------------------------------------------------------------ //

    /** Adds a corpus document and writes it to {@code corpus/}. */
    public EhostProjectFixture addDocument(String filename, String text) {
        documents.put(filename, text);
        pending.put(filename, new ArrayList<Ann>());
        write(corpusFile(filename), text);
        return this;
    }

    /** Queues an annotation covering the first occurrence of {@code phrase}. */
    public EhostProjectFixture annotate(String docFilename, String annotator,
            String annotationClass, String phrase) {
        return annotate(docFilename, annotator, annotationClass, phrase, 1);
    }

    /**
     * Queues an annotation covering the {@code occurrence}-th (1-based)
     * occurrence of {@code phrase} in the document.
     *
     * @throws IllegalArgumentException if the phrase does not occur that often,
     *         which would otherwise silently produce a fixture whose spans do
     *         not match its text
     */
    public EhostProjectFixture annotate(String docFilename, String annotator,
            String annotationClass, String phrase, int occurrence) {
        String text = documents.get(docFilename);
        if (text == null) {
            throw new IllegalArgumentException("unknown document: " + docFilename);
        }
        int start = nthIndexOf(text, phrase, occurrence);
        if (start < 0) {
            throw new IllegalArgumentException("occurrence " + occurrence + " of \""
                    + phrase + "\" not found in " + docFilename);
        }
        pending.get(docFilename).add(
                new Ann(annotator, annotationClass, phrase, start, start + phrase.length()));
        return this;
    }

    /** Writes every queued annotation out as {@code saved/*.knowtator.xml}. */
    public EhostProjectFixture writeSavedAnnotations() {
        for (Map.Entry<String, List<Ann>> entry : pending.entrySet()) {
            write(savedXml(entry.getKey()), savedXmlFor(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    /**
     * Reconstructs this fixture's view of a project that already exists on disk
     * — used after {@link #copyDirectory} to re-annotate the copy.
     */
    public EhostProjectFixture adoptDocuments(EhostProjectFixture source) {
        for (String name : source.documentNames()) {
            documents.put(name, source.textOf(name));
            pending.put(name, new ArrayList<Ann>());
        }
        return this;
    }

    // ------------------------------------------------------------------ //
    // XML
    // ------------------------------------------------------------------ //

    private String savedXmlFor(String docFilename, List<Ann> anns) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<annotations textSource=\"").append(docFilename).append("\">\n");

        int n = 0;
        for (Ann ann : anns) {
            n++;
            String mentionId = "EHOST_Instance_" + n;
            xml.append("    <annotation>\n");
            xml.append("        <mention id=\"").append(mentionId).append("\" />\n");
            xml.append("        <annotator id=\"").append(ANNOTATOR_ID).append("\">")
               .append(ann.annotator).append("</annotator>\n");
            xml.append("        <span start=\"").append(ann.start)
               .append("\" end=\"").append(ann.end).append("\" />\n");
            xml.append("        <spannedText>").append(escape(ann.phrase))
               .append("</spannedText>\n");
            xml.append("        <creationDate>").append(CREATION_DATE).append("</creationDate>\n");
            xml.append("    </annotation>\n");
            xml.append("    <classMention id=\"").append(mentionId).append("\">\n");
            xml.append("        <mentionClass id=\"").append(ann.annotationClass).append("\">")
               .append(escape(ann.phrase)).append("</mentionClass>\n");
            xml.append("    </classMention>\n");
        }

        xml.append("</annotations>\n");
        return xml.toString();
    }

    // ------------------------------------------------------------------ //
    // helpers
    // ------------------------------------------------------------------ //

    /** Recursively copies a whole project directory, as a second annotator would receive it. */
    public static void copyDirectory(File source, File target) {
        final Path sourceRoot = source.toPath();
        final Path targetRoot = target.toPath();
        try (java.util.stream.Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = targetRoot.resolve(sourceRoot.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("failed to copy project " + source, ex);
        }
    }

    /** Every {@code saved/*.knowtator.xml} of the given project, in stable order. */
    public static List<File> savedXmls(File projectDir) {
        List<File> found = new ArrayList<File>();
        File[] files = new File(projectDir, "saved").listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".knowtator.xml")) {
                    found.add(f);
                }
            }
        }
        java.util.Collections.sort(found);
        return found;
    }

    private static int nthIndexOf(String text, String phrase, int occurrence) {
        int index = -1;
        for (int i = 0; i < occurrence; i++) {
            index = text.indexOf(phrase, index + 1);
            if (index < 0) {
                return -1;
            }
        }
        return index;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void mkdirs(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("failed to create " + dir);
        }
    }

    private static void write(File file, String content) {
        try {
            mkdirs(file.getParentFile());
            try (FileWriter w = new FileWriter(file)) {
                w.write(content);
            }
        } catch (IOException ex) {
            throw new RuntimeException("failed to write " + file, ex);
        }
    }

    public static String read(File file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("failed to read " + file, ex);
        }
    }

    /** A single queued annotation. */
    private static final class Ann {
        final String annotator;
        final String annotationClass;
        final String phrase;
        final int start;
        final int end;

        Ann(String annotator, String annotationClass, String phrase, int start, int end) {
            this.annotator = annotator;
            this.annotationClass = annotationClass;
            this.phrase = phrase;
            this.start = start;
            this.end = end;
        }
    }
}
