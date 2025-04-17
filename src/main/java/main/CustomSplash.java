package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Original splashscreen using java build-in SplashScreen. While it's part of the standard Java API, it does have several limitations and reliability issues.
 * So, here using a reimplemented CustomSplash instead.
 *
 * The {@code CustomSplash} class represents a splash screen that can be used to display
 * an introductory loading screen for an application. It includes features such as:
 * - Displaying an image or a default message as the splash background.
 * - Showing a version string overlayed on the splash screen.
 * - Providing a progress bar and status messages to indicate loading progress.
 * - Allowing the splash screen to close automatically or manually by user interaction.
 * <p>
 * Usage:
 * 1. Instantiate the class with the desired image path and version string:
 * {@code CustomSplash splash = new CustomSplash("/path/to/image.png", "1.0");}
 * 2. Call {@link #show()} to display the splash screen.
 * 3. Optionally update the status or progress using {@link #updateStatus(String)}
 * or {@link #updateProgress(int)}.
 * 4. Close the splash screen manually by calling {@link #closeSplash()}.
 * <p>
 * Note: The splash screen will auto-close after a set timeout (default 10 seconds)
 * if not manually closed.
 *
 * @author Jianlins
 */
public class CustomSplash {
    private final JWindow splashWindow;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    private boolean closeRequested = false;

    public CustomSplash(String imagePath, String versionString) {
        splashWindow = new JWindow();

        // Main panel with BorderLayout
        JPanel content = new JPanel(new BorderLayout());
//        content.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Load and display the splash image
        ImageIcon splashImage;
        try {
            // Try loading from resources first
            splashImage = new ImageIcon(getClass().getResource(imagePath));
            if (splashImage.getIconWidth() <= 0) {
                // If resource not found, try from file system
                splashImage = new ImageIcon(imagePath);
            }
        } catch (Exception e) {
            // Fallback to a blank panel with text if image can't be loaded
            splashImage = null;
        }

        // Create a layered pane for the image and overlaid components
        JLayeredPane layeredPane = new JLayeredPane();
        JLabel imageLabel;
        int imageWidth, imageHeight;

        if (splashImage != null && splashImage.getIconWidth() > 0) {
            imageLabel = new JLabel(splashImage);
            imageWidth = splashImage.getIconWidth();
            imageHeight = splashImage.getIconHeight();
        } else {
            imageLabel = new JLabel("Application Loading...");
            imageLabel.setFont(new Font("Arial", Font.BOLD, 24));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageWidth = 400;
            imageHeight = 300;
        }

        // Size the layered pane to match the image
        layeredPane.setPreferredSize(new Dimension(imageWidth, imageHeight));

        // Position and add the image label
        imageLabel.setBounds(0, 0, imageWidth, imageHeight);
        layeredPane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);

        // Status label at the bottom
        statusLabel = new JLabel("Initializing...");
        statusLabel.setForeground(new Color(113, 113, 115));
        statusLabel.setBounds(10, imageHeight - 40, imageWidth - 20, 20);
        layeredPane.add(statusLabel, JLayeredPane.PALETTE_LAYER);

        JLabel versionLabel = new JLabel(versionString);
        versionLabel.setFont(new Font("Impact", Font.ROMAN_BASELINE, 13));
        versionLabel.setForeground(new Color(132, 132, 135));

        // Set the position of the version label at coordinates (425, 355)
        // Height and width are set to accommodate the text
        versionLabel.setBounds(420, 348, 100, 25);

        // Add the version label to the palette layer (above the default layer)
        layeredPane.add(versionLabel, JLayeredPane.PALETTE_LAYER);

        // Progress bar overlay at the bottom of the image
        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setBounds(10, imageHeight - 20, imageWidth - 20, 15);
        progressBar.setOpaque(false);
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)); // 20% opacity (80% transparent)
                super.paintDeterminate(g2d, c);
                g2d.dispose();
            }

            @Override
            protected void paintIndeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f)); // 20% opacity (80% transparent)
                super.paintIndeterminate(g2d, c);
                g2d.dispose();
            }
        });

        layeredPane.add(progressBar, JLayeredPane.PALETTE_LAYER);

        // Add the layered pane to the content panel
        content.add(layeredPane, BorderLayout.CENTER);

        // Add a click handler to close splash screen
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeSplash();
            }
        });

        // Add a "click to close" label
        JLabel clickToClose = new JLabel("Click anywhere to close");
        clickToClose.setHorizontalAlignment(SwingConstants.CENTER);
        clickToClose.setFont(new Font("Arial", Font.ITALIC, 10));
        content.add(clickToClose, BorderLayout.NORTH);

        splashWindow.setContentPane(content);
        splashWindow.pack();
        splashWindow.setLocationRelativeTo(null);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            splashWindow.setVisible(true);

            // Auto-close after 10 seconds
            new Thread(() -> {
                try {
                    // Wait for 10 seconds or until manually closed
                    boolean closed = closeLatch.await(10, TimeUnit.SECONDS);
                    if (!closed) {
                        closeSplash();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    public void updateStatus(String status) {
        if (closeRequested) return;

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }

    public void updateProgress(int value) {
        if (closeRequested) return;

        SwingUtilities.invokeLater(() -> {
            if (progressBar.isIndeterminate()) {
                progressBar.setIndeterminate(false);
            }
            progressBar.setValue(value);
        });
    }

    public void closeSplash() {
        closeRequested = true;
        SwingUtilities.invokeLater(() -> {
            splashWindow.dispose();
            closeLatch.countDown();
        });
    }

    public boolean isClosed() {
        return closeRequested || !splashWindow.isVisible();
    }

    public void waitForClose() throws InterruptedException {
        closeLatch.await();
    }
}