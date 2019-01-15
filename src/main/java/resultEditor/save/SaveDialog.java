//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package resultEditor.save;

import env.Parameters;
import env.Parameters.corpus;
import env.clinicalNoteList.CorpusStructure;
import io.excel.ExcelIO;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import resultEditor.annotations.Article;
import resultEditor.annotations.Depot;
import userInterface.GUI;

public class SaveDialog extends JFrame {
    protected GUI gui;
    protected static String outputpath = null;
    private static Icon notdone;
    private static Icon done;
    protected static Vector<SaveDialog.Filelist> filelist = new Vector();
    public static boolean outputMainBodyOnly = false;
    private JButton jButton2;
    private JButton jButton_save;
    private JButton jButton_save1;
    private JButton jButton_save2;
    private JCheckBox jCheckBox_mainbodyOnly;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private static JLabel jLabel_done;
    private static JLabel jLabel_notdone;
    protected static JList jList1;
    private JPanel jPanel3;
    private JPanel jPanel_saveAsExcel;
    private JPanel jPanel_saveAsXML;
    private JPanel jPanel_statusbar;
    private JScrollPane jScrollPane1;
    private JTabbedPane jTabbedPane1;
    private JTextField jTextField1;

    public SaveDialog(GUI gui) {
        this.setResizable(false);
        this.setTitle("Output Annotations to XML files...");
        this.setAlwaysOnTop(true);
        this.setDefaultCloseOperation(0);
        this.initComponents();
        done = jLabel_done.getIcon();
        notdone = jLabel_notdone.getIcon();
        this.gui = gui;
        filelist.clear();
        this.clearList();
        if (outputpath == null) {
            this.jTextField1.setText("." + File.separator + "output");
        } else {
            this.jTextField1.setText(outputpath);
        }

        filelist.clear();
        filelist = this.buildLocalFilelist();
        listdisplay();
        this.setLocationRelativeTo(gui);
    }

    private void clearList() {
        Vector c = new Vector();
        jList1.setListData(c);
    }

    public static void listdisplay() {
        Vector<Object> listentry = new Vector();

        iListEntry i;
        for (Iterator var1 = filelist.iterator(); var1.hasNext(); listentry.add(i)) {
            SaveDialog.Filelist textsource = (SaveDialog.Filelist) var1.next();
            String filename = textsource.textsourcefile.getName() + ".knowtator.xml";
            int annotationsAmount = countAnnotations(textsource.textsourcefile.getName());
            i = new iListEntry(filename, annotationsAmount, notdone, done, textsource.status);
            if (textsource.wantOutput) {
                i.isCheckBoxSelected = true;
            } else {
                i.isCheckBoxSelected = false;
            }
        }

        jList1.setListData(listentry);
        jList1.setCellRenderer(new iListCellRenderer());
    }

    private void saveAsExcel() {
        this.jButton_save2.setEnabled(false);
        this.saveAsExcel_outputToExcel();
        this.jButton_save2.setEnabled(true);
    }

    private void saveAsExcel_outputToExcel() {
        ExcelIO OutputToExcel = new ExcelIO();
        OutputToExcel.save();
    }

    private static int countAnnotations(String textsourcefilename) {
        Depot depot = new Depot();
        Article article = Depot.getArticleByFilename(textsourcefilename);
        if (article == null) {
            return 0;
        } else {
            return article.annotations == null ? 0 : article.annotations.size();
        }
    }

    private Vector<SaveDialog.Filelist> buildLocalFilelist() {
        Vector<CorpusStructure> textsources = corpus.LIST_ClinicalNotes;
        Vector<SaveDialog.Filelist> lists = new Vector();
        Iterator var3 = textsources.iterator();

        while (var3.hasNext()) {
            CorpusStructure textsource = (CorpusStructure) var3.next();
            SaveDialog.Filelist list = new SaveDialog.Filelist();
            list.textsourcefile = textsource.file;
            list.status = 0;
            lists.add(list);
        }

        return lists;
    }

    public static void setCheckBoxSelectedStatus(String output_filename, boolean flag) {
        if (filelist != null) {
            Iterator var2 = filelist.iterator();

            while (var2.hasNext()) {
                SaveDialog.Filelist file = (SaveDialog.Filelist) var2.next();
                if (file != null) {
                    String filename = file.textsourcefile.getName().trim();
                    if (filename.equals(output_filename.trim())) {
                        file.wantOutput = flag;
                        return;
                    }
                }
            }

        }
    }

    private void initComponents() {
        jLabel_notdone = new JLabel();
        jLabel_done = new JLabel();
        this.jTabbedPane1 = new JTabbedPane();
        this.jPanel_saveAsXML = new JPanel();
        this.jScrollPane1 = new JScrollPane();
        jList1 = new JList();
        this.jTextField1 = new JTextField();
        this.jLabel1 = new JLabel();
        this.jLabel2 = new JLabel();
        this.jButton_save1 = new JButton();
        this.jButton_save = new JButton();
        this.jCheckBox_mainbodyOnly = new JCheckBox();
        this.jPanel_saveAsExcel = new JPanel();
        this.jButton_save2 = new JButton();
        this.jLabel3 = new JLabel();
        this.jPanel_statusbar = new JPanel();
        this.jPanel3 = new JPanel();
        this.jButton2 = new JButton();
        jLabel_notdone.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("res/radiobutton_unchecked_pressed.png")));
        jLabel_notdone.setText("<html>jLabel3------<br>dsd<br>dsdf</html>");
        jLabel_notdone.setVerticalTextPosition(1);
        jLabel_done.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("res/done.png")));
        jLabel_done.setText("jLabel3");
        this.setDefaultCloseOperation(3);
        this.setAlwaysOnTop(true);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                SaveDialog.this.formWindowClosing(evt);
            }
        });
        this.jTabbedPane1.setBorder(BorderFactory.createLineBorder(new Color(0, 51, 153)));
        GridBagLayout jPanel_saveAsXMLLayout = new GridBagLayout();
        jPanel_saveAsXMLLayout.columnWidths = new int[]{0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0};
        jPanel_saveAsXMLLayout.rowHeights = new int[]{0, 5, 0, 5, 0, 5, 0, 5, 0, 5, 0};
        this.jPanel_saveAsXML.setLayout(jPanel_saveAsXMLLayout);
        this.jScrollPane1.setHorizontalScrollBarPolicy(31);
        this.jScrollPane1.setVerticalScrollBarPolicy(22);
        jList1.setFont(new Font("Calibri", 0, 11));
        jList1.setModel(new AbstractListModel() {
            String[] strings = new String[]{"Item 1", "Item 2", "Item 3", "Item 4", "Item 5"};

            public int getSize() {
                return this.strings.length;
            }

            public Object getElementAt(int i) {
                return this.strings[i];
            }
        });
        jList1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                SaveDialog.this.jList1MouseClicked(evt);
            }
        });
        this.jScrollPane1.setViewportView(jList1);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = 1;
        gridBagConstraints.weightx = 1.0D;
        gridBagConstraints.weighty = 1.0D;
        gridBagConstraints.insets = new Insets(5, 0, 5, 5);
        this.jPanel_saveAsXML.add(this.jScrollPane1, gridBagConstraints);
        this.jTextField1.setFont(new Font("Calibri", 0, 11));
        this.jTextField1.setText("jTextField1");
        this.jTextField1.setBorder(BorderFactory.createEtchedBorder());
        this.jTextField1.setDisabledTextColor(new Color(0, 0, 0));
        this.jTextField1.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = 1;
        gridBagConstraints.insets = new Insets(5, 0, 0, 5);
        this.jPanel_saveAsXML.add(this.jTextField1, gridBagConstraints);
        this.jLabel1.setFont(new Font("Calibri", 0, 11));
        this.jLabel1.setText("Output Path:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = 1;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        this.jPanel_saveAsXML.add(this.jLabel1, gridBagConstraints);
        this.jLabel2.setFont(new Font("Calibri", 0, 11));
        this.jLabel2.setText("<html>Destination<br>files<br>for<br>XML<br>output:</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = 1;
        gridBagConstraints.anchor = 13;
        gridBagConstraints.insets = new Insets(5, 5, 30, 0);
        this.jPanel_saveAsXML.add(this.jLabel2, gridBagConstraints);
        this.jButton_save1.setFont(new Font("Calibri", 0, 11));
        this.jButton_save1.setText("Change Path");
        this.jButton_save1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SaveDialog.this.jButton_save1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = 2;
        this.jPanel_saveAsXML.add(this.jButton_save1, gridBagConstraints);
        this.jButton_save.setFont(new Font("Verdana", 0, 12));
        this.jButton_save.setText("Save");
        this.jButton_save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SaveDialog.this.jButton_saveActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = 2;
        this.jPanel_saveAsXML.add(this.jButton_save, gridBagConstraints);
        this.jCheckBox_mainbodyOnly.setText("Only save annotation main body, no attribute and relationship");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = 21;
        this.jPanel_saveAsXML.add(this.jCheckBox_mainbodyOnly, gridBagConstraints);
        this.jTabbedPane1.addTab("Save as XML ...", this.jPanel_saveAsXML);
        this.jButton_save2.setFont(new Font("Verdana", 0, 12));
        this.jButton_save2.setText("Save");
        this.jButton_save2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SaveDialog.this.jButton_save2ActionPerformed(evt);
            }
        });
        this.jLabel3.setText("Save annotations into a Excel file under the \"saved\" folder.");
        GroupLayout jPanel_saveAsExcelLayout = new GroupLayout(this.jPanel_saveAsExcel);
        this.jPanel_saveAsExcel.setLayout(jPanel_saveAsExcelLayout);
        jPanel_saveAsExcelLayout.setHorizontalGroup(jPanel_saveAsExcelLayout.createParallelGroup(Alignment.LEADING).addGroup(jPanel_saveAsExcelLayout.createSequentialGroup().addContainerGap().addComponent(this.jButton_save2, -2, 100, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.jLabel3).addContainerGap(189, 32767)));
        jPanel_saveAsExcelLayout.setVerticalGroup(jPanel_saveAsExcelLayout.createParallelGroup(Alignment.LEADING).addGroup(jPanel_saveAsExcelLayout.createSequentialGroup().addContainerGap().addGroup(jPanel_saveAsExcelLayout.createParallelGroup(Alignment.BASELINE).addComponent(this.jButton_save2).addComponent(this.jLabel3)).addContainerGap(352, 32767)));
        this.jTabbedPane1.addTab("Save as Excel", this.jPanel_saveAsExcel);
        this.getContentPane().add(this.jTabbedPane1, "Center");
        this.jPanel_statusbar.setBackground(new Color(250, 250, 251));
        this.jPanel_statusbar.setBorder(BorderFactory.createMatteBorder(5, 0, 0, 0, new Color(250, 250, 251)));
        this.jPanel_statusbar.setLayout(new BorderLayout());
        this.jPanel3.setBackground(new Color(250, 250, 251));
        this.jPanel3.setLayout(new GridLayout());
        this.jButton2.setFont(new Font("Verdana", 0, 12));
        this.jButton2.setText("Close");
        this.jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                SaveDialog.this.jButton2ActionPerformed(evt);
            }
        });
        this.jPanel3.add(this.jButton2);
        this.jPanel_statusbar.add(this.jPanel3, "East");
        this.getContentPane().add(this.jPanel_statusbar, "South");
        this.pack();
    }

    private void jButton2ActionPerformed(ActionEvent evt) {
        this.closeDialog();
    }

    private void closeDialog() {
        this.gui.setEnabled(true);
        this.dispose();
    }

    private void formWindowClosing(WindowEvent evt) {
        this.closeDialog();
    }

    private void jButton_saveActionPerformed(ActionEvent evt) {
        outputMainBodyOnly = this.jCheckBox_mainbodyOnly.isSelected();
        this.beginSaveToDisk();
        outputMainBodyOnly = false;
    }

    private void jButton_save1ActionPerformed(ActionEvent evt) {
        this.selectOutputfolder();
    }

    private void jList1MouseClicked(MouseEvent evt) {
        int indexmax = jList1.getModel().getSize();
        int selectedindex = jList1.getSelectedIndex();
        if (selectedindex < indexmax && selectedindex >= 0) {
            if (filelist != null) {
                if (selectedindex < filelist.size()) {
                    SaveDialog.Filelist thisoutputfile = (SaveDialog.Filelist) filelist.get(selectedindex);
                    if (thisoutputfile != null) {
                        thisoutputfile.wantOutput = !thisoutputfile.wantOutput;

                        SaveDialog.Filelist file;
                        for (Iterator var5 = filelist.iterator(); var5.hasNext(); file.status = 0) {
                            file = (SaveDialog.Filelist) var5.next();
                        }

                        listdisplay();
                    }
                }
            }
        }
    }

    private void jButton_save2ActionPerformed(ActionEvent evt) {
        this.saveAsExcel();
    }

    private void beginSaveToDisk() {
        String path;
        if (outputpath == null) {
            path = "." + File.separator + "output";
        } else {
            path = outputpath;
        }

        this.jButton_save.setEnabled(false);
        Iterator var2 = filelist.iterator();

        while (var2.hasNext()) {
            SaveDialog.Filelist textsource = (SaveDialog.Filelist) var2.next();
            if (!textsource.wantOutput) {
                textsource.status = 1;
                listdisplay();
            } else {
                String textsourcefilename = textsource.textsourcefile.getName();
                this.outputxml(path, textsourcefilename);
                textsource.status = 2;
                listdisplay();
            }
        }

        this.jButton_save.setEnabled(true);
    }

    private void outputxml(String path, String textsourcefilename) {
        OutputToXML toxml = new OutputToXML(textsourcefilename, path, (Article) null);
        toxml.write();
    }

    private void selectOutputfolder() {
        JFileChooser filechooser = new JFileChooser();
        filechooser.setDialogTitle("Choose foloer to save ...");
        filechooser.setMultiSelectionEnabled(false);
        filechooser.setSelectedFile((File) null);
        filechooser.setFileSelectionMode(1);
        int re = filechooser.showSaveDialog(this);
        if (re == 0) {
            File f = filechooser.getSelectedFile();
            String thisoutputpath;
            if (!f.exists()) {
                thisoutputpath = filechooser.getCurrentDirectory().toString();
            } else {
                thisoutputpath = f.getAbsolutePath();
            }

            outputpath = thisoutputpath;
            this.jTextField1.setText(outputpath);
        }

    }

    class Filelist {
        File textsourcefile;
        int status;
        boolean wantOutput = true;

        Filelist() {
        }
    }

    class OutputfileListEntry {
        Icon icon;
        String lines;

        OutputfileListEntry() {
        }
    }
}
