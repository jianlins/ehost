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
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import rest.server.EhostServerApp;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;


/**
 * The entrance class to load the GUI of eHOST and display the splash dialog
 * before the main frame finishing loading resource.
 *
 * @author Jianwei "Chris" Leng
 */
public class eHOST {
    public static Logger logger = LoggerFactory.getLogger(eHOST.class);
    public static String ehostconfighome;
    public static String workspace;
    public static String restConfig;
    private static CustomSplash splash;

    /**
     * Initial works, before finishing loading the GUI.
     */
    private static void initial() {

        // check whether current OS is a Mac OS
        // and switchpool.iniMSP.isUnixOS = true if its a Mac OS / Unix;
        // we use '/' as the path separator
        Parameters.isUnixOS = commons.OS.isMacOS();

        config.system.SysConf.loadSystemConfigure(ehostconfighome);

        if (workspace!=null && Files.exists(Paths.get(workspace))){
            logger.info("'workspace' has been set in command, it will overwrite the WORKSPACE_PATH in the configuration file "+Paths.get(ehostconfighome, "eHOST.sys"));
            env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath=Paths.get(workspace).toAbsolutePath().toString();
        }else{
            logger.info("'workspace' has been set in command, it will be set from the WORKSPACE_PATH in the configuration file "+Paths.get(ehostconfighome, "eHOST.sys"));
            workspace=env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath;
            logger.info("Now workspace=\t"+workspace);
        }

        // set the flag, after loading configure information
        Parameters.isFirstTimeLoadingConfigureFile = false;

        // set the latest mention id to the module if XML output
        int latest_used_metion_id = Parameters.getLatestUsedMentionID();
        algorithmNegex.XMLMaker
                .set_mention_id_startpoint(latest_used_metion_id);
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

    private static void initConfigsFromArgs(String[] args) {
        Options options = new Options();
        options.addOption("c", "ehostconfighome", true, "The directory that contains eHOST configuration files");
        options.addOption("w", "workspace", true, "eHOST workspace-- the directory where your eHOST projects are hosted.");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.warn(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("utility-name", options);
            return;
        }

        ehostconfighome = cmd.getOptionValue("c");
        if (ehostconfighome==null){
            logger.info("ehostconfighome is not set in command, try access the default directory 'USER_HOME/.ehost'");
            ehostconfighome=Paths.get(System.getProperty("user.home"),".ehost").toString();
        }else{
            logger.info("Try read configurations from " + ehostconfighome);
        }
        Path ehostConfigPath=Paths.get(ehostconfighome, "eHOST.sys");
        if (!Files.exists(ehostConfigPath))
            copyDefaultFileFromResources("eHOST.sys", ehostConfigPath);

//        now check rest controller server configurations
        String restConfigLocation = System.getProperty("spring.config.location");
        if (restConfigLocation==null || !Files.exists(Paths.get(restConfigLocation))){
            logger.info(" -Dspring.config.location has not been set in command or does not exists. Try to find it in current directory and then USER_HOME/.ehost.\n" +
                    "\tIf you do want to set a specific application.properties to use, you can try command like: \n" +
                    "\tjava -Dspring.config.location=file:/path/to/config/dir/application.properties -jar ehost-xxxx.jar");
            Path localRestConfig=Paths.get("application.properties");
            if(Files.exists(localRestConfig)){
                logger.info("Find rest controller configuration in your local directory: "+localRestConfig.toString());
                restConfig=localRestConfig.toString();
            }else{
                logger.info("No application.properties file is found in your current directory: "+Paths.get(".").toAbsolutePath());
                Path userHomeRestConfig=Paths.get(ehostconfighome, "application.properties");
                if(Files.exists(userHomeRestConfig)){
                    logger.info("Find rest controller configuration in your USER_HOME directory: "+userHomeRestConfig.toAbsolutePath());
                }else{
                    logger.info("No application.properties file is found in your USER_HOME directory: "+userHomeRestConfig.toAbsolutePath());
                    logger.info("Try to create one there using default settings...");
                    copyDefaultFileFromResources("application.properties", userHomeRestConfig);
                }
                restConfig=userHomeRestConfig.toString();
            }
        }else{
            restConfig=restConfigLocation;
        }

        workspace = cmd.getOptionValue("w");
    }

    private static void copyDefaultFileFromResources(String resourcePath, String targetFile){
        Path target=Paths.get(targetFile);
        copyDefaultFileFromResources(resourcePath, target);
    }

    private static void copyDefaultFileFromResources(String resourcePath, Path targetFilePath){
        Path parentDir=targetFilePath.getParent();
        Resource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            if (!Files.exists(parentDir)){
                logger.info("Directory "+parentDir+ " does not exist. Try to create one.");
                Files.createDirectories(parentDir);
            }

            if (inputStream == null) {
                // Resource file not found
                logger.warn("Resource file not found. Make sure the path is correct.");
                return;
            }
            logger.info("Copy resource file '"+resourcePath+"' from jar to: "+targetFilePath);
            Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copied successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                executeStep("Initializing configurations...", this::initConfigurations, 5, 10,500);
                executeStep("Initializing settings...", this::initSettings, 15,20, 500);

                if (Parameters.RESTFulServer) {
                    executeStep("Starting RESTful server in the background...", this::startRESTServer, 30, 50, 600);
                }

                executeStep("Loading GUI...", this::loadGUI, 70, 90, 1800);
            } catch (Exception e) {
                e.printStackTrace();
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
            initConfigsFromArgs(args);
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
                    SpringApplication app = new SpringApplication(EhostServerApp.class);
                    Properties properties = new Properties();
                    String configLocation = Paths.get(restConfig).toAbsolutePath().toUri().toString();
                    properties.setProperty("spring.config.location", configLocation);
                    app.setDefaultProperties(properties);
                    ConfigurableApplicationContext context = app.run(args);
                    showRESTfulInfo(context);
                } catch (Exception e) {
                    e.printStackTrace();
                    Parameters.RESTFulServer = false;
                }
            }).start();
        }

        private void showRESTfulInfo(ConfigurableApplicationContext context){
            Environment env = context.getEnvironment();
            String port = env.getProperty("server.port", "8080"); // Default to 8080 if not specified
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String host = env.getProperty("server.address", "localhost");

            // Print server information to console
            System.out.println("\n--------------------------------------------------------------");
            System.out.println("eHOST RESTful Server is running at:");
            System.out.println(" - Local URL: http://" + host + ":" + port + contextPath);

        }

        private void loadGUI() throws Exception {
            userInterface.GUI gui;
            if (workspace != null)
                gui = new userInterface.GUI(workspace);
            else
                gui = new userInterface.GUI();

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