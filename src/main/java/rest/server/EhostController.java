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


    @GetMapping(value = "/status")
    @ResponseBody
    String getStatus() {
        eHOST.logger.debug("GUI status status: "+GUI.status);
        return (GUI.status >GUI.readyThreshold) + "";
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
        }
        return response;
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
