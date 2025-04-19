package rest.server;

import main.eHOST;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import userInterface.GUI;

import java.io.File;

@Controller
public class EhostController {

    private static final String HTML_HEADER = "<!DOCTYPE html><html><head><title>eHOST Control Server</title>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }" +
            "h1 { color: #2c3e50; }" +
            "h2 { color: #3498db; }" +
            ".info { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }" +
            "code { background-color: #f1f1f1; padding: 2px 5px; border-radius: 3px; }" +
            "ul { margin-left: 20px; }" +
            "</style></head><body>";

    private static final String HTML_FOOTER = "</body></html>";

    // Helper method to create help section content
    private String getHelpContent() {
        return "<div class='info'>" +
                "<h2>Available Endpoints:</h2>" +
                "<ul>" +
                "<li>Status check: <code>http://127.0.0.1:8009/</code> or <code>http://127.0.0.1:8009/status</code></li>" +
                "<li>Shutdown server: <code>http://127.0.0.1:8009/shutdown</code></li>" +
                "<li>Navigate to project: <code>http://127.0.0.1:8009/ehost/{projectName}</code></li>" +
                "<li>Navigate to file in project: <code>http://127.0.0.1:8009/ehost/{projectName}/{fileName}</code></li>" +
                "</ul>" +
                "<p>Note: You can change the server port by editing the application.properties file.</p>" +
                "</div>";
    }

    @GetMapping(value = "/")
    @ResponseBody
    String getRoot() {
        // Reuse the existing implementation of getStatus
        return getStatus();
    }


    @GetMapping(value = "/status")
    @ResponseBody
    String getStatus() {
        eHOST.logger.debug("GUI status status: "+GUI.status);

        StringBuilder htmlResponse = new StringBuilder(HTML_HEADER);
        htmlResponse.append("<h1>eHOST Control Server Status</h1>");

        if (GUI.status > GUI.readyThreshold) {
            htmlResponse.append("<div class='info' style='background-color: #d4edda; color: #155724;'>" +
                    "<h2>Server is READY</h2>" +
                    "<p>The eHOST control server is available and ready to process requests.</p>" +
                    "</div>");
        } else {
            htmlResponse.append("<div class='info' style='background-color: #f8d7da; color: #721c24;'>" +
                    "<h2>Server is BUSY</h2>" +
                    "<p>The eHOST control server is currently busy. Please try again later.</p>" +
                    "</div>");
        }

        htmlResponse.append(getHelpContent());
        htmlResponse.append(HTML_FOOTER);

        return htmlResponse.toString();
    }


    @GetMapping(value = "/ehost/{projectName}/{fileName}")
    @ResponseBody
    String getDoc(@PathVariable String projectName,
                  @PathVariable String fileName) throws InterruptedException {
        String response="";
        eHOST.logger.debug("GUI status: "+GUI.status);
        if (projectName != null && GUI.status >GUI.readyThreshold) {
            GUI.status = 0;
            response= GUI.gui.selectProject(projectName,fileName);
            GUI.status = 3;
        }
        return response;
    }

    @GetMapping(value = "/ehost/{projectName}")
    @ResponseBody
    String getProject(@PathVariable String projectName) throws InterruptedException {
        String response="";
        if (projectName != null && GUI.status >GUI.readyThreshold) {
            GUI.status = 0;
            response= GUI.gui.selectProject(projectName,null);
            GUI.status = 3;
            return response;
        }
        return "Ehost is busy. Try again later.";
    }


    @RequestMapping(value = "/path/", method = RequestMethod.POST)
    @ResponseBody
    public String displayFileByPath(@RequestBody PathElements pathElements) {
        String response="";
        if (pathElements != null && pathElements.projectpath != null && GUI.status >GUI.readyThreshold) {
            GUI.status = 0;
            GUI.gui.selectProject(new File(pathElements.projectpath),pathElements.file);
            GUI.status = 3;
        }
        return response;
    }

    public void updateServerPort(String newPort) {
        PropertiesUtil.updatePort(newPort);
        // You might need to restart the server for the new port to take effect
    }



    @Autowired
    private ApplicationContext context;

    @GetMapping("/shutdown")
    @ResponseBody
    public String shutdownApp() {
//      Sent out msg before shutdown completely.
        SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
        return "Ehost with its REST server is shutting down";

    }


}
