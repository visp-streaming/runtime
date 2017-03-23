package ac.at.tuwien.infosys.visp.runtime.ui;


import org.quartz.SchedulerException;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@DependsOn("configurationprovider")
public class UtilController {

    @RequestMapping("/util")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("message", null);
        return "util";
    }

}
