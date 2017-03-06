package ac.at.tuwien.infosys.visp.runtime.ui;


import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UtilController {

    @Value("${visp.runtime.ip}")
    private String runtimeip;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/util")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("message", null);
        return "util";
    }

}
