package rest.server;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    private static String propertiesPath = "application.properties";
    private static boolean initialized = false;
    private static Properties cachedProperties = new Properties();

    public static void init(String[] args) {
        if (initialized) return;

        // Check command line arguments for custom properties file
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--spring.config.location=")) {
                propertiesPath = args[i].substring("--spring.config.location=".length());
                break;
            }
        }

        // If no custom path specified, use the current directory
        if (propertiesPath.equals("application.properties")) {
            ApplicationHome home = new ApplicationHome(EhostServerApp.class);
            File jarDir = home.getDir();
            logger.info("JAR directory: {}", jarDir.getAbsolutePath());
            propertiesPath = new File(jarDir, "application.properties").getAbsolutePath();
        }

        initializePropertiesFile();
        loadProperties(); // Load initial properties into cache
        initialized = true;
    }

    public static void initDefault() {
        if (initialized) return;

        ApplicationHome home = new ApplicationHome(EhostServerApp.class);
        File jarDir = home.getDir();
        propertiesPath = new File(jarDir, "application.properties").getAbsolutePath();

        initializePropertiesFile();
        loadProperties(); // Load properties into cache
        initialized = true;
    }

    private static void initializePropertiesFile() {
        File propertiesFile = new File(propertiesPath);
        if (!propertiesFile.exists()) {
            try {
                // Try to copy from classpath if exists
                ClassPathResource defaultProps = new ClassPathResource("application.properties");
                if (defaultProps.exists()) {
                    org.springframework.util.FileCopyUtils.copy(
                            defaultProps.getInputStream(),
                            new FileOutputStream(propertiesFile)
                    );
                } else {
                    // Create empty properties file with default port
                    Properties props = new Properties();
                    props.setProperty("server.port", "8080");
                    try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
                        props.store(fos, "Initial properties");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadProperties() {
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            cachedProperties.load(fis);
        } catch (IOException e) {
            // If file doesn't exist or can't be read, set default port
            cachedProperties.setProperty("server.port", "8001");
            cachedProperties.setProperty("server.address", "127.0.0.1");
        }
    }

    public static String getPort() {
        if (!initialized) {
            initDefault();
        }
        // Always read from file to get the latest value
        try (FileInputStream fis = new FileInputStream(new File(propertiesPath))) {
            Properties props = new Properties();
            props.load(fis);
            return props.getProperty("server.port", "8001");
        } catch (IOException e) {
            return "8001"; // default port if file not found or error
        }
    }

    public static String getAddress() {
        if (!initialized) {
            initDefault();
        }
        // Always read from file to get the latest value
        try (FileInputStream fis = new FileInputStream(propertiesPath)) {
            Properties props = new Properties();
            props.load(fis);
            return props.getProperty("server.address", "127.0.0.1");
        } catch (IOException e) {
            return "127.0.0.1"; // default port if file not found or error
        }
    }

    public static void updatePort(String newPort) {
        try {
            // Load existing properties
            Properties props = new Properties();
            File file = new File(propertiesPath);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                }
            }

            // Update port
            props.setProperty("server.port", newPort);

            // Save properties
            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "Updated server port");
            }

            // Update cache
            loadProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getPropertiesPath() {
        if (!initialized) {
            initDefault();
        }
        return propertiesPath;
    }
}