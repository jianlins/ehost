package rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import userInterface.GUI;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

@Controller
public class EhostController {


    @GetMapping(value = "/status")
    @ResponseBody
    String getStatus() {
        return GUI.gui.ready + "";
    }

    @GetMapping(value = "/ehost/{projectName}/{fileName}")
    @ResponseBody
    String getDoc(@PathVariable String projectName,
                  @PathVariable String fileName) throws InterruptedException {
        String response="";
        if (projectName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            response= GUI.gui.selectProject(projectName);
        }


        if (fileName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            response= GUI.gui.showFileContextInTextPane(fileName);
        }


        return response;
    }

    @GetMapping(value = "/ehost/{projectName}")
    @ResponseBody
    String getProject(@PathVariable String projectName) throws InterruptedException {
        String response="";
        if (projectName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            response= GUI.gui.selectProject(projectName);
            return response;
        }
        return "Ehost is busy. Try again later.";
    }


    @RequestMapping(value = "/path/", method = RequestMethod.POST)
    @ResponseBody
    public String displayFileByPath(@RequestBody PathElements pathElements) {
        String response="";
        if (pathElements != null && pathElements.projectpath != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            GUI.gui.selectProject(new File(pathElements.projectpath));
        }

        if (pathElements != null && pathElements.file != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            response=GUI.gui.showFileContextInTextPane(pathElements.file);
        }
        return response;
    }


    @Autowired
    private ApplicationContext context;

    @GetMapping("/shutdown")
    @ResponseBody
    public String shutdownApp() {
//      Sent out msg before shutdown completely.
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        // Your code here
                        int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
                        System.exit(exitCode);
                    }
                },
                500
        );
        return "Ehost with its REST server is shutting down";

    }


}
