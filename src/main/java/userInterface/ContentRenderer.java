package userInterface;

import adjudication.parameters.Paras;
import adjudication.statusBar.DiffCounter;
import env.Parameters;
import main.eHOST;
import org.apache.commons.io.FileUtils;
import resultEditor.annotations.Annotation;
import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import resultEditor.conflicts.classConflict;
import resultEditor.conflicts.spanOverlaps;
import resultEditor.conflicts.tmp_Conflicts;
import resultEditor.positionIndicator.JPositionIndicator;
import resultEditor.workSpace.WorkSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.logging.Level;

/**
 * Manages content rendering functionality for the GUI.
 * This class encapsulates all content rendering related operations
 * that were previously part of the GUI class.
 *
 * @author jianlins
 */
public class ContentRenderer {
    private final GUI gui;

    public ContentRenderer(GUI gui) {
        this.gui=gui;
    }

    /**
     * Shows file content in the text pane by file name
     *
     * @param fileName The name of the file to show
     * @return Response message indicating success or failure
     */
    public String showFileContextInTextPane(String fileName) {
        String response = "File: '" + fileName + "' not found in project: " + Parameters.previousProjectPath;
        int previousSelectFileId = gui.gui.jComboBox_InputFileList.getSelectedIndex();
        int selectFileId = -1;
        if (gui.fileIdMap.containsKey(fileName)) {
            selectFileId = gui.fileIdMap.get(fileName);
            response = "success";
        } else {
            for (String loadedFileName : gui.fileIdMap.keySet()) {
                if (loadedFileName.contains(fileName)) {
                    selectFileId = gui.fileIdMap.get(loadedFileName);
                    response = "Load file: " + loadedFileName;
                    break;
                }
            }
        }
        if (previousSelectFileId == selectFileId) {
            return "Still the same file";
        }

        if (selectFileId >= gui.gui.jList_corpus.getModel().getSize()) {
            main.eHOST.logger.debug("No file found for file index id: " + selectFileId);
            return "No file found";
        }
        gui.gui.jList_corpus.ensureIndexIsVisible(selectFileId);
        gui.gui.jList_corpus.setSelectedIndex(selectFileId);

        goUserDesignatedTable();
        return response;
    }

    /**
     * Shows file content in the text pane by index
     *
     * @param index The index of the file to show
     */
    public void showFileContextInTextPane(int index) {
        try {
            if (index < 0)
                index = 0;

            // ##1 validity check to "selectedIndex"
            // numbers of items in the filelist
            int size = gui.jComboBox_InputFileList.getItemCount();
            if (index > (size - 1))
                return;
            gui.jComboBox_InputFileList.setSelectedIndex(index);
            gui.jList_corpus.setSelectedIndex(index);
            gui.jList_corpus.ensureIndexIsVisible(index);

            // ##2 get the file you want to show in text pane.
            if (Parameters.corpus.LIST_ClinicalNotes.size() < 0) {
                main.eHOST.logger.debug("No file opened yet.");
                return;
            }
            if (index >= Parameters.corpus.LIST_ClinicalNotes.size()) {
                main.eHOST.logger.warn("Try to access file index " + index + " greater than the total number of files (" + Parameters.corpus.LIST_ClinicalNotes.size() + ")in current project. Ignore this request.");
                return;
            }
            File currentTextSource = Parameters.corpus.LIST_ClinicalNotes.get(index).file;

            // ##2.1 record current operatig file into workset
            WorkSet.setCurrentFile(currentTextSource, index);
            // validity check
            if (currentTextSource == null)
                return;

            // ##3 Load all text content by lines from this file.
            ArrayList<String> contents = loadFileContents(currentTextSource);

            setDoclength(contents);
            // ##4 SHOW text content on screen and HIGHLIGHT annotations
            resultEditor.display.Screen display = new resultEditor.display.Screen(
                    gui.textPaneforClinicalNotes, currentTextSource // text source
            );

            display.ShowTextAndBackgroundHighLight(contents);
            main.eHOST.logger.debug("file content highlighted.");
        } catch (Exception ex) {
            main.eHOST.logger.warn("error 1417");
        }
    }

    /**
     * Shows file content in the text pane by file
     *
     * @param _current_text_file The file to show
     */
    public void showFileContextInTextPane(File _current_text_file) {
        if (_current_text_file == null) {
            main.eHOST.logger.warn("error 1103251356:: fail to find file context");
            return;
        }

        // ##2.1 record current operatig file into workset
        WorkSet.setCurrentFile(_current_text_file);
        // validity check

        // ##3 Load all text content by lines from this file.
        ArrayList<String> contents = loadFileContents(_current_text_file);
        setDoclength(contents);

        // ##4 SHOW text content on screen and HIGHLIGHT annotations
        resultEditor.display.Screen display = new resultEditor.display.Screen(
                gui.textPaneforClinicalNotes, _current_text_file // text source
        );

        display.ShowTextAndBackgroundHighLight(contents);
    }

    /**
     * Loads file contents
     *
     * @param _rawTextDocument The file to load
     * @return ArrayList of strings containing the file contents
     */
    private ArrayList<String> loadFileContents(File _rawTextDocument) {
        return commons.Filesys.ReadFileContents(_rawTextDocument);
    }

    /**
     * Sets the document length
     *
     * @param contents The contents of the document
     */
    private void setDoclength(ArrayList<String> contents) {
        if (contents == null)
            return;

        int length = 0;
        for (String paragraph : contents) {
            if (paragraph == null)
                continue;
            length = length + paragraph.length() + 1;
        }

        // The position indicator would need to be passed to this class
        // or this method would need to be called from the GUI class
    }

    public void showFirstFile_of_corpus(String fileName, boolean refresh) {

        gui.textPaneforClinicalNotes.setText(null);
        // ##1##
        // exit from annotation comparison mode if editor panel and comparator
        // panel are showed for comparing annotation conflits
        try {
            ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusInvisible();
            resetVerifier();
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE,
                    "error 1012011313-2:: fail to leave annotations compaison mode!!!");
        }

        try {
            // numbers of items in the filelist
            int size = gui.jComboBox_InputFileList.getItemCount();
            if (size < 1) {
                return;
            }

            WorkSet.latestScrollBarValue = 0;
            int fileId = getFileIdByName(fileName);
            if (fileId == -1)
                fileId = loadProjectPreviousViewedFileId();
            showFileContextInTextPane(fileId);


        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE,
                    "error 1012011314:: fail to switch to another document!!!");
        }

        // reset details display
        disableAnnotationDisplay();
        disable_AnnotationEditButtons();
        // currentScreen = infoScreens.NONE;
        if (refresh)
            refreshInfo();
    }
    protected void disable_AnnotationEditButtons() {
        // disable buttons for span and class edit
        gui.jButton_SelectClass.setEnabled(false);
        gui.jButton_spaneditor_lefttToLeft.setEnabled(false);
        gui.jButton_spaneditor_leftToRight.setEnabled(false);
        gui.jButton_span_rightToLeft.setEnabled(false);
        gui.jButton_spaneditor_delete.setEnabled(false);
        gui.jButton4_spanEditor_rightToRight.setEnabled(false);
        // jButton11.setEnabled(false);
        gui.jButton_SelectClass.setEnabled(false);
        gui.jButton_relationships.setEnabled(false);
        gui.delete_Relationships.setEnabled(false);
        gui.jButton_attribute.setEnabled(false);

        Vector v = new Vector();
        gui.jList3.setListData(v);
        gui.jList3.setBorder(null);

    }

    public void display_RelationshipPath_Remove() {
        if (!gui.jCardcontainer_interactive.isVisible())
            return;

        try {
            ((userInterface.txtScreen.TextScreen) gui.textPaneforClinicalNotes)
                    .clearComplexRelationshipCoordinates();

        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "\n==== ERROR ====::1106101301::" + ex.toString());
        }

    }
    public void disableAnnotationDisplay() {
        try {
            display_RelationshipPath_Remove();

            Vector v = new Vector();
            v.clear();
            gui.jList_selectedAnnotations.setListData(v);
            gui.jList_normalrelationship.setListData(v);
            gui.jList_complexrelationships.setListData(v);
            // jLabel_typeOfRelationship.setText("Attributes: ");
            gui.jTextPane_explanations.setText(null);
            gui.jList3.setListData(v);
            gui.jScrollPane_Spans.setBorder(BorderFactory
                    .createLineBorder(new Color(153, 153, 153)));
            gui.jTextArea_comment.setText("");
            gui.jList3.setBorder(null);
            gui.jList3.setListData(v);
            gui.jTextField_annotationClassnames.setText(null);

            gui.jTextField_annotator.setText(null);
            gui.jTextField_annotator.setBorder(gui.STANDARD_TEXTFIELD_BOARD);

            gui.jTextField_creationdate.setText(null);
            gui.jTextField_creationdate.setBorder(gui.STANDARD_TEXTFIELD_BOARD);

            gui.jButton_SelectClass.setEnabled(false);

            // refresh screen only if the text pane is visiable go user;
            // otherwise, do nothing.
            if (gui.textPaneforClinicalNotes.isVisible()) {
                resultEditor.underlinePainting.SelectionHighlighter painter = new resultEditor.underlinePainting.SelectionHighlighter(
                        gui.textPaneforClinicalNotes);
                painter.RemoveAllUnderlineHighlight();
            }
        } catch (Exception ex) {
            log.LoggingToFile.log(
                    Level.SEVERE,
                    "\n==== ERROR ====:: 1106101243:: fail to clear screen for new document"
                            + ex.toString());
        }
    }

    protected int loadProjectPreviousViewedFileId() {
        File log = new File(Parameters.previousProjectPath, ".log");
        int id = 0;
        if (log.exists()) {
            try {
                String content = FileUtils.readFileToString(log, StandardCharsets.UTF_8).trim();
                if (content != null && content.length() > 0) {
                    id = Integer.parseInt(content);
                }
            } catch (Exception e) {

            }
        }
        return id;
    }

    private int getFileIdByName(String fileName) {
        HashMap<String, Integer> fileIdMap = gui.fileIdMap;
        int selectFileId = -1;
        if (fileName == null || fileName.trim().length() == 0)
            return selectFileId;
        if (fileIdMap.containsKey(fileName)) {
            selectFileId = fileIdMap.get(fileName);
        } else {
            for (String loadedFileName : fileIdMap.keySet()) {
                if (loadedFileName.contains(fileName)) {
                    selectFileId = fileIdMap.get(loadedFileName);
                    break;
                }
            }
        }
        return selectFileId;
    }
    /**
     * By the user disignated textsourceFilename, show its text contents in text
     * area
     */
    public void goUserDesignated() {
        JTextPane textPaneforClinicalNotes = gui.textPaneforClinicalNotes;
        JPanel jPanel60 = gui.jPanel60;
        JComboBox jComboBox_InputFileList = gui.jComboBox_InputFileList;
        gui.display_removeSymbolIndicators();
        // ##1##
        // exit from annotation comparison mode if editor panel and comparator
        // panel are showed for comparing annotation conflits
        try {
            textPaneforClinicalNotes.setText(null);
            ((userInterface.annotationCompare.ExpandButton) jPanel60).setStatusInvisible();
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE,
                    "error 1012011313:: fail to leave annotations compaison mode!!!");
        }

        resetVerifier();

        try {
            ((userInterface.annotationCompare.ExpandButton) jPanel60).setStatusInvisible();
            ((userInterface.annotationCompare.ExpandButton) jPanel60).noDiff();

            // numbers of items in the filelist
            int size = jComboBox_InputFileList.getItemCount();
            if (size < 1)
                return;
            int fileListSize = env.Parameters.corpus.getSize();

            // use this to avoid the initial loading side effect
            if (size != fileListSize)
                return;

            int selected = jComboBox_InputFileList.getSelectedIndex();

            if (selected <= (size - 1)) {
                WorkSet.latestScrollBarValue = 0;
                // jComboBox_InputFileList.setSelectedIndex(selected + 1);
                showFileContextInTextPane(selected);
                gui.showAnnotationCategoriesInTreeView_CurrentArticle();
                gui.showValidPositionIndicators_setAll();
                gui.showValidPositionIndicators();

                gui.display_showSymbolIndicators();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE,
                    "error 1012011314:: fail to switch to another document!!!");
        }

        try {
            // reset details display
            disableAnnotationDisplay();
            disable_AnnotationEditButtons();
            // currentScreen = infoScreens.NONE;
            refreshInfo();
        } catch (Exception ex) {
            log.LoggingToFile
                    .log(Level.SEVERE,
                            "error 1012011312:: fail to update screen after switched to another document!!!");
        }

        if (gui.reviewmode == GUI.ReviewMode.adjudicationMode) {
            DiffCounter diffcounter = new DiffCounter(
                    gui.jLabel23, WorkSet.getCurrentFile().getName(), gui);
            diffcounter.reset();
        }
    }


    /**
     * Should be called whenever the infoList needs to be redrawn. currentScreen
     * variable must be set correctly for this to work.
     */
    private void reDrawInfoList() {

        switch (gui.currentScreen) {
            case ANNOTATIONS:
                gui.setToAnnotations();
                break;
            case ANNOTATORS:
                gui.setToAnnotators();
                break;
            case CLASSCONFLICT:
                gui.setToClassConflict();
                break;
            case NONE:
                gui.infoList.setListData(new Vector());
                break;
            case SPANCONFLICT:
                gui.setToSpanConflict();
                break;
            case CLASSES:
                gui.setToClasses();
                break;
            case VERIFIER:
                gui.setToVerifier();
                break;
        }
    }

    public void refreshInfo() {

        reDrawInfoList();

        new Thread() {

            @Override
            public void run() {
                try {
                    // jLabel29.setText("");
                    gui.classesList = new HashSet<String>();
                    gui.annotatorsList = new HashSet<String>();
                    gui.annotationsList = new Vector<Annotation>();
                    gui.conflictWithWorking = new Vector<classConflict>();
                    gui.verifierAnnotations = new Vector<Annotation>();
                    Depot depot = new Depot();
                    WorkSet.getCurrentFileIndex();
                    File current = WorkSet.getCurrentFile();
                    if (current != null) {
                        Article article = depot.getArticleByFilename(current.getName());
                        if (article == null) {
                            // If article is null then just clear out the
                            // results
                            gui.annotations.setText("<html>" + gui.annotationsText + "</html>");
                            gui.classes.setText("<html>" + gui.classesText + "</html>");
                            gui.annotators.setText("<html>" + gui.annotatorsText + "</html>");
                            gui.workingConflicts.setText("<html>" + gui.conflictsText + "</html>");
                            gui.overlapping.setText("<html>" + gui.overlappingText + "</html>");
                            gui.verifierFlagged.setText("<html>" + gui.verifierText + "</html>");
                            gui.currentScreen = GUI.infoScreens.NONE;
                            reDrawInfoList();
                            return;
                        }

                        // Get all of the annotations
                        gui.annotationsList = article.annotations;
                        // Loop through all annotations to get data
                        for (Annotation annotation : gui.annotationsList) {
                            // Capture all classes and annotators
                            gui.classesList.add(annotation.annotationclass);
                            gui.annotatorsList.add(annotation.getAnnotator());

                            // If the annotation has Verifier information then
                            // capture that as well
                            if ((annotation.verifierFound != null && annotation.verifierFound
                                    .size() > 0)
                                    || (annotation.verifierSuggestion != null && annotation.verifierSuggestion
                                    .size() > 0) || annotation.isVerified()) {
                                gui.verifierAnnotations.add(annotation);
                            }
                        }

                        // Update tab with new information
                        gui.overlappingAnnotations = tmp_Conflicts.getSpanConflicts(current.getName());
                        gui.conflictWithWorking = tmp_Conflicts.getClassConflicts(current.getName());
                        gui.annotations.setText("<html>" + gui.annotationsText + "<font color = \"blue\">"
                                + gui.annotationsList.size() + "</font></html>");
                        gui.classes.setText("<html>" + gui.classesText + "<font color = \"blue\">"
                                + gui.classesList.size() + "</font></html>");
                        gui.annotators.setText("<html>" + gui.annotatorsText + "<font color = \"blue\">"
                                + gui.annotatorsList.size() + "</font></html>");
                        gui.workingConflicts.setText("<html>" + gui.conflictsText
                                + "<font color = \"blue\">" + gui.conflictWithWorking.size()
                                + "</font></html>");
                        gui.overlapping.setText("<html>" + gui.overlappingText + "<font color = \"blue\">"
                                + gui.overlappingAnnotations.size() + "</font></html>");
                        gui.verifierFlagged.setText("<html>" + gui.verifierText + "<font color = \"blue\">"
                                + gui.verifierAnnotations.size() + "</font></html>");
                    }
                    GUI.status++;
                    if (GUI.status > GUI.readyThreshold)
                        GUI.selectedFromComobox = true;
                    eHOST.logger.debug("refreshInfo finished.\t Current status: " + gui.status);
                } catch (Exception ex) {
                    eHOST.logger.debug("refreshInfo throw exceptions");
                    GUI.status = 4;
                }
            }

        }.start();

    }

    private void resetVerifier() {
        WorkSet.currentlyViewing = new ArrayList<Integer>();
        WorkSet.filteredViewing = false;
    }
    public void goUserDesignatedTable() {
        // resetVerifier();
        JPanel jPanel60 = gui.jPanel60;
        gui.selectedFromComobox = false;
        ((userInterface.annotationCompare.ExpandButton) jPanel60).setStatusInvisible();
        ((userInterface.annotationCompare.ExpandButton) jPanel60).noDiff();

        // numbers of items in the filelist
        int size = gui.gui.jList_corpus.getModel().getSize();
        if (size < 1)
            return;
        int fileListSize = env.Parameters.corpus.getSize();

        // use this to avoid the initial loading side effect
        if (size != fileListSize)
            return;


        int selected = gui.gui.jList_corpus.getSelectedIndex();

        if (selected <= (size - 1)) {
            gui.jComboBox_InputFileList.setSelectedIndex(selected);
            WorkSet.latestScrollBarValue = 0;
            // gui.jComboBox_InputFileList.setSelectedIndex(selected + 1);
            showFileContextInTextPane(selected);
            gui.showAnnotationCategoriesInTreeView_CurrentArticle();
            gui.showValidPositionIndicators_setAll();
            gui.showValidPositionIndicators();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }

        // reset details display
        disableAnnotationDisplay();
        disable_AnnotationEditButtons();
        // currentScreen = infoScreens.NONE;
        refreshInfo();

        if (gui.reviewmode == GUI.ReviewMode.adjudicationMode) {
            DiffCounter diffcounter = new DiffCounter(
                    gui.jLabel23, WorkSet.getCurrentFile().getName(), gui);
            diffcounter.reset();
        }
    }

    /**
     * Tell system which review mode eHOST will be working in, are set the
     * button status. There are two review modes: annotation mode, and consensus
     * mode.
     */
    public void setReviewMode(GUI.ReviewMode thismode) {

        gui.reviewmode = thismode; // set current review mode

        // by given current review mode, select matched togglebutton
        switch (thismode) {

            // annotation mode
            case ANNOTATION_MODE:

                env.Parameters.enabled_Diff_Display = true;

                gui.jRadioButton_annotationMode.setSelected(true);
                gui.jRadioButton_adjudicationMode.setSelected(false);
                gui.jRadioButton_annotationMode.setVisible(true);
                gui.jRadioButton_adjudicationMode.setVisible(true);
                gui.jButton9.setVisible(false);

                // if( )
                // jPanel60.setVisible( env.Parameters.EnableDiffButton );

                setComponentsVisibleForConsensusMode(true); // show top function
                // buttons

                gui.display_adjudication_setOperationBarStatus(gui.HIDEN);

                break;

            // consensus mode
            case adjudicationMode:

                // jPanel60.setVisible( true );
                gui.display_adjudication_setOperationBarStatus(gui.DISABLED);

                env.Parameters.enabled_Diff_Display = true; // enable diff flag to
                // show diff
                gui.display_repaintHighlighter();

                gui.jRadioButton_annotationMode.setVisible(true);
                gui.jRadioButton_adjudicationMode.setVisible(true);
                gui.jRadioButton_annotationMode.setSelected(false);
                gui.jRadioButton_adjudicationMode.setSelected(true);
                gui.jButton9.setVisible(true);

                setComponentsVisibleForConsensusMode(false); // hide buttons which
                // are not related
                // to consensus mode
                gui.repaint();

                Paras.__adjudicated = adjudication.data.AdjudicationDepot.isReady();

                // let user select what they want
                if ((Paras.__adjudicated) && (Paras.isReadyForAdjudication())) {
                    Object[] options = {"Yes, please", "No, Start a new adjudication", "Cancel"};
                    int i = JOptionPane.showOptionDialog(gui,
                            "Would you like to continue your previous adjudication work?", "yes",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            options, options[0]);

                    // ---- 2.1 ----
                    if (i == 0) { // yes, continue latest adjudication work
                        gui.mode_continuePreviousAdjudicationWork();
                        return;
                    } else if (i == 2) { // cancel
                        // ---- 2.2 ----
                        gui.mode_enterAnnotationMode(true);
                    }

                    if (i != 1)
                        break;
                }

                // ---- 2.3 ----
                // enable the dialog to let user select conditions and enter the
                // adjudication mode

                // enable the dialog to ask user setting what is difference
                if (gui.dialog_adjudication == null) {

                    gui.dialog_adjudication = new adjudication.Adjudication(gui);
                    gui.dialog_adjudication.setVisible(true);
                } else {
                    gui.dialog_adjudication.dispose();
                    gui.dialog_adjudication = new adjudication.Adjudication(gui);
                    gui.dialog_adjudication.setVisible(true);
                }

                /**/
                break;

            case OTHERS:
                // jPanel60.setVisible( false );
                gui.jRadioButton_annotationMode.setSelected(true);
                gui.jRadioButton_adjudicationMode.setSelected(false);
                gui.jRadioButton_annotationMode.setVisible(false);
                gui.jRadioButton_adjudicationMode.setVisible(false);
                gui.jButton9.setVisible(false);

                setComponentsVisibleForConsensusMode(true); // show top function
                // buttons while out of
                // consense mode
                break;
        }
    }

    /**
     * set components, such as buttons, visible or invisible for the consensus
     * review mode.
     *
     * @param isVisible false: hide non-relavent components for consensus review mode;
     *                  true: set these components visible to user.
     */
    private void setComponentsVisibleForConsensusMode(boolean isVisible) {

        // jToggle_AssignmentsScreen.setVisible( isVisible );
        JToggleButton jToggleButton_CreateAnnotaion = gui.jToggleButton_CreateAnnotaion;
        JToggleButton jToggleButton_PinExtractor = gui.jToggleButton_PinExtractor;
        JToggleButton jToggleButton_DictionarySetting = gui.jToggleButton_DictionarySetting;
        JToggleButton jToggleButton_DictionaryManager = gui.jToggleButton_DictionaryManager;
        if (env.Parameters.Sysini.functions[1] == '1')
            jToggleButton_CreateAnnotaion.setVisible(isVisible);
        else
            jToggleButton_CreateAnnotaion.setVisible(false);

        if (env.Parameters.Sysini.functions[2] == '1')
            jToggleButton_PinExtractor.setVisible(isVisible);
        else
            jToggleButton_PinExtractor.setVisible(false);

        if (env.Parameters.Sysini.functions[3] == '1')
            jToggleButton_DictionaryManager.setVisible(isVisible);
        else
            jToggleButton_DictionaryManager.setVisible(false);

        if (env.Parameters.Sysini.functions[5] == '1')
            jToggleButton_DictionarySetting.setVisible(isVisible);
        else
            jToggleButton_DictionarySetting.setVisible(false);

        gui.jToggle_AssignmentsScreen.setVisible(isVisible);

        gui.jToggleButton_Converter.setVisible(false);

        // display or hide the button of "import annotations"
        gui.jButton_importAnnotations.setVisible(isVisible);

        // display or hide the button of
        // "remove all current annotations from memory"
        gui.jButton_removeAllAnnotations.setVisible(isVisible);

        // display or hide the separator after above two buttons
        // jLabel_separator02_onViewer.setVisible( isVisible );

        gui.jLabel_infobar_FlagOfDiff.setVisible(isVisible);

        // jLabel11.setVisible(isVisible);

        gui.jButton20.setVisible(isVisible);

    }

    /*
     * Called when display current list in viewer button is pressed. This method
     * will display whatever is in the current list, if it is a list of
     * annotations.
     */
    protected void displayCurrentActionPerformed(ActionEvent evt)// GEN-FIRST:event_displayCurrentActionPerformed
    {// GEN-HEADEREND:event_displayCurrentActionPerformed
        // Make sure we're viewing a list of annotations... either from the
        // Verifier or just
        // annotations
        JList infoList = gui.infoList;
        if (gui.currentScreen == GUI.infoScreens.ANNOTATIONS || gui.currentScreen == GUI.infoScreens.VERIFIER) {
            // Get all of the Annotations in the list
            int entries = infoList.getModel().getSize();

            // if there are no entries then return.
            if (entries == 0) {
                return;
            }
            // If the first object is not an annotation then return.
            if (!infoList.getModel().getElementAt(0).getClass().isInstance(new Annotation())) {
                return;
            }
            // to store results
            Vector<Annotation> currentlyViewing = new Vector<Annotation>();

            // get the list of annotations so we can display them.
            for (int i = 0; i < entries; i++) {
                currentlyViewing.add((Annotation) infoList.getModel().getElementAt(i));
            }

            // Draw the selected annotations(highlights)
            repaintNewAnnotations(currentlyViewing);
        }
        // If we're viewing class conflicts then display all annotations
        // involved.
        else if (gui.currentScreen == GUI.infoScreens.CLASSCONFLICT) {
            // Get the number of class conflicts
            int entries = infoList.getModel().getSize();

            // Return if there are no entries.
            if (entries == 0) {
                return;
            }
            // return if the first element is not a class conflict
            if (!infoList.getModel().getElementAt(0).getClass().isInstance(new classConflict())) {
                return;
            }
            // Get the curently viewed class conflicts.
            Vector<classConflict> currentlyViewing = new Vector<classConflict>();
            for (int i = 0; i < entries; i++) {
                currentlyViewing.add((classConflict) infoList.getModel().getElementAt(i));
            }

            // Get the annotations from each class conflict.
            Vector<Annotation> annotations = new Vector<Annotation>();
            for (classConflict conflict : currentlyViewing) {
                annotations.addAll(conflict.getInvolved());
            }
            // Redraw the highlighting
            repaintNewAnnotations(annotations);
        }
        // Display all annotations in a span conflict.
        else if (gui.currentScreen == GUI.infoScreens.SPANCONFLICT) {
            int entries = infoList.getModel().getSize();
            // return if there are no entries
            if (entries == 0) {
                return;
            }
            // return if it's not a list of span overlaps
            if (!infoList.getModel().getElementAt(0).getClass().isInstance(new spanOverlaps())) {
                return;
            }
            // get the span overlaps
            Vector<spanOverlaps> currentlyViewing = new Vector<spanOverlaps>();
            for (int i = 0; i < entries; i++) {
                currentlyViewing.add((spanOverlaps) infoList.getModel().getElementAt(i));
            }
            // get the annotations
            Vector<Annotation> annotations = new Vector<Annotation>();
            for (spanOverlaps conflict : currentlyViewing) {
                annotations.addAll(conflict.getInvolved());
            }
            // redraw the annotations
            repaintNewAnnotations(annotations);
        }
    }// GEN-LAST:event_displayCurrentActionPerformed

    /**
     * Redraw the highlighting and only draw the passed in annotations
     *
     * @param currentlyViewing - the annotations to highlight.
     */
    private void repaintNewAnnotations(Vector<Annotation> currentlyViewing) {
        JPositionIndicator jpositionIndicator = gui.jpositionIndicator;
        Depot depot = new Depot();
        Article article = depot.getArticleByFilename(WorkSet.getCurrentFile().getName());
        depot.setAnnotationsVisible(currentlyViewing, article);

        // repaint
        jpositionIndicator.removeAllIndicators();
        jpositionIndicator.paintArticle(WorkSet.getCurrentFile().getName().trim());
        jpositionIndicator.forcepaint();
        jpositionIndicator.repaint();

        // go to file viewer to view newly painted annotations
        int selected = gui.jComboBox_InputFileList.getSelectedIndex();
        showFileContextInTextPane(selected);
        gui.jTabbedPane3.setSelectedIndex(0);
    }

    /**
     * Handles mouse click events on the text pane
     *
     * @param evt The mouse event that triggered the method
     */
    public void mouseClickOnTextPane(MouseEvent evt) {
        try {
            // validity checking
            if ((!gui.isEnabled()) || (!gui.isVisible()) || (!gui.isActive()))
                return;

            // remove previous selection highlighters
            final resultEditor.underlinePainting.SelectionHighlighter highlighter = new resultEditor.underlinePainting.SelectionHighlighter(
                    gui.textPaneforClinicalNotes);
            highlighter.RemoveAllUnderlineHighlight();

            Annotation previousAnnotation = null;
            if (WorkSet.currentAnnotation != null) {
                previousAnnotation = WorkSet.currentAnnotation;
            }

            int clicks = evt.getClickCount();

            // #### 0.1 #### clicked by left key or right key
            boolean leftClick = evt.getButton() == MouseEvent.BUTTON1;
            boolean rightClick = evt.getButton() == MouseEvent.BUTTON3;

            if (!rightClick) {
                // #### 0.4 #### preset: leave from comparision mode if it
                // was in a potential compraision status
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusInvisible();
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).noDiff();
            }

            // cancel any possible flag of "create new dis-joint span"
            // after any right click.
            if (rightClick)
                gui.setFlag_allowToAddSpan(false);

            // ---------------------------------------------- //
            // function: add dis-joint span if wanted
            if (gui.WANT_NEW_SPAN) {
                // if the indicator is "ON"(false), check and correct span
                // border by space or symbols, o.w. use exact span that user just
                // selected
                gui.operation_checkBorder_whileNeeded(gui.textPaneforClinicalNotes);

                gui.addSpan(evt, false);
                gui.setFlag_allowToAddSpan(false);
                return;
            }
            // ---------------------------------------------- //

            // #### 0.2 #### get filename of current document
            String textsourceFilename = WorkSet.getCurrentFile().getName();

            // get position of mouse
            int position = gui.textPaneforClinicalNotes.viewToModel(evt.getPoint());

            // #### 0.3 #### try to use current mouse carpet position to select
            // annotations
            ArrayList<Annotation> selectedAnnotaions = Depot.SelectedAnnotationSet
                    .selectAnnotations_ByPosition(textsourceFilename, position, false);

            // Catch all Left-Click Mouse Events. If we are creating a
            // relatinoship this will signal the end of relationship creation.
            // The Currently active relationship chain will be stopped and this
            // method will return(to avoid activating other mouse events).
            if (WorkSet.makingRelationships && leftClick) {
                if (gui.stopRelationshiping())
                    return;
            }

            // ##1## right key to popup Dialog popmenu for select text to
            // build a new annotation
            // ##2## caught similar annotations in all documents
            if (gui.textPaneforClinicalNotes.getSelectedText() != null) {
                gui.setFlag_allowToAddSpan(true);

                // if the indicator is "ON"(false), check and correct span
                // border by space or symbols, o.w. use exact span that user just
                // selected
                gui.operation_checkBorder_whileNeeded(gui.textPaneforClinicalNotes);

                if (gui.popmenu == null || !gui.popmenu.isGood()) {
                    gui.popmenu = new resultEditor.annotationBuilder.Popmenu(gui.textPaneforClinicalNotes,
                            gui, evt);
                }

                // do not want to build a annotation by method of
                // "ONE-CLICK-TO-BUILD-ANNOTATION"
                if (env.Parameters.currentMarkables_to_createAnnotation_by1Click == null)
                    gui.popmenu.pop(evt.getX(), evt.getY(), gui.getX(), gui.getY(), gui.getWidth(),
                            gui.getHeight());
                else
                // "ONE-CLICK-TO-BUILD-ANNOTATION", no popmenu will be pop out
                // in following process
                {
                    if (gui.popmenu != null) {
                        gui.popmenu.setVisible(false);
                        gui.popmenu = new resultEditor.annotationBuilder.Popmenu(
                                gui.textPaneforClinicalNotes, gui, evt);
                    } else
                        gui.popmenu = new resultEditor.annotationBuilder.Popmenu(
                                gui.textPaneforClinicalNotes, gui, evt);

                    String thismarkablename = env.Parameters.currentMarkables_to_createAnnotation_by1Click;
                    gui.popmenu.oneClicktoCreateAnnotation(thismarkablename);
                }

                return;
            }

            // #============================================================#
            // ##3## build complex relationships between annotaions
            // This occurs when Relationship button is toggled and a right
            // click occurs. This allows users to add Annotations to a
            // relationship and create new relationships.
            // #============================================================#
            else if ((clicks == 1) && WorkSet.makingRelationships && rightClick) {
                // Simply return if no annotation is currently selected
                if (WorkSet.currentAnnotation == null)
                    return;

                gui.addRelationship(position);

                // reset the flag everytime after adding a relationship
                // WorkSet.makingRelationships = false;

                return;
            }

            Depot.SelectedAnnotationSet.selectAnnotations_ByPosition(
                    textsourceFilename, position, true);
            // ##2## left key to get a current position and show found
            // annotations in this position

            // Record for current workset: latest position in text panel
            WorkSet.latestClickPosition = position;

            // if no annotation caught
            if ((selectedAnnotaions == null) || (selectedAnnotaions.size() < 1)) {
                // disable all operation buttons
                // if these is no annotation catched by current cursor position
                WorkSet.currentAnnotation = null;
                disableAnnotationDisplay();
                disable_AnnotationEditButtons();

                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusInvisible();
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).noDiff();

                return;
            }

            // if got result in this cursor position
            int amount = selectedAnnotaions.size();
            if (amount > 0) {
                if (gui.reviewmode == GUI.ReviewMode.adjudicationMode) {
                    // jPanel60.setVisible( true );
                    DiffCounter diffposition = new DiffCounter(gui.jLabel23, WorkSet.getCurrentFile()
                            .getName(), gui);
                    // int cursorPosition = textPaneforClinicalNotes.(
                    // evt.getPoint() );
                    diffposition.setSelected(position);
                }

                // record mouse position
                WorkSet.latestValidClickPosition = position;

                // show found annotations in list
                gui.showSelectedAnnotations_inList(0);

                // if multiple annotations got selected, we will use diff panel
                // to manage them
                if (amount == 1) {
                    // if (reviewmode == GUI.ReviewMode.ANNOTATION_MODE) {

                    // If same annotation Clicked open Attribute Editor
                    if ((!rightClick) && (previousAnnotation != null)
                            && (selectedAnnotaions != null))//
                    {
                        boolean contained = false;
                        for (Annotation anno : selectedAnnotaions) {
                            if (anno == null) {
                                continue;
                            }

                            if (anno.isDuplicate(previousAnnotation, WorkSet.getCurrentFile()
                                    .getName())) {

                                if ((gui.attributeAnnotation != null)
                                        && (gui.attributeAnnotation.isDuplicate(previousAnnotation,
                                        WorkSet.getCurrentFile().getName()))) {
                                    gui.attributeAnnotation = null;
                                    contained = false;
                                    break;
                                } else {
                                    gui.attributeAnnotation = previousAnnotation;
                                    contained = true;
                                    break;
                                }
                            }
                        }

                        if (contained && env.Parameters.enabled_displayAttributeEditor) {
                            // && (previousAnnotation.attributes != null)
                            // && (previousAnnotation.attributes.size()>0) ) {
                            gui.openAttributeEditor();
                            return;
                        }
                    }

                    if (gui.reviewmode == GUI.ReviewMode.adjudicationMode) {
                        boolean showbutton = false;
                        if (gui.checkAnnotator_ADJUDICATION(selectedAnnotaions))
                            showbutton = false;
                        else
                            showbutton = true;

                        gui.setVisible_EditorAdjuducation(showbutton);

                        if (showbutton) {
                            Depot.setSingleAnnotation(selectedAnnotaions
                                    .get(0));
                        }

                        gui.display_relationshipPath_setPath(selectedAnnotaions.get(0));
                    }
                } else if (amount > 1) {
                    // set the diff button to visible
                    ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusVisible();

                    // if these annotations are same
                    // if( isAllSelectedAnnotationSame() ){
                    // right diff panel available if flag is true
                    // ((userInterface.annotationCompare.ExpandButton)jPanel60).setStatusInvisible();
                    // start diff if flag is true
                    ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).noDiff();

                    // show all annotations
                    // showAnnotatorsOfAllSelectedAnnotations();
                    // }
                    // else
                    // {
                    // start diff if flag is true
                    ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).startDiff();
                    // }

                } else {
                    // right diff panel available if flag is true
                    ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusInvisible();
                    // start diff if flag is true
                    ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).noDiff();
                }

                gui.enable_AnnotationEditButtons();

                // Check for right click
                if (evt.getModifiers() == InputEvent.BUTTON3_MASK) {
                    Annotation toPopFor = WorkSet.currentAnnotation;
                    resultEditor.PopUp.rightClickOnAnnotPopUp annotPopUp = new resultEditor.PopUp.rightClickOnAnnotPopUp(
                            gui.textPaneforClinicalNotes, selectedAnnotaions, gui);
                    annotPopUp.pop(toPopFor, evt.getX(), evt.getY());
                }

            } else // if no annotation is selected
            {
                // set the diff button to visible
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusVisible();
                // start diff if flag is true
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).startDiff();

                // right diff panel available if flag is true
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).setStatusInvisible();
                // start diff if flag is true
                ((userInterface.annotationCompare.ExpandButton) gui.jPanel60).noDiff();
                // disable all operation buttons
                // if these is no annotation catched by current cursor position
                disableAnnotationDisplay();
                disable_AnnotationEditButtons();
            }

        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1106081501::" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
