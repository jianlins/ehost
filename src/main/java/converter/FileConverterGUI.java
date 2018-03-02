/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FileConverterGUI.java
 *
 * Created on Aug 4, 2010, 3:27:32 PM
 */
package converter;

import converter.fileConverters.iConversion;
import converter.params.iParameterSet;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.text.Document;
import preAnnotate.ExtensionFileFilter;

/**
 * This class extends the JPanel class and can easily be placed onto any gui,
 * by simply adding this JPanel to it.  This class will allow you to convert between
 * i2b2, knowtator XML, and pins file types.
 * @author Kyle
 */
public class FileConverterGUI extends javax.swing.JPanel implements iGUI
{
    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    ///The main processing thread
    private Thread running = null;

    //This object will perform the actual conversions, and will be used to
    //manage the conversoin files as well.
    private iConversion work = null;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /** Creates new form FileConverterGUI */
    public FileConverterGUI()
    {
        initComponents();

        //Clear out all temp files
        String separator = env.Parameters.isUnixOS ? "/" : "\\";
        File oldStuff = new File("temp" + separator + "currentConversion");
        deleteAll(oldStuff);

        //Disable the GUI
        disableGUIExceptChoices();

        //Put available formats in the 'from' combo box.
        fromChoice.addItem("");
        for (String convertable : TypeMatcher.getInitialChoices())
        {
            fromChoice.addItem(convertable);
        }

        //Disable toChoice so user can't select 'to' until they've selected from.
        toChoice.setEnabled(false);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Implemented from iGUI">
    /**
     * Set the progress of this GUI.
     * @param progress - number between 0 to 100
     * @param description - description of what happened.
     */
    public void setProgress(int progress, String description)
    {
        this.jProgressBar1.setValue(progress);
        this.jProgressBar1.setString(description);
        this.jProgressBar1.setStringPainted(true);
        if(progress == 100)
        {
            jProgressBar1.setVisible(false);
        }
        else
        {
            jProgressBar1.setVisible(true);
        }
    }
    /**
     * This method will output text.
     * @param output - text to output
     */
    public void Output(String output)
    {
        appendToTextPane(jTextPane1, output);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Methods">
    /**
     * Append text to a text pane.
     * @param toAppend - the textpane to append to
     * @param newText - the new tet that you want to append.
     */
    private void appendToTextPane(JTextPane toAppend, String newText)
    {
        try
        {
            String aString = newText;
            Document doc = toAppend.getDocument();

            // Move the insertion point to the end
            toAppend.setCaretPosition(doc.getLength());

            // Insert the text
            toAppend.replaceSelection(aString);

            // Convert the new end location
            // to view co-ordinates
            Rectangle r = toAppend.modelToView(doc.getLength());

            // Finally, scroll so that the new text is visible
            if (r != null)
            {
                toAppend.scrollRectToVisible(r);
            }
        }
        catch (Exception e)
        {
            //System.out.println("Failed to append text: " + e);
        }
    }
     /**
     * Open file dialog to allow user to select input files... multiple selections are allowed.
     */
    private ArrayList<File> openFileDialog(Vector<String> acceptable, String filter)
    {
        jFileChooser1.setSelectedFile(new File(""));
        File[] f = new File[1];
        f[0] = new File("");
        jFileChooser1.setSelectedFiles(f);

        //Allow multiple file selection.
        jFileChooser1.setMultiSelectionEnabled(true);
        jFileChooser1.setFileSelectionMode(JFileChooser.FILES_ONLY);
        // Add file filter
        String[] theExtensions = new String[acceptable.size()];
        for(int i = 0; i< acceptable.size(); i++)
        {
            theExtensions[i] = acceptable.get(i);
        }
        jFileChooser1.addChoosableFileFilter(new ExtensionFileFilter(
                theExtensions,
                filter));

        // Turn off 'All Files' capability of file chooser,
        // so only our custom filter is used.
        jFileChooser1.setAcceptAllFileFilterUsed(false);

        //Store the result so we know if the user selected ok or not.
        int result = jFileChooser1.showOpenDialog(this);

        //If the user did not approve the selection then just return.
        if (result != JFileChooser.APPROVE_OPTION)
        {
            return new ArrayList<File>();
        }
        //Otherwise get the list of selected files.
        File[] selFiles = jFileChooser1.getSelectedFiles();

        ArrayList<File> toReturn = new ArrayList<File>();
        for(File file: selFiles)
            toReturn.add(file);
        return toReturn;

    }
    /**
     * This method will recursively set enabled all components inside this component
     * @param component - the component to set enabled all components inside
     * @param set - true if we want all components enabled, false for disabled.
     */
    private void setEnabledAll(JComponent component, boolean set)
    {
        component.setEnabled(set);
        Component[] aList = component.getComponents();
        for (Component aComponent : aList)
        {
            aComponent.setEnabled(set);
            try
            {
                JComponent casted = (JComponent) aComponent;
                setEnabledAll(casted, set);
            }
            catch (Exception e)
            {
            }
        }
    }
    /**
     * This method will remove selected values from the passed in list. It will use
     * the current work conversion object.
     * @param sourceList - the list to remove the selected objects from
     * @param source - true if removing source files, false it removing convert files
     */
    private void removeSelectedFromList(JList sourceList)
    {
        //Get the selected values
        Vector<iParameterSet> toDelete = new Vector<iParameterSet>();

        //Loop through all selected values, cast them to list objects, and add them
        //to a list of toDelete objects.
        for(Object object: sourceList.getSelectedValues())
        {
            toDelete.add((iParameterSet)object);
        }

        //This will hold the listObjects
        Vector<iParameterSet> all = new Vector<iParameterSet>();

        all = work.removeFiles(toDelete);

        //set list data
        sourceList.setListData(all);
    }
    /**
     * Update the to JComboBox with updated choices of 'to' file types.  If the user
     * selected a from file type... all file types that the 'from' file type can convert
     * to will be put in the 'to' combo box as possible choices.
     */
    private void updateToChoices()
    {
        //Get the selected combo box choice.
        String selected = "";
        if (fromChoice.getSelectedItem() != null)
        {
            selected = fromChoice.getSelectedItem().toString();
        }

        //Get the 'to' choices based on the from choice
        String[] toChoices = TypeMatcher.getToByFrom(selected);

        //Make sure toChoices returned a result... and add it to the toChoice combo box.
        if(toChoices!=null)
        {
            addAllToCombo(toChoices, toChoice);
        }
    }

    /**
     * This method will set the conversion object based on user inputs.  Thie method will
     * extract information from the toChoice and fromChoice combo boxes and set the object
     * based on them.
     */
    private void setWork()
    {
        //If either choice is null, set the work object to null
        if(toChoice.getSelectedItem() == null || fromChoice.getSelectedItem() == null)
        {
            work = null;
        }
        //If the to and from choice are both selected then set the work(iConversion) object to be the
        //correct file converter
        else
        {
            work = TypeMatcher.getWorkerByToAndFrom(fromChoice.getSelectedItem().toString(),
                    toChoice.getSelectedItem().toString(), this);
        }

    }
    /**
     * Disable all GUI components except choice combo boxes.
     */
    private void disableGUIExceptChoices()
    {
        setEnabledAll(jTabbedPane1,false);
        setEnabledAll(jPanel4, false);
        setEnabledAll(jPanel15, true);
    }
    /*
     * Disable all GUI components
     */
    private void disableGUI()
    {
        setEnabledAll(jTabbedPane1,false);
        setEnabledAll(jPanel4, false);
        setEnabledAll(jPanel15, false);
    }
/*
     * Enable all GUI components
     */
    private void enableGUI()
    {
        setEnabledAll(jTabbedPane1,true);
        setEnabledAll(jPanel4, true);
        setEnabledAll(jPanel15, true);
    }
    /**
     * This method will set the tool tip text based on the listObject that is currently
     * being hovered over in a given list.  The tool tip will contain all of the missing
     * files that are required for conversion.
     *
     * @param convertList - the list that contains listObjects
     * @param evt - the mouse mouve event
     */
    private void setToolTip(JList convertList, java.awt.event.MouseEvent evt)
    {
       // Get the index of the item that the mouse is currently on top of.
        int index = convertList.locationToIndex(evt.getPoint());

        //If the index is negative one than we didn't find anything.
        if(index == -1)
        {
            convertList.setToolTipText(null);
            return;
        }
        //Get the bounds of the listOBject that of the index that the mouse is currently over
        //We need this because the .locationToIndex will return the nearest index to the mouse
        //event, and we only want it if the mouse is directly over it.
        Rectangle r = convertList.getCellBounds(index,index);

        //If the mouse event y value is greater than the max bounds of the rectangle
        //then the mouse is not over it, so set the tool tip to null and return.
        if(evt.getPoint().getY()> r.getMaxY())
        {
            convertList.setToolTipText(null);
            return;
        }

        // Get the selected listObject.
        iParameterSet item = (iParameterSet) convertList.getModel().getElementAt(index);

        //Create the tool tip based on the list of missing files.
        String toolTip = item.getToolTipText();
        convertList.setToolTipText(toolTip);

    }

    /**
     * Clear out previous choices and set new choices for a jComboBox, and set the combo
     * box to enabled.
     * @param choices - new choices for a combo box
     * @param box - the combo box to clear, add choices to, and enable.
     */
    private void addAllToCombo(String[] choices, JComboBox box)
    {
        box.removeAllItems();
        for (String allowed : choices)
        {
            box.addItem(allowed);
        }
        box.setEnabled(true);
    }
    /**
     * Open a dialog that will allow a user to select a directory to save files to.
     * @param textField - The absolute path of the directory will be placed
     * in this text field.
     */
    private void openSingleSaveDirectoryDialog(JTextField textField)
    {
        jFileChooser1.setMultiSelectionEnabled(false);
        jFileChooser1.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = jFileChooser1.showSaveDialog(this);
        //If the user did not approve the selection then just return.
        if (result != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        textField.setText(jFileChooser1.getSelectedFile().getAbsolutePath());
    }

    /**
     * This file will delete all files and subdirectories in a directory, and the directory itself.
     * @param file - the file/directory to clear out.
     */
    private void deleteAll(File file)
    {
        //If it's a directory first clear out all of the files inside of it.
        if (file.isDirectory())
        {
            for (File inDirectory : file.listFiles())
            {
                deleteAll(inDirectory);
            }
        }
        //delete the file or directory.
        file.delete();

    }
    /*
     * Update the GUI based on the user 'to' and 'from' choices.
     */
    private void updateGUIBasedOnChoices()
    {
        //If the chosen item is null then return
        if (toChoice.getSelectedItem() == null || fromChoice.getSelectedItem() == null)
        {
            disableGUIExceptChoices();
            return;
        }
        //If the 'to' choice is not "" then enable the tabbed pane without the sourcefiles tab
        else if (!toChoice.getSelectedItem().toString().equals("") && !fromChoice.getSelectedItem().toString().equals(""))
        {
            enableGUI();
            return;
        }
        //else disable the jtabbedpane1
        else
        {
            setEnabledAll(jTabbedPane1, false);
        }
    }
    //</editor-fold>
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        fromChoice = new JComboBox();
        jLabel6 = new javax.swing.JLabel();
        toChoice = new JComboBox();
        jPanel18 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        sourceFiles = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        sourceList = new JList();
        jPanel19 = new javax.swing.JPanel();
        addSourceButton = new javax.swing.JButton();
        removeSourceButton = new javax.swing.JButton();
        jPanel20 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        outputPath = new JTextField();
        browseOutputDirectory = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        startProcess = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPane1 = new JTextPane();
        jPanel16 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.GridLayout(1, 2, 5, 5));

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel17.setLayout(new java.awt.BorderLayout());

        jPanel15.setLayout(new java.awt.GridLayout(1, 4));

        jLabel5.setFont(new java.awt.Font("Calibri", 0, 13));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("From:");
        jPanel15.add(jLabel5);

        fromChoice.setFont(new java.awt.Font("Calibri", 0, 13));
        fromChoice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromChoiceActionPerformed(evt);
            }
        });
        jPanel15.add(fromChoice);

        jLabel6.setFont(new java.awt.Font("Calibri", 0, 13));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("To:");
        jPanel15.add(jLabel6);

        toChoice.setFont(new java.awt.Font("Calibri", 0, 13));
        toChoice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toChoiceActionPerformed(evt);
            }
        });
        jPanel15.add(toChoice);

        jPanel17.add(jPanel15, java.awt.BorderLayout.PAGE_START);

        jPanel5.add(jPanel17, java.awt.BorderLayout.CENTER);

        jPanel18.setLayout(new java.awt.BorderLayout());

        jPanel14.setBackground(new java.awt.Color(41, 119, 167));
        jPanel14.setForeground(new java.awt.Color(255, 255, 255));
        jPanel14.setLayout(new java.awt.BorderLayout());

        jLabel4.setBackground(new java.awt.Color(41, 119, 167));
        jLabel4.setFont(new java.awt.Font("Calibri", 1, 13));
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("FORMAT CONVERTER");
        jPanel14.add(jLabel4, java.awt.BorderLayout.CENTER);

        jPanel18.add(jPanel14, java.awt.BorderLayout.CENTER);

        jPanel5.add(jPanel18, java.awt.BorderLayout.PAGE_START);

        jPanel2.add(jPanel5, java.awt.BorderLayout.PAGE_START);

        jPanel11.setLayout(new java.awt.BorderLayout());

        jTabbedPane1.setFont(new java.awt.Font("Calibri", 0, 13));

        sourceFiles.setLayout(new java.awt.BorderLayout());

        sourceList.setFont(new java.awt.Font("Calibri", 0, 13));
        sourceList.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                sourceListMouseMoved(evt);
            }
        });
        jScrollPane1.setViewportView(sourceList);

        sourceFiles.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel19.setLayout(new java.awt.GridLayout(1, 3));

        addSourceButton.setFont(new java.awt.Font("Calibri", 0, 13));
        addSourceButton.setText("Add Files");
        addSourceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSourceButtonActionPerformed(evt);
            }
        });
        jPanel19.add(addSourceButton);

        removeSourceButton.setFont(new java.awt.Font("Calibri", 0, 13));
        removeSourceButton.setText("Remove");
        removeSourceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSourceButtonActionPerformed(evt);
            }
        });
        jPanel19.add(removeSourceButton);
        jPanel19.add(jPanel20);

        sourceFiles.add(jPanel19, java.awt.BorderLayout.PAGE_END);

        jTabbedPane1.addTab("Source Files", sourceFiles);

        jPanel11.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel12.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.GridLayout(3, 1));

        jPanel10.setLayout(new java.awt.BorderLayout());

        jLabel3.setFont(new java.awt.Font("Calibri", 0, 13));
        jLabel3.setText("Output Directory:");
        jPanel10.add(jLabel3, java.awt.BorderLayout.LINE_START);

        outputPath.setFont(new java.awt.Font("Calibri", 0, 13));
        jPanel10.add(outputPath, java.awt.BorderLayout.CENTER);

        browseOutputDirectory.setFont(new java.awt.Font("Calibri", 0, 13));
        browseOutputDirectory.setText("Browse...");
        browseOutputDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseOutputDirectoryActionPerformed(evt);
            }
        });
        jPanel10.add(browseOutputDirectory, java.awt.BorderLayout.LINE_END);

        jPanel4.add(jPanel10);

        jPanel13.setLayout(new java.awt.BorderLayout());

        startProcess.setFont(new java.awt.Font("Calibri", 0, 13));
        startProcess.setText("Go!");
        startProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startProcessActionPerformed(evt);
            }
        });
        jPanel13.add(startProcess, java.awt.BorderLayout.CENTER);

        jPanel4.add(jPanel13);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jProgressBar1.setFont(new java.awt.Font("Calibri", 0, 13));
        jPanel6.add(jProgressBar1, java.awt.BorderLayout.CENTER);

        jPanel4.add(jPanel6);

        jPanel12.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel12, java.awt.BorderLayout.PAGE_END);

        jPanel1.add(jPanel2);

        jPanel3.setLayout(new java.awt.BorderLayout());

        jTextPane1.setFont(new java.awt.Font("Calibri", 0, 13)); // NOI18N
        jTextPane1.setDisabledTextColor(new java.awt.Color(51, 51, 51));
        jTextPane1.setEnabled(false);
        jScrollPane3.setViewportView(jTextPane1);

        jPanel3.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel16.setBackground(new java.awt.Color(41, 119, 167));
        jPanel16.setLayout(new java.awt.BorderLayout());

        jLabel7.setBackground(new java.awt.Color(41, 119, 167));
        jLabel7.setFont(new java.awt.Font("Calibri", 1, 13));
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("CONVERSION OUTPUT");
        jPanel16.add(jLabel7, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel16, java.awt.BorderLayout.PAGE_START);

        jPanel1.add(jPanel3);

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    //<editor-fold defaultstate="collapsed" desc="Event Handling">
    /**
     * This wil be called when the 'start' button is pressed. This should start
     * the conversion process.
     *
     * @param evt
     */
    private void startProcessActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_startProcessActionPerformed
    {//GEN-HEADEREND:event_startProcessActionPerformed
        if (running == null || !running.isAlive())
        {
            this.disableGUI();
            running = new converterThread();
            running.start();
        }

    }//GEN-LAST:event_startProcessActionPerformed

    private void browseOutputDirectoryActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_browseOutputDirectoryActionPerformed
    {//GEN-HEADEREND:event_browseOutputDirectoryActionPerformed
        openSingleSaveDirectoryDialog(outputPath);
    }//GEN-LAST:event_browseOutputDirectoryActionPerformed

    /**
     * This will be called whenever an action is performed on the 'from' combo box.
     * This method will make sure that everything else in the GUI is in sync with the users
     * choice.
     * @param evt
     */
    private void fromChoiceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fromChoiceActionPerformed
    {//GEN-HEADEREND:event_fromChoiceActionPerformed
        
        sourceList.setListData(new Vector());
        setWork();
        updateToChoices();
        updateGUIBasedOnChoices();
        
    }//GEN-LAST:event_fromChoiceActionPerformed

    /**
     * This will be called whenever the toChoice combo box has an action performed.
     * It will catch any events where the JComboBox has a different value selected.
     * @param evt - the evt that was caught.
     */
    private void toChoiceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_toChoiceActionPerformed
    {//GEN-HEADEREND:event_toChoiceActionPerformed

        sourceList.setListData(new Vector());
        setWork();
        updateGUIBasedOnChoices();
        
         
    }//GEN-LAST:event_toChoiceActionPerformed
    /*
     * Add source files.
     */
    private void addSourceButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_addSourceButtonActionPerformed
    {//GEN-HEADEREND:event_addSourceButtonActionPerformed
        Vector<iParameterSet> all= new Vector<iParameterSet>();
        Vector<String> extensions = work.getExtensions();
        
        //Append periods before, cause that what the extension filter requires.
        for(int i = 0; i< extensions.size(); i++)
        {
            extensions.set(i, "." +extensions.get(i));
        }
        ArrayList<File> theFiles = this.openFileDialog(extensions, work.getFileDescription());
        for(File file: theFiles)
        {
            work.addFilesDirectly(file);
        }
        all = work.getAllEntries();
        sourceList.setListData(all);
    }//GEN-LAST:event_addSourceButtonActionPerformed
    
    /**
     * remove selected source files.
     * @param evt
     */
    private void removeSourceButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_removeSourceButtonActionPerformed
    {//GEN-HEADEREND:event_removeSourceButtonActionPerformed
        removeSelectedFromList(sourceList);
    }//GEN-LAST:event_removeSourceButtonActionPerformed

    /**
     * set tool tips for mouse movements on the source list.
     * @param evt
     */
    private void sourceListMouseMoved(java.awt.event.MouseEvent evt)//GEN-FIRST:event_sourceListMouseMoved
    {//GEN-HEADEREND:event_sourceListMouseMoved
        setToolTip(sourceList,evt);
    }//GEN-LAST:event_sourceListMouseMoved
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Class for Running Conversions">
    /**
     * This class is a private class that will run the actual conversions between
     * file types.
     */
    private class converterThread extends Thread
    {
        /**
         * The method call that will convert from one file type to another file type.
         */
        @Override
        public void run()
        {

            try
            {
                //Clear out previous results
                jTextPane1.setText("");
                work.convert(outputPath.getText());
                enableGUI();
            }
            //Catch any exceptions
            catch (Exception ex)
            {
                System.out.println(ex.getMessage());
                enableGUI();
            }
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Outdated Methods">
    /*****OUT OF USE*****
     * This method will redirect text that would go to the console(.println()) to
     * the jTextPane in this GUI.
     */
    private void reDirectConsole()
    {
        // New output Stream
        final OutputStream makeString = new OutputStream()
        {

            private StringBuilder string = new StringBuilder();

            /**
             * This will be called whenever a function attempts to print anything to the console.
             * It will be redirected to the JTextPane in this GUI.
             * @param b - the integer to append(cast to a char).
             */
            @Override
            public void write(int b) throws IOException
            {
                //Cast the int to a char
                this.string.append((char) b);

                //Add the char to a string
                String aString = new String();
                aString += (char) b;

                //Append the string to the text pane.
                appendToTextPane(jTextPane1, aString);

                //enable the gui
                enableGUI();
            }

            // Return the contents, and clear the string
            @Override
            public String toString()
            {
                String toReturn = this.string.toString();
                this.string = new StringBuilder();
                return toReturn;

            }
        };
        // Set default output stream to be the one we just made.
        final PrintStream toRead = new PrintStream(makeString);
        System.setOut(toRead);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="GUI Component Variables">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addSourceButton;
    private javax.swing.JButton browseOutputDirectory;
    private JComboBox fromChoice;
    private JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private JTextPane jTextPane1;
    private JTextField outputPath;
    private javax.swing.JButton removeSourceButton;
    private javax.swing.JPanel sourceFiles;
    private JList sourceList;
    private javax.swing.JButton startProcess;
    private JComboBox toChoice;
    // End of variables declaration//GEN-END:variables
    //</editor-fold>
}
