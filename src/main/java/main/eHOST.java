package main;
/*
 * The contents of this file are subject to the GNU GPL v3 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at URL http://www.gnu.org/licenses/gpl.html
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *
 * The Original Code is eHOST.
 *
 * The Initial Developer of the Original Code is University of Utah.
 * Copyright (C) 2009 - 2012.  All Rights Reserved.
 *
 * eHOST was developed by the Division of Epidemiology at the
 * University of Utah. Current information about eHOST can be located at
 * http://http://code.google.com/p/ehost/
 *
 * Categories:
 * Scientific/Engineering
 *
 * Intended Audience:
 * Science/Research
 *
 * User Interface:
 * Java Swing, Java AWT, Custom Components.
 *
 * Programming Language
 * Java
 *
 * Contributor(s):
 *   Jianwei "Chris" Leng <Chris.Leng@utah.edu> (Original Author), 2009-2012
 *   Kyle Anderson added the Verifier Function, 2010
 *   Annotation Admin Team added the Sync functions, 2011-2012
 *   Jianlins reimplement the loading process with multiple threads to speedup loading, 2025
 *
 */

import env.Parameters;
import log.LogCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import rest.server.EhostServerApp;
import rest.server.PropertiesUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The entrance class to load the GUI of eHOST and display the splash dialog
 * before the main frame finishing loading resource.
 *
 * @author Jianwei "Chris" Leng
 */
public class eHOST {
    public static Logger logger = LoggerFactory.getLogger(eHOST.class);
    private static CustomSplash splash;

    /**
     * Initial works, before finishing loading the GUI.
     */
    private static void initial() {
        // check whether current OS is a Mac OS
        // and switchpool.iniMSP.isUnixOS = true if its a Mac OS / Unix;
        // we use '/' as the path separator
        Parameters.isUnixOS = commons.OS.isMacOS();

        // Get config paths from PropertiesUtil
        String ehostconfighome = PropertiesUtil.getEhostConfighome();
        config.system.SysConf.loadSystemConfigure(ehostconfighome);
        logger.info("Now workspace=\t" + PropertiesUtil.getWorkspace());


        // set the flag, after loading configure information
        Parameters.isFirstTimeLoadingConfigureFile = false;

        // set the latest mention id to the module if XML output
        int latest_used_metion_id = Parameters.getLatestUsedMentionID();
        algorithmNegex.XMLMaker.set_mention_id_startpoint(latest_used_metion_id);
    }

    /**
     * Main method of eHOST project. It's the entrance that used to start to
     * create and display an instance of the primary GUI from class "GUI.java".
     */
    public static void main(String[] args) {
        VersionInfo.printVersionInfo();
        splash = new CustomSplash("/splash.png", VersionInfo.getVersion());
        splash.show();

        new Thread(() -> {
            InitializationManager initManager = new InitializationManager(splash, args);
            initManager.performInitializationSequence();
        }).start();
    }

    /**
     * Manages the application initialization process with visual progress feedback
     */
    public static class InitializationManager {
        private final CustomSplash splash;
        private final String[] args;
        private int currentProgress = 0;

        public InitializationManager(CustomSplash splash, String[] args) {
            this.splash = splash;
            this.args = args;
        }

        public void performInitializationSequence() {
            try {
                // Execute each step in sequence with specified minimum durations
                executeStep("Initializing configurations...", this::initConfigurations, 5, 10, 500);
                executeStep("Initializing settings...", this::initSettings, 15, 20, 500);

                if (Parameters.RESTFulServer) {
                    executeStep("Starting RESTful server in the background...", this::startRESTServer, 30, 50, 600);
                }

                executeStep("Loading GUI...", this::loadGUI, 70, 90, 1800);
            } catch (Exception e) {
                logger.error("Error during initialization sequence", e);
            } finally {
                splash.closeSplash();
            }
        }

        /**
         * Executes a single initialization step with status update, progress animation, and minimum duration.
         *
         * @param statusMessage The status message to display
         * @param step The initialization step to execute
         * @param startProgress The starting progress percentage for this step
         * @param endProgress The ending progress percentage for this step
         * @param minimumStepTimeMs The minimum time this step should take in milliseconds
         */
        private void executeStep(String statusMessage, InitializationStep step, int startProgress, int endProgress, long minimumStepTimeMs) {
            splash.updateStatus(statusMessage);

            // Update progress to start position
            updateProgressToValue(startProgress);

            long startTime = System.currentTimeMillis();
            try {
                // Execute the step
                step.execute();

                // Update progress bar to end position
                updateProgressWithAnimation(startProgress, endProgress);

                // Ensure minimum time has passed
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = minimumStepTimeMs - elapsedTime;
                if (remainingTime > 0) {
                    Thread.sleep(remainingTime);
                }
            } catch (Exception e) {
                logger.error("Error during initialization step: " + statusMessage, e);
                throw new RuntimeException(e);
            }
        }

        /**
         * Overloaded method with default minimum time of 500ms
         */
        private void executeStep(String statusMessage, InitializationStep step, int startProgress, int endProgress) {
            executeStep(statusMessage, step, startProgress, endProgress, 500);
        }

        /**
         * Updates progress immediately to a specific value without animation
         */
        private void updateProgressToValue(int targetProgress) {
            currentProgress = targetProgress;
            splash.updateProgress(currentProgress);
        }

        /**
         * Animates the progress bar from start value to end value
         */
        private void updateProgressWithAnimation(int startValue, int endValue) {
            try {
                // Animate progress bar smoothly between values
                int totalChange = endValue - startValue;
                int steps = 5; // Number of animation steps
                int progressStep = Math.max(1, totalChange / steps);

                currentProgress = startValue;
                while (currentProgress < endValue) {
                    currentProgress += progressStep;
                    currentProgress = Math.min(currentProgress, endValue);
                    splash.updateProgress(currentProgress);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Individual initialization steps - now focused purely on their tasks without timing concerns
        private void initConfigurations() throws Exception {
            // Use PropertiesUtil to handle all configuration initialization
            PropertiesUtil.initConfigsFromArgs(args);
        }

        private void initSettings() throws Exception {
            LogCleaner deleteLocker = new LogCleaner();
            deleteLocker.doit();
            initial();
        }

        private void startRESTServer() {
            // Start REST server in background thread
            new Thread(() -> {
                try {
                    String configPath = PropertiesUtil.getPropertiesPath();
                    String configLocation = Paths.get(configPath).toAbsolutePath().toUri().toString();
                    
                    // Load current properties to get the configured port
                    Properties currentProperties = new Properties();
                    try (FileInputStream fis = new FileInputStream(configPath)) {
                        currentProperties.load(fis);
                    } catch (IOException e) {
                        logger.error("Failed to load properties file", e);
                    }
                    
                    // Get the currently configured port or default to 8080
                    int configuredPort = Integer.parseInt(currentProperties.getProperty("server.port", "8080"));
                    
                    boolean serverStarted = false;
                    int attempts = 0;
                    int maxAttempts = 5;
                    int port = configuredPort;
                    
                    while (!serverStarted && attempts < maxAttempts) {
                        attempts++;
                        try {
                            SpringApplication app = new SpringApplication(EhostServerApp.class);
                            Properties properties = new Properties();
                            properties.setProperty("spring.config.location", configLocation);
                            properties.setProperty("server.port", String.valueOf(port));
                            app.setDefaultProperties(properties);
                            
                            ConfigurableApplicationContext context = app.run(args);
                            showRESTfulInfo(context);
                            serverStarted = true;

                        } catch (Exception e) {
                            if (e.getCause() != null && e.getCause().getMessage() != null && 
                                    e.getCause().getMessage().contains("already in use")) {
                                // Port is already in use, try the next port
                                logger.warn("Port {} is already in use, trying next port", port);
                                port++;
                                // If we're using a different port than configured, update the properties file
                                if (port != configuredPort) {
                                    try {
                                        // Load properties again to get all current settings
                                        Properties updatedProperties = new Properties();
                                        try (FileInputStream fis = new FileInputStream(configPath)) {
                                            updatedProperties.load(fis);
                                        }

                                        // Update the port and save
                                        updatedProperties.setProperty("server.port", String.valueOf(port));
                                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(configPath)) {
                                            updatedProperties.store(fos, "Updated port after finding available port");
                                            logger.info("Updated configuration with new port: " + port);
                                        }
                                    } catch (IOException f) {
                                        logger.error("Failed to update properties file with new port", f);
                                    }
                                }
                            } else {
                                logger.error("Failed to start RESTful server", e);
                                Parameters.RESTFulServer = false;
                                break;
                            }
                        }
                    }
                    
                    if (!serverStarted) {
                        logger.error("Failed to start RESTful server after {} attempts", maxAttempts);
                        Parameters.RESTFulServer = false;
                    }
                } catch (Exception e) {
                    logger.error("Failed to start RESTful server", e);
                    Parameters.RESTFulServer = false;
                }

            }).start();
}

        private void showRESTfulInfo(ConfigurableApplicationContext context) {
            Environment env = context.getEnvironment();
            String port = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String host = env.getProperty("server.address", "localhost");

            // Base URL
            String baseUrl = "http://" + host + ":" + port + contextPath;

            System.out.println("\n--------------------------------------------------------------");
            System.out.println("eHOST RESTful Server is running at:");
            System.out.println(" - Local URL: " + baseUrl);
            System.out.println("--------------------------------------------------------------\n");
        }

        private void loadGUI() throws Exception {
            String baseUrl = "";
            if (PropertiesUtil.isRestfulServerEnabled()) {
                baseUrl = PropertiesUtil.getRestServerBaseUrl();
            }
            userInterface.GUI gui;
            gui = new userInterface.GUI(PropertiesUtil.getWorkspace(), baseUrl);

            gui.setVisible(true);
            resultEditor.loadingNotes.ShowNotes.setGUIHandler(gui);
        }

        // Functional interface for initialization steps
        @FunctionalInterface
        private interface InitializationStep {
            void execute() throws Exception;
        }
    }
}