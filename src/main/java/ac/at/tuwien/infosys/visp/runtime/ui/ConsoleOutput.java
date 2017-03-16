package ac.at.tuwien.infosys.visp.runtime.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@Controller
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

    @RequestMapping(value = "/update", method = RequestMethod.GET)
    public @ResponseBody
    String updateConsoleOutput() {
        return getConsoleOutput();
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