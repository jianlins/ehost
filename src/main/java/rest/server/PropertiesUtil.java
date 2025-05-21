package rest.server;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    private static String propertiesPath = "application.properties";
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final Properties cachedProperties = new Properties();

    private static final Properties cachedEhostConfigParams = new Properties();
    private static boolean ehostConfigLoaded = false;


    // Application config paths
    private static String ehostConfighome;
    private static String workspace;
    private static String restConfig = "";

    // Default values
    private static final String DEFAULT_PORT = "8001";
    private static final String DEFAULT_ADDRESS = "127.0.0.1";

    /**
     * Parses command line arguments and initializes all configuration paths
     *
     * @param args Command line arguments
     */
    public static void initConfigsFromArgs(String[] args) {
        Options options = new Options();
        options.addOption("c", "ehostconfighome", true, "The directory that contains eHOST configuration files");
        options.addOption("w", "workspace", true, "eHOST workspace-- the directory where your eHOST projects are hosted.");
        options.addOption("s", "spring.config.location", true, "Spring application configuration file location.");
        options.addOption("h", "help", false, "Display help information");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.warn(e.getMessage());
            formatter.printHelp("eHOST", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("eHOST", options);
            return;
        }
        workspace = cmd.getOptionValue("w");
        // Process eHOST config home path
        initEhostConfigHome(cmd);

        // Process REST config location
        initRestConfigLocation(cmd);

        // Set workspace



        // Initialize properties based on the discovered paths
        init(args);
    }

    /**
     * Initializes eHOST configuration home directory
     */
    private static void initEhostConfigHome(CommandLine cmd) {
        ehostConfighome = cmd.getOptionValue("c");
        if (ehostConfighome == null) {
            logger.info("ehostconfighome is not set in command, using default directory 'USER_HOME/.ehost'");
            ehostConfighome = Paths.get(System.getProperty("user.home"), ".ehost").toString();
        } else {
            logger.info("Reading configurations from {}", ehostConfighome);
        }

        Path ehostConfigPath = Paths.get(ehostConfighome, "eHOST.sys");
        if (!Files.exists(ehostConfigPath)) {
            copyDefaultFileFromResources("eHOST.sys", ehostConfigPath);
        }

        if (workspace == null) {
            workspace = readWorkspaceFromConfig();
            if (workspace != null) {
                logger.info("Workspace path from eHOST.sys: {}", workspace);
            }
        }
        env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath=workspace;

        boolean restfulEnabled = isRestfulServerEnabled();
        logger.info("RESTful server enabled in configuration: {}", restfulEnabled);
        // Set the parameter for the application to use
        env.Parameters.RESTFulServer = restfulEnabled;
    }

    /**
     * Reads the workspace path from eHOST.sys configuration file
     *
     * @return The workspace path, or null if not found
     */
    public static String readWorkspaceFromConfig() {
        return readEhostConfigParameter("WORKSPACE_PATH");
    }

    /**
     * Determines if the RESTful server is enabled in the eHOST configuration
     *
     * @return true if enabled, false otherwise
     */
    public static boolean isRestfulServerEnabled() {
        String value = readEhostConfigParameter("RESTFUL_SERVER");
        return "true".equalsIgnoreCase(value);
    }

       /**
     * Loads and caches all parameters from the eHOST.sys configuration file
     *
     * @return true if loaded successfully, false otherwise
     */
    private static synchronized boolean loadEhostConfigParams() {
        if (ehostConfigLoaded) {
            return true;
        }

        try {
            Path ehostConfigPath = Paths.get(ehostConfighome, "eHOST.sys");
            if (!Files.exists(ehostConfigPath)) {
                logger.warn("eHOST.sys file not found at {}", ehostConfigPath);
                return false;
            }

            String currentParam = null;
            cachedEhostConfigParams.clear();

            for (String line : Files.readAllLines(ehostConfigPath)) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("//")) {
                    continue;
                }

                // Check if this is a parameter
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentParam = line.substring(1, line.length() - 1);
                    continue;
                }

                // If we have a current parameter and this is a value, store it
                if (currentParam != null && !line.isEmpty()) {
                    cachedEhostConfigParams.setProperty(currentParam, line);
                    // Note: This will store only the first value for each parameter
                }
            }

            ehostConfigLoaded = true;
            logger.debug("Loaded {} parameters from eHOST.sys", cachedEhostConfigParams.size());
            return true;
        } catch (IOException e) {
            logger.error("Error loading eHOST.sys: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reads a specific parameter value from the cached eHOST.sys configuration
     *
     * @param parameterName The parameter name to look for (without brackets)
     * @return The first value for the parameter, or null if not found
     */
    public static String readEhostConfigParameter(String parameterName) {
        ensureInitialized();

        if (!ehostConfigLoaded) {
            loadEhostConfigParams();
        }

        return cachedEhostConfigParams.getProperty(parameterName);
    }

    /**
     * Reloads the eHOST.sys configuration file into the cache
     */
    public static void reloadEhostConfig() {
        ehostConfigLoaded = false;
        loadEhostConfigParams();
    }

    /**
     * Updates a parameter in the eHOST.sys configuration file and cache
     *
     * @param parameterName The parameter name to update
     * @param value The new value to set
     * @return true if updated successfully, false otherwise
     */
    public static boolean updateEhostConfigParameter(String parameterName, String value) {
        ensureInitialized();

        try {
            Path ehostConfigPath = Paths.get(ehostConfighome, "eHOST.sys");
            if (!Files.exists(ehostConfigPath)) {
                logger.warn("eHOST.sys file not found at {}", ehostConfigPath);
                return false;
            }

            List<String> lines = Files.readAllLines(ehostConfigPath);
            List<String> newLines = new ArrayList<>();

            String currentParam = null;
            boolean paramFound = false;
            boolean valueUpdated = false;

            for (String line : lines) {
                String trimmedLine = line.trim();

                // Handle parameter headers
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    currentParam = trimmedLine.substring(1, trimmedLine.length() - 1);
                    newLines.add(line);

                    if (currentParam.equals(parameterName)) {
                        paramFound = true;
                    }
                    continue;
                }

                // If we're at our target parameter and haven't updated the value yet
                if (paramFound && currentParam.equals(parameterName) && !valueUpdated) {
                    // Skip empty lines at the start
                    if (trimmedLine.isEmpty() || trimmedLine.startsWith("//")) {
                        newLines.add(line);
                        continue;
                    }

                    // Replace the first non-empty, non-comment line with our value
                    newLines.add(value);
                    valueUpdated = true;

                    // Continue to next line without adding the current line
                    continue;
                }

                // Add all other lines unchanged
                newLines.add(line);
            }

            // If parameter wasn't found, add it
            if (!paramFound) {
                newLines.add("");  // Empty line before new parameter
                newLines.add("[" + parameterName + "]");
                newLines.add(value);
            }

            // Write the updated file
            Files.write(ehostConfigPath, newLines);

            // Update the cache
            cachedEhostConfigParams.setProperty(parameterName, value);

            logger.info("Updated parameter {} to value {} in eHOST.sys", parameterName, value);
            return true;
        } catch (IOException e) {
            logger.error("Error updating eHOST.sys: {}", e.getMessage(), e);
            return false;
        }
    }


    /**
     * Initializes REST configuration location
     */
    private static void initRestConfigLocation(CommandLine cmd) {
        String restConfigLocation = System.getProperty("spring.config.location");

        if (restConfigLocation == null || !Files.exists(Paths.get(restConfigLocation))) {
            if (cmd.hasOption("spring.config.location") &&
                    Files.exists(Paths.get(cmd.getOptionValue("spring.config.location")))) {

                restConfig = cmd.getOptionValue("spring.config.location");
                logger.info("spring.config.location set as program argument: {}",
                        Paths.get(cmd.getOptionValue("spring.config.location")).toUri());
            } else {
                logger.info("spring.config.location not set or doesn't exist. " +
                        "Searching in current directory and USER_HOME/.ehost");

                Path localRestConfig = Paths.get(ehostConfighome, "application.properties");
                if (Files.exists(localRestConfig)) {
                    logger.info("Found REST controller configuration: {}", localRestConfig.toUri());
                    restConfig = localRestConfig.toString();
                } else {
                    logger.info("No application.properties found in current directory: {}",
                            Paths.get(".").toAbsolutePath());

                    Path userHomeRestConfig = Paths.get(ehostConfighome, "application.properties");
                    if (Files.exists(userHomeRestConfig)) {
                        logger.info("Found REST controller configuration: {}",
                                userHomeRestConfig.toAbsolutePath());
                    } else {
                        logger.info("No application.properties found in USER_HOME directory: {}",
                                userHomeRestConfig.toAbsolutePath());
                        logger.info("Creating one with default settings...");
                        copyDefaultFileFromResources("application.properties", userHomeRestConfig);
                    }
                    restConfig = userHomeRestConfig.toString();
                }
            }
            System.setProperty("spring.config.location", restConfig);
        } else {
            logger.info("spring.config.location set as VM argument: {}",
                    Paths.get(restConfigLocation).toUri());
            restConfig = restConfigLocation;
        }

        // Set the properties path
        propertiesPath = restConfig;
    }



    /**
     * Builds and returns the complete base URL for the REST server
     *
     * @return The base URL in format "http://host:port"
     */
    public static String getRestServerBaseUrl() {
        ensureInitialized();

        try {
            // Get server configuration from properties file
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(restConfig)) {
                properties.load(fis);
            }
            String port = properties.getProperty("server.port", getPort());
            String host = properties.getProperty("server.address", getAddress());
            return "http://" + host + ":" + port;
        } catch (IOException e) {
            logger.warn("Error loading REST properties from file, using cached values", e);
            // Use cached properties as fallback
            return "http://" + getAddress() + ":" + getPort();
        }
    }


    /**
     * Copies a default resource file to the specified path
     *
     * @param resourcePath Resource path inside JAR
     * @param targetFilePath Target file path to copy to
     */
    private static void copyDefaultFileFromResources(String resourcePath, Path targetFilePath) {
        Path parentDir = targetFilePath.getParent();
        Resource resource = new ClassPathResource(resourcePath);

        try {
            if (!Files.exists(parentDir)) {
                logger.info("Directory {} does not exist. Creating it.", parentDir);
                Files.createDirectories(parentDir);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                if (inputStream == null) {
                    logger.warn("Resource file not found: {}", resourcePath);
                    return;
                }

                logger.info("Copying resource '{}' to: {}", resourcePath, targetFilePath);
                Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("File copied successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to copy resource file: {}", e.getMessage(), e);
        }
    }

    /**
     * Initializes the properties configuration from command line arguments
     *
     * @param args Command line arguments
     */
    public static void init(String[] args) {
        if (initialized.getAndSet(true)) {
            return;
        }

        // If not already set in initConfigsFromArgs
        if (propertiesPath.equals("application.properties")) {
            // Check command line arguments for custom properties file
            if (System.getProperty("spring.config.location") != null) {
                propertiesPath = System.getProperty("spring.config.location");
                logger.info("Custom properties file from system property: {}", propertiesPath);
            } else {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].startsWith("--spring.config.location=")) {
                        propertiesPath = args[i].substring("--spring.config.location=".length());
                        logger.info("Custom properties file from command line: {}", propertiesPath);
                        break;
                    }
                }
            }

            // If still using default, set absolute path
            if (propertiesPath.equals("application.properties")) {
                ApplicationHome home = new ApplicationHome(EhostServerApp.class);
                File jarDir = home.getDir();
                logger.info("JAR directory: {}", jarDir.getAbsolutePath());
                propertiesPath = new File(jarDir, "application.properties").getAbsolutePath();
            }
        }

        initializePropertiesFile();
        loadProperties();
    }

    /**
     * Initializes with default settings if not already initialized
     */
    public static void initDefault() {
        if (initialized.getAndSet(true)) {
            return;
        }

        if (System.getProperty("spring.config.location") != null) {
            propertiesPath = System.getProperty("spring.config.location");
            logger.info("Custom properties file: {}", propertiesPath);
        } else {
            ApplicationHome home = new ApplicationHome(EhostServerApp.class);
            File jarDir = home.getDir();
            propertiesPath = new File(jarDir, "application.properties").getAbsolutePath();
        }

        initializePropertiesFile();
        loadProperties();
    }

    /**
     * Creates default properties file if it doesn't exist
     */
    private static void initializePropertiesFile() {
        Path propertiesFilePath = Paths.get(propertiesPath);
        if (!Files.exists(propertiesFilePath)) {
            try {
                // Ensure parent directory exists
                Path parent = propertiesFilePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                // Try to copy from classpath if exists
                ClassPathResource defaultProps = new ClassPathResource("application.properties");
                if (defaultProps.exists()) {
                    try (InputStream is = defaultProps.getInputStream();
                         OutputStream os = Files.newOutputStream(propertiesFilePath)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            os.write(buffer, 0, length);
                        }
                    }
                    logger.info("Created properties file from classpath template at: {}", propertiesFilePath);
                } else {
                    // Create empty properties file with default values
                    Properties props = new Properties();
                    props.setProperty("server.port", DEFAULT_PORT);
                    props.setProperty("server.address", DEFAULT_ADDRESS);
                    try (OutputStream os = Files.newOutputStream(propertiesFilePath)) {
                        props.store(os, "Initial properties");
                    }
                    logger.info("Created default properties file at: {}", propertiesFilePath);
                }
            } catch (IOException e) {
                logger.error("Failed to create properties file: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Loads properties from file into cache
     */
    private static synchronized void loadProperties() {
        try (InputStream is = Files.newInputStream(Paths.get(propertiesPath))) {
            cachedProperties.clear();
            cachedProperties.load(is);
            logger.debug("Properties loaded from: {}", propertiesPath);
        } catch (IOException e) {
            logger.warn("Could not read properties file, using defaults: {}", e.getMessage());
            // Set defaults if file can't be read
            cachedProperties.setProperty("server.port", DEFAULT_PORT);
            cachedProperties.setProperty("server.address", DEFAULT_ADDRESS);
        }
    }

    /**
     * Gets the server port from properties
     *
     * @return The configured server port
     */
    public static String getPort() {
        ensureInitialized();
        return getProperty("server.port", DEFAULT_PORT);
    }

    /**
     * Gets the server address from properties
     *
     * @return The configured server address
     */
    public static String getAddress() {
        ensureInitialized();
        return getProperty("server.address", DEFAULT_ADDRESS);
    }

    /**
     * Gets any property with a default fallback
     *
     * @param key Property key
     * @param defaultValue Default value if property not found
     * @return The property value or default
     */
    public static String getProperty(String key, String defaultValue) {
        ensureInitialized();
        synchronized (cachedProperties) {
            return cachedProperties.getProperty(key, defaultValue);
        }
    }

    /**
     * Updates or adds a property and persists to the properties file
     *
     * @param key Property key to update
     * @param value New value to set
     */
    public static synchronized void updateProperty(String key, String value) {
        ensureInitialized();

        try {
            synchronized (cachedProperties) {
                // Update cache
                cachedProperties.setProperty(key, value);

                // Save to file
                try (OutputStream os = Files.newOutputStream(Paths.get(propertiesPath))) {
                    cachedProperties.store(os, "Updated by PropertiesUtil");
                }
            }
            logger.info("Property {} updated to {}", key, value);
        } catch (IOException e) {
            logger.error("Failed to update property {}: {}", key, e.getMessage(), e);
        }
    }

    /**
     * Updates the server port
     *
     * @param newPort New port number
     */
    public static void updatePort(String newPort) {
        updateProperty("server.port", newPort);
    }

    /**
     * Returns the path to the properties file
     *
     * @return Absolute path to the properties file
     */
    public static String getPropertiesPath() {
        ensureInitialized();
        return propertiesPath;
    }

    /**
     * Gets the eHOST configuration home directory
     *
     * @return The eHOST configuration home directory
     */
    public static String getEhostConfighome() {
        ensureInitialized();
        return ehostConfighome;
    }

    public static String getWorkspace() {
        ensureInitialized();
        return workspace;
    }

    /**
     * Gets the REST configuration path
     *
     * @return The REST configuration path
     */
    public static String getRestConfig() {
        ensureInitialized();
        return restConfig;
    }

    /**
     * Ensures the properties are initialized
     */
    private static void ensureInitialized() {
        if (!initialized.get()) {
            initDefault();
        }
    }

    /**
     * Reloads properties from disk
     * @return true if reload was successful
     */
    public static boolean reloadProperties() {
        try {
            loadProperties();
            return true;
        } catch (Exception e) {
            logger.error("Failed to reload properties: {}", e.getMessage(), e);
            return false;
        }
    }
}