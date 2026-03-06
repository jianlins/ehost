package rest.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves IAA report files via HTTP so that report navigation links
 * use the same origin (host:port) as the eHOST REST server.
 * This allows multiple users on different ports to share the same
 * report files while each user's links route to their own eHOST instance.
 */
@Controller
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private static volatile File reportBaseDir;

    private static final Map<String, MediaType> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", MediaType.TEXT_HTML);
        MIME_TYPES.put("htm", MediaType.TEXT_HTML);
        MIME_TYPES.put("css", MediaType.valueOf("text/css"));
        MIME_TYPES.put("js", MediaType.valueOf("application/javascript"));
        MIME_TYPES.put("json", MediaType.APPLICATION_JSON);
        MIME_TYPES.put("png", MediaType.IMAGE_PNG);
        MIME_TYPES.put("jpg", MediaType.IMAGE_JPEG);
        MIME_TYPES.put("jpeg", MediaType.IMAGE_JPEG);
        MIME_TYPES.put("gif", MediaType.IMAGE_GIF);
        MIME_TYPES.put("svg", MediaType.valueOf("image/svg+xml"));
        MIME_TYPES.put("txt", MediaType.TEXT_PLAIN);
        MIME_TYPES.put("xml", MediaType.APPLICATION_XML);
    }

    /**
     * Sets the base directory from which report files are served.
     *
     * @param dir the report directory (e.g., {project}/reports/)
     */
    public static void setReportBaseDir(File dir) {
        reportBaseDir = dir;
        logger.info("Report base directory set to: {}", dir != null ? dir.getAbsolutePath() : "null");
    }

    /**
     * Returns the current report base directory.
     */
    public static File getReportBaseDir() {
        return reportBaseDir;
    }

    @GetMapping("/reports/**")
    public ResponseEntity<byte[]> serveReportFile(HttpServletRequest request) {
        logger.debug("Report request: {} | baseDir: {}", request.getRequestURI(),
                reportBaseDir != null ? reportBaseDir.getAbsolutePath() : "null");

        if (reportBaseDir == null || !reportBaseDir.isDirectory()) {
            String msg = "No report directory configured. baseDir=" +
                    (reportBaseDir != null ? reportBaseDir.getAbsolutePath() : "null");
            logger.warn(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes());
        }

        // Extract the relative path after "/reports/"
        String fullPath = request.getRequestURI();
        String relativePath = fullPath.substring("/reports/".length());

        if (relativePath.isEmpty()) {
            relativePath = "index.html";
        }

        // Decode URL-encoded characters (e.g., %20 for spaces)
        try {
            relativePath = java.net.URLDecoder.decode(relativePath, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is always supported
        }

        // Resolve and validate the file path
        Path resolved = reportBaseDir.toPath().resolve(relativePath).normalize();
        if (!resolved.startsWith(reportBaseDir.toPath().normalize())) {
            logger.warn("Path traversal attempt blocked: {}", relativePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Access denied.".getBytes());
        }

        File file = resolved.toFile();
        if (!file.exists() || !file.isFile()) {
            String msg = "File not found: " + relativePath + " (resolved to: " + resolved + ")";
            logger.warn(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes());
        }

        try {
            byte[] content = Files.readAllBytes(file.toPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(file.getName()));
            // Disable caching so folder changes take effect immediately
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Error reading report file: {}", file.getAbsolutePath(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error reading file.".getBytes());
        }
    }

    private MediaType getMediaType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = fileName.substring(dotIndex + 1).toLowerCase();
            MediaType type = MIME_TYPES.get(ext);
            if (type != null) {
                return type;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
