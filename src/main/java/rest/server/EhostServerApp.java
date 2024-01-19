package rest.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@EnableConfigurationProperties(RESTFulConfig.class)
public class EhostServerApp extends SpringBootServletInitializer {

    public static void main(String[] args) {


        SpringApplication.run(EhostServerApp.class, args);
    }


}
