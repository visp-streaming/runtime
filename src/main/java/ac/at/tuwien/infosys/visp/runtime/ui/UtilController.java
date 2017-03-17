package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@DependsOn("configurationprovider")
public class UtilController {

    @Autowired
    private Configurationprovider config;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/util")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("pagetitle", "VISP Runtime - " + config.getRuntimeIP());
        model.addAttribute("message", null);
        return "util";
    }

}
