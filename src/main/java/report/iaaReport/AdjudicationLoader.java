package report.iaaReport;

import imports.ImportXML;
import imports.importedXML.eXMLFile;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Vector;
import java.util.logging.Level;

/**
 * Loads adjudication annotations from the project's adjudication/ folder
 * and adds them to the Depot with annotator name "Adjudication" so they
 * can be compared against human annotators in IAA reports.
 */
public class AdjudicationLoader {

    public static final String ADJUDICATION_ANNOTATOR_NAME = "Adjudication";

    private static final ArrayList<Annotation> loadedAnnotations = new ArrayList<Annotation>();

    /**
     * Checks if adjudication annotations exist and loads them into the Depot.
     *
     * @return true if adjudication annotations were found and loaded
     */
    public static boolean load() {
        loadedAnnotations.clear();

        File adjudicationDir = getAdjudicationDir();
        if (adjudicationDir == null || !adjudicationDir.exists() || !adjudicationDir.isDirectory()) {
            log.LoggingToFile.log(Level.INFO, "No adjudication folder found under current project.");
            return false;
        }

        Vector<File> xmlFiles = listKnowtatorXMLs(adjudicationDir);
        if (xmlFiles == null || xmlFiles.isEmpty()) {
            log.LoggingToFile.log(Level.INFO, "No .knowtator.xml files found in adjudication folder.");
            return false;
        }

        log.LoggingToFile.log(Level.INFO, "Found " + xmlFiles.size()
                + " adjudication XML file(s). Loading...");

        ImportAnnotation importer = new ImportAnnotation();
        Depot depot = new Depot();

        for (File xmlFile : xmlFiles) {
            try {
                eXMLFile parsedXml = ImportXML.readXMLContents(xmlFile);
                if (parsedXml == null) {
                    continue;
                }
                parsedXml = importer.assignateAnnotationIndex(parsedXml);

                // Remove adjudicating elements (type=5) before extraction.
                // These are duplicates of <annotation> elements created either
                // by the backward compatibility fix or by <adjudicating> XML
                // elements that mirror their <annotation> counterparts.
                for (int k = parsedXml.annotations.size() - 1; k >= 0; k--) {
                    if (parsedXml.annotations.get(k).type == 5) {
                        parsedXml.annotations.remove(k);
                    }
                }

                // Extract annotations without adding to Depot
                Article article = importer.XMLExtractor(parsedXml, false);
                if (article == null || article.annotations == null || article.annotations.isEmpty()) {
                    continue;
                }

                String textFilename = article.filename;

                // Ensure article exists in the Depot
                depot.articleInsurance(textFilename);
                Article depotArticle = depot.getArticleByFilename(textFilename);
                if (depotArticle == null) {
                    continue;
                }

                // Add each annotation with annotator set to "Adjudication"
                for (Annotation ann : article.annotations) {
                    ann.setAnnotator(ADJUDICATION_ANNOTATOR_NAME);
                    depotArticle.annotations.add(ann);
                    loadedAnnotations.add(ann);
                }

                log.LoggingToFile.log(Level.INFO, "Loaded " + article.annotations.size()
                        + " adjudication annotations from " + xmlFile.getName());

            } catch (Exception ex) {
                log.LoggingToFile.log(Level.WARNING,
                        "Failed to load adjudication XML: " + xmlFile.getName()
                                + " - " + ex.getMessage());
            }
        }

        log.LoggingToFile.log(Level.INFO, "Total adjudication annotations loaded: "
                + loadedAnnotations.size());
        return !loadedAnnotations.isEmpty();
    }

    /**
     * Removes all adjudication annotations that were loaded into the Depot
     * so that normal operation is not affected after report generation.
     */
    public static void cleanup() {
        Depot depot = new Depot();
        ArrayList<Article> articles = depot.getAllArticles();
        if (articles != null) {
            // Identity, not Annotation.equals(): value equality would also drop
            // annotations this loader never added.
            IdentityHashMap<Annotation, Boolean> added =
                    new IdentityHashMap<Annotation, Boolean>();
            for (Annotation ann : loadedAnnotations) {
                added.put(ann, Boolean.TRUE);
            }
            for (Article article : articles) {
                if (article == null || article.annotations == null) {
                    continue;
                }
                // Removed in place so any other holder of this Vector sees it.
                for (int i = article.annotations.size() - 1; i >= 0; i--) {
                    if (added.containsKey(article.annotations.get(i))) {
                        article.annotations.remove(i);
                    }
                }
            }
        }
        loadedAnnotations.clear();
        log.LoggingToFile.log(Level.INFO, "Adjudication annotations cleaned up from Depot.");
    }

    /**
     * Returns the members of {@code candidates} that are not the very same
     * object as any member of {@code exclude}.
     *
     * <p>{@code Annotation} overrides {@code equals()} with value semantics, so
     * {@code List.removeAll} would discard distinct annotations that merely look
     * alike. The caller compares two snapshots of the same object graph, for
     * which reference identity is the correct — and unambiguous — test.
     */
    private static Vector<Annotation> removeByIdentity(
            Vector<Annotation> candidates, Collection<Annotation> exclude) {
        IdentityHashMap<Annotation, Boolean> excluded =
                new IdentityHashMap<Annotation, Boolean>();
        for (Annotation ann : exclude) {
            excluded.put(ann, Boolean.TRUE);
        }
        Vector<Annotation> kept = new Vector<Annotation>();
        for (Annotation ann : candidates) {
            if (!excluded.containsKey(ann)) {
                kept.add(ann);
            }
        }
        return kept;
    }

    /**
     * Returns true if adjudication data is available in the project.
     */
    public static boolean isAdjudicationAvailable() {
        File adjudicationDir = getAdjudicationDir();
        if (adjudicationDir == null || !adjudicationDir.exists() || !adjudicationDir.isDirectory()) {
            return false;
        }
        Vector<File> xmlFiles = listKnowtatorXMLs(adjudicationDir);
        return xmlFiles != null && !xmlFiles.isEmpty();
    }

    /**
     * Loads adjudication working state from the adjudication/ folder.
     *
     * <p>Those XMLs hold two element types, and after the disjoint-writer fix
     * each annotation appears as exactly one of them: {@code <adjudicating>}
     * (type=5, in-progress working state) and {@code <annotation>} (the final
     * adjudicated result). Both carry an {@code <AdjudicationStatus>}, and both
     * end up in AdjudicationDepot so the session can resume exactly as it was
     * left.
     *
     * @return true if any adjudication working state was loaded
     */
    public static boolean loadWorkingState() {
        File adjudicationDir = getAdjudicationDir();
        if (adjudicationDir == null || !adjudicationDir.exists() || !adjudicationDir.isDirectory()) {
            return false;
        }

        Vector<File> xmlFiles = listKnowtatorXMLs(adjudicationDir);
        if (xmlFiles == null || xmlFiles.isEmpty()) {
            return false;
        }

        ImportAnnotation importer = new ImportAnnotation();
        Depot depot = new Depot();

        for (File xmlFile : xmlFiles) {
            try {
                eXMLFile parsedXml = ImportXML.readXMLContents(xmlFile);
                if (parsedXml == null) {
                    continue;
                }
                parsedXml = importer.assignateAnnotationIndex(parsedXml);

                // Legacy compatibility: adjudication XMLs written before
                // <AdjudicationStatus> existed carried only final results, so a
                // missing status means "agreed match". "NOBODY" is the sentinel
                // ImportXML uses when the element is absent.
                for (imports.importedXML.eAnnotationNode node : parsedXml.annotations) {
                    if (node != null && node.type != 5
                            && node.__adjudication_status != null
                            && "NOBODY".equals(node.__adjudication_status.trim())) {
                        node.__adjudication_status = "MATCHES_OK";
                    }
                }

                // Derive text filename (mirrors ImportAnnotation.getXMLTextSource)
                String textFilename = parsedXml.filename.trim()
                        .replaceAll("\\.knowtator\\.xml", " ").trim();

                // Snapshot current Depot annotations before import so we can
                // restore them afterwards — XMLExtractor (no-param) routes
                // type=5 elements to AdjudicationDepot (which we need) but
                // also adds regular annotations to Depot (which duplicates
                // what was already loaded from saved/).
                depot.articleInsurance(textFilename);
                Article depotArticle = depot.getArticleByFilename(textFilename);
                Vector<Annotation> originalAnnotations = null;
                if (depotArticle != null) {
                    originalAnnotations = new Vector<>(depotArticle.annotations);
                }

                importer.XMLExtractor(parsedXml);

                // The <annotation> elements of the adjudication XML are the
                // final adjudicated results; XMLExtractor routed them to Depot.
                // They also belong in AdjudicationDepot so they survive the next
                // save, carrying the status parsed from the XML.
                if (depotArticle != null && originalAnnotations != null) {
                    Vector<Annotation> newlyAdded =
                            removeByIdentity(depotArticle.annotations, originalAnnotations);

                    if (!newlyAdded.isEmpty()) {
                        adjudication.data.AdjudicationDepot adjDepotInstance =
                                new adjudication.data.AdjudicationDepot();
                        adjDepotInstance.articleInsurance(textFilename);
                        Article adjArticle = adjudication.data.AdjudicationDepot
                                .getArticleByFilename(textFilename);
                        if (adjArticle != null) {
                            for (Annotation ann : newlyAdded) {
                                adjArticle.annotations.add(ann);
                            }
                        }
                        log.LoggingToFile.log(Level.INFO, "Loaded " + newlyAdded.size()
                                + " adjudicated annotations into AdjudicationDepot from "
                                + xmlFile.getName());
                    }

                    // Restore original annotations to undo duplicate additions
                    depotArticle.annotations = originalAnnotations;
                }
            } catch (Exception ex) {
                log.LoggingToFile.log(Level.WARNING,
                        "Failed to load adjudication state from: " + xmlFile.getName()
                                + " - " + ex.getMessage());
            }
        }

        log.LoggingToFile.log(Level.INFO,
                "Loaded adjudication working state from " + xmlFiles.size() + " file(s).");
        return true;
    }

    private static File getAdjudicationDir() {
        File project = env.Parameters.WorkSpace.CurrentProject;
        if (project == null || !project.exists()) {
            return null;
        }
        return new File(project.getAbsolutePath() + File.separatorChar + "adjudication");
    }

    private static Vector<File> listKnowtatorXMLs(File folder) {
        Vector<File> xmlFiles = new Vector<File>();
        File[] files = folder.listFiles();
        if (files == null) {
            return xmlFiles;
        }
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".knowtator.xml")) {
                xmlFiles.add(f);
            }
        }
        return xmlFiles;
    }
}
