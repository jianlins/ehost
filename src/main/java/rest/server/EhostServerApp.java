package rest.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableConfigurationProperties(RESTFulConfig.class)
public class EhostServerApp extends SpringBootServletInitializer {
    @Value("${logging.level.root:OFF}")
    String message = "";

    public static void main(String[] args) {
        SpringApplication.run(EhostServerApp.class, args);
    }


}
