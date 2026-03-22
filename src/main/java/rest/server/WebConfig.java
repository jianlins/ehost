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
        // Create arrays of allowed origins for ports 8001-8020
        String[] localhostOrigins = IntStream.rangeClosed(8001, 8020)
                .mapToObj(port -> "http://localhost:" + port)
                .toArray(String[]::new);
        String[] ipOrigins = IntStream.rangeClosed(8001, 8020)
                .mapToObj(port -> "http://127.0.0.1:" + port)
                .toArray(String[]::new);

        // Combine all origins into a single array — chained allowedOrigins()
        // calls replace rather than append, so we must pass them all at once.
        String[] extraOrigins = {"null", "file://"};
        String[] allOrigins = new String[localhostOrigins.length + ipOrigins.length + extraOrigins.length];
        System.arraycopy(localhostOrigins, 0, allOrigins, 0, localhostOrigins.length);
        System.arraycopy(ipOrigins, 0, allOrigins, localhostOrigins.length, ipOrigins.length);
        System.arraycopy(extraOrigins, 0, allOrigins, localhostOrigins.length + ipOrigins.length, extraOrigins.length);

        registry.addMapping("/**")
                .allowedOrigins(allOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")  // Allow all headers
                .exposedHeaders("*")  // Expose all headers
                .allowCredentials(true)
                .maxAge(3600L); // 1 hour max age
    }
}