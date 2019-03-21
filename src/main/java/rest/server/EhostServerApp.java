package rest.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RESTFulConfig.class)
public class EhostServerApp {

    public static void main(String[] args) {
        SpringApplication.run(EhostServerApp.class, args);
    }


}
