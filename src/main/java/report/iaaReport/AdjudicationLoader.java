package report.iaaReport;

import imports.ImportXML;
import imports.importedXML.eXMLFile;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.annotations.ImportAnnotation;

import java.io.File;
import java.util.ArrayList;
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
            for (Article article : articles) {
                if (article == null || article.annotations == null) {
                    continue;
                }
                article.annotations.removeAll(loadedAnnotations);
            }
        }
        loadedAnnotations.clear();
        log.LoggingToFile.log(Level.INFO, "Adjudication annotations cleaned up from Depot.");
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
