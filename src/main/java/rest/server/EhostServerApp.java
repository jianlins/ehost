package rest.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import java.net.ServerSocket;
import java.io.IOException;

@SpringBootApplication
public class EhostServerApp extends SpringBootServletInitializer {

    private static final int MIN_PORT = 8001;
    private static final int MAX_PORT = 8020;  // adjust range as needed

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EhostServerApp.class);

        int port = findAvailablePort();

        if (port != -1) {
            // Update application.properties with the new port
            PropertiesUtil.updatePort(String.valueOf(port));

            // Set the port for this run
            app.setDefaultProperties(java.util.Collections.singletonMap(
                    "server.port", String.valueOf(port)));

            app.run(args);
        } else {
            System.err.println("No available ports found in range " + MIN_PORT + " - " + MAX_PORT);
        }
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static int findAvailablePort() {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

}