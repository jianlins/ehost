/*
 * Manager.java
 * Created on Mar 25, 2011, 1:00:40 AM
 */

package graphicsReport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * This is the panel that we named "report pane". It's the panel that lists all
 * report function as buttons.
 *
 * Currently, we have three report functions:
 * 1. Graph Reports of Position Indicators
 * 2. Annotator Performance
 * 3. Open Existing Reports in Browser
 *
 * @author  Jianwei Chris Leng
 * @since   Created on Mar 25, 2011, 1:00:40 AM, Refactored on Apr 18, 2025 by jianlins
 */
public class Manager extends JPanel {

    // Parent components
    private userInterface.GUI gui;

    // Report components
    private report.iaaReport.IAA iaaReport;
    private GraphicsReport_PositionIndicators positionIndicatorsReport;

    // UI Components
    private JButton jButton1;
    private JButton jButton2;
    private JButton jButton3;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;

    // Colors and styling
    private static final Color BACKGROUND_COLOR = new Color(240, 240, 241);
    private static final Color BORDER_COLOR = Color.BLACK;
    private static final int VERTICAL_GAP = 10;
    private static final int HORIZONTAL_GAP = 10;
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 30;

    /**
     * Creates new Manager panel
     *
     * @param _gui The pointer which links to the current parent GUI
     */
    public Manager(userInterface.GUI _gui) {
        this.gui = _gui;
        createComponents();
        configureComponents();
        layoutComponents();
        addEventHandlers();
    }

    /**
     * Creates and initializes all UI components
     */
    private void createComponents() {
        // Buttons
        jButton1 = new JButton("Graph Reports of Position Indicators");
        jButton2 = new JButton("Annotator Performance");
        jButton3 = new JButton("Open Existing Reports in Browser.");

        // Labels
        jLabel1 = new JLabel("Graph report of position indicators to multiple documents.");
        jLabel2 = new JLabel("Generate IAA Reports for Annotator Performance");
        jLabel3 = new JLabel("Open previously generated IAA reports in a browser window.");
    }

    /**
     * Configures properties for all components
     */
    private void configureComponents() {
        // Main panel configuration
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 15, 15, 15));
        
        // Configure buttons with consistent size
        Dimension buttonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        jButton1.setPreferredSize(buttonSize);
        jButton1.setMaximumSize(buttonSize);
        jButton1.setMinimumSize(buttonSize);
        
        jButton2.setPreferredSize(buttonSize);
        jButton2.setMaximumSize(buttonSize);
        jButton2.setMinimumSize(buttonSize);
        
        jButton3.setPreferredSize(buttonSize);
        jButton3.setMaximumSize(buttonSize);
        jButton3.setMinimumSize(buttonSize);
    }

    /**
     * Adds all components to their containers using BoxLayout
     */
    private void layoutComponents() {
        // Set the main panel layout to BoxLayout with vertical alignment
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Add the report items with spacing between them
        add(createReportItem(jButton1, jLabel1));
        add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        
        add(createReportItem(jButton2, jLabel2));
        add(Box.createRigidArea(new Dimension(0, VERTICAL_GAP)));
        
        add(createReportItem(jButton3, jLabel3));
        
        // Add some glue at the bottom to push everything to the top
        add(Box.createVerticalGlue());
    }
    
    /**
     * Creates a panel containing a button and its description label
     * 
     * @param button The button component
     * @param label The description label
     * @return A JPanel containing the components
     */
    private JPanel createReportItem(JButton button, JLabel label) {
        // Create panel with horizontal BoxLayout
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Add button
        panel.add(button);
        
        // Add gap between button and label
        panel.add(Box.createRigidArea(new Dimension(HORIZONTAL_GAP, 0)));
        
        // Add label - make it align to the left and take all available space
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        
        // Make the panel aligned properly and take full width
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        return panel;
    }

    /**
     * Adds event handlers to interactive components
     */
    private void addEventHandlers() {
        jButton1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                openPositionIndicatorsReport();
            }
        });

        jButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                openAnnotatorPerformanceReport();
            }
        });

        jButton3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                openExistingReportsInBrowser();
            }
        });
    }

    /**
     * Opens the Position Indicators report
     */
    private void openPositionIndicatorsReport() {
        try {
            if (positionIndicatorsReport != null) {
                positionIndicatorsReport.dispose();
            }

            positionIndicatorsReport = new GraphicsReport_PositionIndicators(gui);
            positionIndicatorsReport.setVisible(true);

            logSuccess("the annotation mapping report system");
        } catch (Exception ex) {
            logError("annotation mapping system", ex);
        }
    }

    /**
     * Opens the Annotator Performance report
     */
    private void openAnnotatorPerformanceReport() {
        try {
            if (iaaReport != null) {
                iaaReport.dispose();
            }

            iaaReport = new report.iaaReport.IAA(gui);
            iaaReport.setVisible(true);

            logSuccess("IAA report");
        } catch (Exception ex) {
            logError("IAA report system", ex);
        }
    }

    /**
     * Opens existing reports in the browser
     */
    private void openExistingReportsInBrowser() {
        if (!gui.infoBarTarget.isEmpty()) {
            File reportFile = new File(new File(gui.infoBarTarget, "reports"), "index.html");
            if (reportFile.exists()) {
                if (openReportInBrowser(reportFile)) {
                    jLabel3.setText("Open report file: " + reportFile.getAbsolutePath());
                }
            } else {
                jLabel3.setText("The report file does not exist.");
            }
        }
    }

    /**
     * Opens a report file in the default browser
     *
     * @param reportFile The report file to open
     * @return true if successful, false otherwise
     */
    private boolean openReportInBrowser(File reportFile) {
        try {
            Desktop.getDesktop().browse(reportFile.toURI());
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Could not open browser: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Logs an error with consistent formatting
     *
     * @param componentName Name of the component that failed
     * @param ex The exception that was thrown
     */
    private void logError(String componentName, Exception ex) {
        log.LoggingToFile.log(Level.SEVERE,
                "Fail to load the form GUI of " + componentName + " into current tab.\n" +
                        "\tError Details: " + ex.toString());
    }

    /**
     * Logs a success message with consistent formatting
     *
     * @param componentName Name of the component that was loaded successfully
     */
    private void logSuccess(String componentName) {
        log.LoggingToFile.log(Level.FINEST,
                "Loaded the GUI of " + componentName + " into current tab as a form.");
    }
}