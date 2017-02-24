package ac.at.tuwien.infosys.visp.runtime.ui;


import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UtilController {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());


    @RequestMapping("/list")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("tasks", null);
        return "tasks";
    }


    @RequestMapping("/configuration")
    public String configuration(Model model) {

        model.addAttribute("endpointConfiguration", null);
        model.addAttribute("message", null);
        return "configuration";
    }


}
