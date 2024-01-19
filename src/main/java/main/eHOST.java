package main;/*
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
 *
 */

import env.Parameters;
import log.LogCleaner;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
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

    /**
     * Initial works, before finishing loading the GUI.
     */
    private static void initial() {

        // check whether current OS is a Mac OS
        // and switchpool.iniMSP.isUnixOS = true if its a Mac OS / Unix;
        // we use '/' as the path separator
        Parameters.isUnixOS = commons.OS.isMacOS();

        // log begin
        String text = "#eHOST# Reading configure file ...";
        //log.LoggingToFile.log(Level.INFO, text);

        // display the splash window for this software
        userInterface.splashWindow.SplashController.showtext(text);

        // load eHOST system configure information
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

        String text;
        initConfigsFromArgs(args);

        // start the splash window
        userInterface.splashWindow.SplashController.start();

        try {
            LogCleaner deleteLocker = new LogCleaner();
            deleteLocker.doit();

            // init the log system
            //Logger logger = Logger.getLogger("eHOST");
            //log.LoggingToFile.setLogingProperties(logger, Level.ALL);
            //log.LoggingToFile.setLogger(logger);

            text = "#eHOST# Initializing ...";
            //log.LoggingToFile.log(Level.INFO, text);
            userInterface.splashWindow.SplashController.showtext(text);

            // read configure settings and load values into memory
            initial();

            // show the first sentence on the splash window
            text = "#eHOST# Launching the main GUI ...";
            //log.LoggingToFile.log(Level.INFO, text);
            userInterface.splashWindow.SplashController.showtext(text);

            // start loading the main GUI in a new thread
            new Thread() {
                @Override
                public void run() {

                    // show dialog - main GUI window of eHOST project
                    userInterface.GUI gui;
                    if (workspace!=null)
                        gui = new userInterface.GUI(workspace);
                    else
                        gui = new userInterface.GUI();
                    gui.setVisible(true);
                    resultEditor.loadingNotes.ShowNotes.setGUIHandler(gui);

                }
            }.start();

            if (Parameters.RESTFulServer) {
                try {
                    SpringApplication app = new SpringApplication(EhostServerApp.class);
                    Properties properties = new Properties();
                    String configLocation=Paths.get(restConfig).toAbsolutePath().toUri().toString();
                    properties.setProperty("spring.config.location", configLocation);
                    app.setDefaultProperties(properties);
                    app.run(args);
                } catch (Exception e) {
                    e.printStackTrace();
                    Parameters.RESTFulServer = false;
                }
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }
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
}
