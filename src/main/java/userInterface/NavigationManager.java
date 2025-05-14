package userInterface;

import env.Parameters;
import main.eHOST;
import org.apache.commons.io.comparator.NameFileComparator;

import userInterface.GUI.ReviewMode;
import userInterface.structure.FileObj;
import userInterface.structure.FileRenderer;
import webservices.AssignmentsScreen;
import workSpace.ProjectLock;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import static userInterface.GUI.reviewmode;

/**
 * Manages file navigation functionality for the GUI.
 * This class encapsulates all file navigation related operations
 * that were previously part of the GUI class.
 *
 * @author AI Assistant
 */
public class NavigationManager {
    private GUI gui;

    public NavigationManager(GUI gui) {
        this.gui=gui;
    }

    /**
     * Refreshes the file list in the UI
     */
    public void refreshFileList() {
        main.eHOST.logger.debug("Starting refreshFileList...");
        File[] corpus = listCorpus_inProjectFolder(Parameters.WorkSpace.CurrentProject);
        if (corpus != null)
            Arrays.sort(corpus, NameFileComparator.NAME_COMPARATOR);
        listCorpus(corpus);

        // add into memory
        Parameters.corpus.RemoveAll();
        if (corpus != null) {
            for (File txtfile : corpus) {
                if (txtfile == null)
                    continue;
                Parameters.corpus.addTextFile(txtfile);
            }
        }

        showTextFiles_inComboBox();
    }

    /**
     * Lists the corpus files in the UI
     *
     * @param corpus Array of corpus files
     */
    public void listCorpus(File[] corpus) {
        // empty the list
        gui.jList_corpus.setListData(new Vector());
        gui.fileIdMap.clear();
        if (corpus != null)
            Arrays.sort(corpus, NameFileComparator.NAME_COMPARATOR);

        try {
            Vector<FileObj> entries = new Vector<FileObj>();
            for (File file : corpus) {
                FileObj fileobj = new FileObj(file.getName(), gui.icon_note2);
                gui.fileIdMap.put(file.getName(), entries.size());
                entries.add(fileobj);
            }
            gui.jList_corpus.setListData(entries);
            gui.jList_corpus.setCellRenderer(new FileRenderer());
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101211147:: fail to list corpus on screen"
                    + ex.getMessage());
        }
    }

    /**
     * Lists all txt files under the given project folder
     *
     * @param project The project folder
     * @return Array of text files
     */
    public File[] listCorpus_inProjectFolder(File project) {
        File[] txtfiles = null;

        try {
            // get corpus folder
            File corpusfolder = null;
            File[] files = project.listFiles();
            if (files == null)
                return null;
            for (File file : files) {
                if (file.getName().toLowerCase().compareTo(env.CONSTANTS.corpus) == 0) {
                    corpusfolder = file;
                    break;
                }
            }

            if (corpusfolder == null)
                return null;

            File[] allcorpusfiles = corpusfolder.listFiles();
            if (allcorpusfiles == null)
                return null;
            int countfiles = 0;
            for (File file : allcorpusfiles) {
                if (file.isFile() && (!file.isHidden())) {
                    countfiles++;
                }
            }

            int point = 0;
            txtfiles = new File[countfiles];
            for (File file : allcorpusfiles) {
                if (file.isFile() && (!file.isHidden())) {
                    txtfiles[point] = file;
                    point++;
                }
            }
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101211133::");
        }

        return txtfiles;
    }

    /**
     * Selects a project by name
     *
     * @param projectName The name of the project
     * @param fileName The name of the file to select (can be null)
     * @return The selected file name
     */
    public String selectProject(String projectName, String fileName) {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            String response = "Project: " + projectName + " not found";
            try {
                if (gui.projectIdMap.containsKey(projectName)) {
                    if (Parameters.previousProjectPath == null || !Parameters.previousProjectPath.endsWith(projectName)) {
                        selectProject(gui.projectIdMap.get(projectName), fileName);
                        response = "success";
                    } else {
                        gui.showFileContextInTextPane(fileName);
                        response = projectName + " / " + fileName + " loaded";
                    }
                    GUI.status = 3;
                } else {
                    GUI.status = 4;
                }
                eHOST.logger.debug(response);

                // Complete the future with the response value
                responseFuture.complete(response);
            } catch (Exception e) {
                // Handle any exceptions during the operation
                responseFuture.completeExceptionally(e);
                eHOST.logger.error("Error selecting project: " + e.getMessage(), e);
            }
        });

        try {
            // Wait for and return the response from the EDT operation
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            eHOST.logger.error("Error waiting for project selection: " + e.getMessage(), e);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return "Error selecting project: " + e.getMessage();
        }
    }

    /**
     * Selects a project by ID
     *
     * @param projectId The ID of the project
     * @param fileName The name of the file to select (can be null)
     */
    public void selectProject(int projectId, String fileName) {
        Object o = gui.jList_NAV_projects.getModel().getElementAt(projectId);
        if (o == null)
            return;

        navigatorContainer.ListEntry_Project entry = (navigatorContainer.ListEntry_Project) o;

        if (entry == null) {
            log.LoggingToFile
                    .log(Level.SEVERE,
                            "#### ERROR #### 1106091554::fail to get a selected item from the list of project!!!");
            return;
        }

        File p = entry.getFolder();
        if (p == null) {
            log.LoggingToFile
                    .log(Level.SEVERE,
                            "#### ERROR #### 1102110353:: current project we got from you selected item in the list of project is NULL");
            return;
        }

        gui.setFlag_allowToAddSpan(false);
        selectProject(p, fileName);
    }

    /**
     * Selects a project by file
     *
     * @param projectFolder The project file
     * @param fileName The name of the file to select (can be null)
     */
    public void selectProject(File projectFolder, String fileName) {
        JPanel NavigationPanel_editor = gui.NavigationPanel_editor;
        JPanel NavigationPanel1 = gui.NavigationPanel1;
        try {
            if (GUI.status > 1) {
                eHOST.logger.debug("Reset gui status to 0.");
                GUI.status = 0;
            }

            // check if project is used by another eHOST instance

            ProjectLock lock = new ProjectLock(projectFolder);

            if (!lock.acquireLock()) {
                eHOST.logger.warn("The project is already in use by another user.");
                return;
            }


            eHOST.logger.debug("Selecting project: " + projectFolder.getName() + "...\t Current status" + GUI.status);
            gui.getContentRenderer().setReviewMode(ReviewMode.ANNOTATION_MODE);

            //release current lock
            if (env.Parameters.WorkSpace.CurrentProject!=null && env.Parameters.WorkSpace.CurrentProject!=projectFolder){
                new ProjectLock(Parameters.WorkSpace.CurrentProject).releaseLock();
            }
            // ##3## set current project

            env.Parameters.WorkSpace.CurrentProject = projectFolder;
            env.Parameters.previousProjectPath = projectFolder.getAbsolutePath();

            gui.modified = false;

            // #### load configure settings of this project
            config.project.ProjectConf projectconf = new config.project.ProjectConf(projectFolder);
            gui.infoBarTarget=projectFolder.getAbsolutePath();
            if (projectconf.foundOldConfigureFile()) {
                Object[] options = {"Yes, please", "No"};
                final JOptionPane optionPane = new JOptionPane();
                int xp = gui.NavigationPanel1.getWidth();
                int yp = gui.ToolBar.getHeight();
                optionPane.setSize(500, 300);

                optionPane.setLocation(gui.getX() + xp + (int) ((gui.getWidth() - xp - 500) / 2),
                        gui.getY() + yp + (int) ((gui.getHeight() - yp - 300) / 2));
                int answer = optionPane
                        .showOptionDialog(
                                gui,
                                "<html>We find an old version of eHOST configure file under<p> your "
                                        + "current project. Do you want to load and convert it into new format?<html>",
                                "Older Configure File:", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (answer == JOptionPane.YES_OPTION) {
                    projectconf.loadConfigure();
                    projectconf.rename();
                } else {
                    projectconf.loadXmlConfigure();
                }

            } else {
                projectconf.loadXmlConfigure();
            }

            // open this folder
            setNAVCurrentTab(2);
            gui.showStatusButtons();
            gui.ActivedNVATAB = 2;


            File[] corpus = listCorpus_inProjectFolder(projectFolder);

            listCorpus(corpus);
            GUI.status = 1;
            // add into memory
            if (corpus != null) {
                env.Parameters.corpus.LIST_ClinicalNotes.clear();
                for (File txtfile : corpus) {
                    if (txtfile == null) {
                        continue;
                    }
                    // env.clinicalNoteList.CorpusLib.addTextFile(txtfile);
                    env.clinicalNoteList.CorpusStructure cs = new env.clinicalNoteList.CorpusStructure();
                    cs.file = txtfile;

                    env.Parameters.corpus.LIST_ClinicalNotes.add(cs);

                }

                showTextFiles_inComboBox();

                gui.getContentRenderer().showFirstFile_of_corpus(fileName, false);
            }

            // update screen
            gui.jCardcontainer_interactive.setVisible(true);

            gui.updateScreen_for_variables();

            // load saved annotations from "saved" folder
            resultEditor.reloadSavedAnnotations.Reload.load(gui);

            gui.showAnnotationCategoriesInTreeView_CurrentArticle();
            gui.reavtiveMainPanel();

            // display the editor panel as default
            if (!((resultEditor.customComponents.ExpandablePanel_editor) NavigationPanel_editor)
                    .isExtended()) {
                ((resultEditor.customComponents.ExpandablePanel_editor) NavigationPanel_editor)
                        .afterMousePressed();
                ((resultEditor.customComponents.ExpandablePanel_editor) NavigationPanel_editor)
                        .setNormalColor();
            }

            // Update several status buttons on screen by these project
            // parameters.
            // There are several status buttons on the status bar at the button
            // of
            // eHOST window, such as Oracle status button, Diff status button,
            // etc.
            gui.updateGUI_byProjectParameters();

            gui.getContentRenderer().setReviewMode(reviewmode.ANNOTATION_MODE);

            ((navigatorContainer.TabPanel) NavigationPanel1).setTab_All();

            String annotator_name = resultEditor.annotator.Manager.getAnnotatorName_OutputOnly();
            AssignmentsScreen assignmentsScreen = gui.getAssignmentsScreen(annotator_name);
            // String annotator_id =
            // resultEditor.annotator.Manager.getAnnotatorID_outputOnly(); NOT
            // SAME AS AA USER ID
            // assignmentsScreen.updateAssignments(); THIS DISPLAYS THE ASGS,
            // but then asg selects do not show correctly in the results editor

            if ((env.Parameters.OracleStatus.visible == false)
                    || (env.Parameters.OracleStatus.sysvisible == false)) {

                if ((env.Parameters.OracleStatus.sysvisible == false)) {
                    env.Parameters.oracleFunctionEnabled = false;
                }

                gui.jLabel_infobar_FlagOfOracle.setVisible(false);
            } else {
                gui.jLabel_infobar_FlagOfOracle.setVisible(true);
            }
            gui.infoBarManager.setInfoBarTarget(gui.infoBarTarget);
            gui.infoBarManager.updateInfoBar("<html><b>Current Project:</b>  <font color=blue> <a  href=''>"
                    + projectFolder.getAbsolutePath() + "</a></font>.</html>");

        } catch (Exception ex) {
            System.out.println("error 1204031721");
            ex.printStackTrace();
        }
        GUI.status = 3;

    }

    /**
     * Shows text files in the combo box
     */
    public void showTextFiles_inComboBox() {
        try {
            gui.jComboBox_InputFileList.removeAllItems();

            // show inputted file into the combobox
            for (env.clinicalNoteList.CorpusStructure corpusfile : Parameters.corpus.LIST_ClinicalNotes) {
                if (corpusfile == null)
                    continue;
                if (corpusfile.file == null)
                    continue;
                gui.jComboBox_InputFileList.addItem(corpusfile.file.getName());
            }

            if (gui.jComboBox_InputFileList.getItemCount() > 0) {
                gui.jComboBox_InputFileList.setSelectedIndex(0);
            }
        } catch (Exception ex) {
            System.out.println("ERROR 1204031722::");
        }
    }

    /**
     * Gets the file ID map
     *
     * @return The file ID map
     */
    public HashMap<String, Integer> getFileIdMap() {
        return gui.fileIdMap;
    }

    protected void goBackToProjectList() {
        gui.setFlag_allowToAddSpan(false); // cancel possible operation of adding
        // new span
        // if user modified something of this current, ask user whether they
        // want to save changes or not.
        if (gui.modified) {
            // get user's decision
            boolean yes_no = gui.popDialog_Asking_ChangeSaving();

            // call saving function if needed.
            if (yes_no) {
                gui.saveto_originalxml();
            }
        }

        // release project lock
        ProjectLock lock = new ProjectLock(env.Parameters.WorkSpace.CurrentProject.getAbsolutePath());
        lock.releaseLock();

        gui.hideStatusButtons();

        // close protential consensus mode
        gui.getContentRenderer().setReviewMode(reviewmode.OTHERS);

        ((navigatorContainer.TabPanel) gui.NavigationPanel1).setTab_onlyProject();

        // go to tab which has the list of project
        setNAVCurrentTab(1);
        gui.jCardcontainer_interactive.setVisible(false);

        // save configure setting for current project which you just left
        gui.config_saveProjectSetting();

        // set current project as NULL to indicate that you go back to
        // workspace level and there is no project got selected
        env.Parameters.WorkSpace.CurrentProject = null;
        gui.infoBarManager.showDefaultInfo();
    }

    /**
     * by users' click, dicide which navigator menu item got selected and then
     * active related panel.
     *
     * @param i 0 means first panel, 1 means second panel
     */
    public void setNAVCurrentTab(int i) {
        JPanel jPanel_NAV_CardContainer = gui.jPanel_NAV_CardContainer;
        JPanel NavigationPanel1 = gui.NavigationPanel1;
        CardLayout card = (CardLayout) jPanel_NAV_CardContainer.getLayout();
        switch (i) {
            case 2:
                // ((ResultEditor.CustomComponents.NagivationMenuItem)jPanel_SelectFiles).setSelected();
                // ((ResultEditor.CustomComponents.NagivationMenuItem)jPanel_AnnotationsAndMarkables).setNormal();
                card.show(jPanel_NAV_CardContainer, "card3");
                gui.ActivedNVATAB = 2;
                break;
            case 3:
                // ((ResultEditor.CustomComponents.NagivationMenuItem)jPanel_SelectFiles).setNormal();
                // ((ResultEditor.CustomComponents.NagivationMenuItem)jPanel_AnnotationsAndMarkables).setSelected();

                card.show(jPanel_NAV_CardContainer, "card2");
                ((navigatorContainer.TabPanel) NavigationPanel1).setTabActived("Navigator");
                break;
            case 1:
                card.show(jPanel_NAV_CardContainer, "card4");
                gui.ActivedNVATAB = 1;
                gui.display_refreshNAV_WorkSpace();
                break;
        }
    }

}
