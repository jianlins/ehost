package rest.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Create array of allowed origins for ports 8001-8020
        String[] allowedOrigins = IntStream.rangeClosed(8001, 8020)
                .mapToObj(port -> "http://localhost:" + port)
                .toArray(String[]::new);

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedOrigins("null", "file://")  // Allow null origin and file protocol
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")  // Allow all headers
                .exposedHeaders("*")  // Expose all headers
                .allowCredentials(true)
                .maxAge(3600L); // 1 hour max age
    }
}