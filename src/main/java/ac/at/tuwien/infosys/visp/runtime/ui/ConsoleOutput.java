package ac.at.tuwien.infosys.visp.runtime.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@Controller
@DependsOn("configurationprovider")
@RequestMapping("/console")
public class ConsoleOutput {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Value("${logging.file}")
    private String logFile;

    @RequestMapping()
    public String consoleOutput(Model model) {
        model.addAttribute("executionLogOutput", getConsoleOutput());
        return "consoleoutput";
    }


    private String getConsoleOutput() {
        String result = "";
        try {
            if(Files.exists(Paths.get(logFile))) {
                result = new String(Files.readAllBytes(Paths.get(logFile)));
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
        return result;
    }
}