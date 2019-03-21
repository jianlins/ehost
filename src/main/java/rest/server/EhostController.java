package rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import userInterface.GUI;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

@Controller
public class EhostController {


    @GetMapping(value = "/ehost/{projectName}/{fileName}")
    @ResponseBody
    String getDoc(@PathVariable String projectName,
                  @PathVariable String fileName) throws InterruptedException {
        if (projectName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            GUI.gui.selectProject(projectName);
        }


        if (fileName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            GUI.gui.showFileContextInTextPane(fileName);
        }


        return "success";
    }

    @GetMapping(value = "/ehost/{projectName}")
    @ResponseBody
    String getProject(@PathVariable String projectName) throws InterruptedException {
        if (projectName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            GUI.gui.selectProject(projectName);
            return "success";
        }
        return "Ehost is busy. Try again later.";
    }


    @RequestMapping(value = "/path/", method = RequestMethod.POST)
    @ResponseBody
    public String displayFileByPath(@RequestBody PathElements pathElements) {
        if (pathElements != null && pathElements.projectPath != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            GUI.gui.selectProject(new File(pathElements.projectPath));
        }

        if (pathElements != null && pathElements.fileName != null && GUI.gui.ready) {
            GUI.gui.ready = false;
            GUI.gui.showFileContextInTextPane(pathElements.fileName);
        }
        return "";
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
