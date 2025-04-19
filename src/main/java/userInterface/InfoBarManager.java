package userInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Timer;

/**
 * Manages the information bar functionality for the GUI.
 * Because additional functionality is added, isolating the code for infoLabel is better.
 *
 * @author jianlins
 */
public class InfoBarManager {


    private final JLabel jLabel_infobar;
    private String infoBarTarget = "server";
    private Timer hoverTimer;


    private String restfulServerURL="";

    public InfoBarManager(String restfulServerURL) {
        this.jLabel_infobar = new JLabel();
        jLabel_infobar.setBackground(new Color(183, 183, 183));
        jLabel_infobar.setFont(new Font("Calibri", 0, 11)); // NOI18N
        jLabel_infobar.setMinimumSize(new Dimension(400, 16));
        jLabel_infobar.setPreferredSize(new Dimension(400, 16));
        jLabel_infobar.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        this.restfulServerURL=restfulServerURL;
        if (restfulServerURL=="")
            showDefaultInfo();
        else {
            updateInfoBar("<html>Welcome to eHOST!&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n<a href=''><span style=\"color:rgb(58,95,147)\">Visit the eHOST Control Server</span></a></html>");
            // Make the label opaque to ensure proper rendering of the HTML
            jLabel_infobar.setOpaque(false);
            // Add mouse listener to handle click events
            jLabel_infobar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        addMouseEvents();
    }

    public void addMouseEvents(){
        jLabel_infobar.setOpaque(false);
        // Add mouse listener to handle click events
        jLabel_infobar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jLabel_infobar.addMouseListener(new MouseAdapter() {


            @Override
            public void mouseClicked(MouseEvent e) {
                if (infoBarTarget=="server") {
                    try {
                        Desktop.getDesktop().browse(new URI(restfulServerURL));
                        // Disable the link after clicking
                        showDefaultInfo();
                        jLabel_infobar.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        infoBarTarget = "";
                    } catch (IOException | URISyntaxException ex) {
                        JOptionPane.showMessageDialog(null,
                                "Could not open browser: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }else if (!infoBarTarget.isEmpty()){
                    File reportFile=new File(new File(infoBarTarget, "reports"), "index.html");
                    if (reportFile.exists()) {
                        try {
                            Desktop.getDesktop().browse(reportFile.toURI());
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null,
                                    "Could not open browser: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }else{
                    showDefaultInfo();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Cancel the timer if it's running
                if (hoverTimer != null && hoverTimer.isRunning()) {
                    hoverTimer.stop();
                }
                jLabel_infobar.setToolTipText(null);
            }


            @Override
            public void mouseEntered(MouseEvent e) {
                // Cancel the timer if it's running
                if (infoBarTarget!="server" && !infoBarTarget.isEmpty()) {
                    // Create and start a timer with 2-second delay
                    if (hoverTimer != null && hoverTimer.isRunning()) {
                        hoverTimer.stop();
                    }

                    hoverTimer = new javax.swing.Timer(1000, evt -> {
                        jLabel_infobar.setToolTipText("Click to open the report in browser");
                        // Force tooltip to show immediately
                        ToolTipManager.sharedInstance().mouseMoved(
                                new MouseEvent(jLabel_infobar, MouseEvent.MOUSE_MOVED,
                                        System.currentTimeMillis(), 0,
                                        e.getX(), e.getY(), 0, false));
                    });
                    hoverTimer.setRepeats(false);
                    hoverTimer.start();
                }
            }

        });

//        jLabel_infobar.addMouseMotionListener(new MouseMotionAdapter() {
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                if (infoBarTarget!="server" && !infoBarTarget.isEmpty()) {
//                    // Create and start a timer with 2-second delay
//                    if (hoverTimer != null && hoverTimer.isRunning()) {
//                        hoverTimer.stop();
//                    }
//
//                    hoverTimer = new javax.swing.Timer(1000, evt -> {
//                        jLabel_infobar.setToolTipText("Click to open the report in browser");
//                        // Force tooltip to show immediately
//                        ToolTipManager.sharedInstance().mouseMoved(
//                                new MouseEvent(jLabel_infobar, MouseEvent.MOUSE_MOVED,
//                                        System.currentTimeMillis(), 0,
//                                        e.getX(), e.getY(), 0, false));
//                    });
//                    hoverTimer.setRepeats(false);
//                    hoverTimer.start();
//                }
//            }
//        });
    }


    public String getInfoBarTarget() {
        return infoBarTarget;
    }

    public void setInfoBarTarget(String infoBarTarget) {
        this.infoBarTarget=infoBarTarget;
    }


    public void updateInfoBar(String message) {
        jLabel_infobar.setText(message);
    }

    public void showDefaultInfo() {
        updateInfoBar("<html>Welcome to eHOST!</html>");
        if (!restfulServerURL.isEmpty())
            setInfoBarTarget("server");
        else
            setInfoBarTarget("");
    }

    public void clearInfoBar() {
        jLabel_infobar.setText("");
    }

    public String getRestfulServerURL() {
        return restfulServerURL;
    }

    public void setRestfulServerURL(String restfulServerURL) {
        this.restfulServerURL = restfulServerURL;
    }

    public JLabel getjLabel_infobar() {
        return jLabel_infobar;
    }
}