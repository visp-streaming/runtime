package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UtilController {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private Utilities utilities;

    @Value("${visp.runtime.ip}")
    private String runtimeip;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/util")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("message", null);
        return "util";
    }

    @RequestMapping("/util/clear")
    public String reinitialize(Model model) throws SchedulerException {

        utilities.createInitialStatus();

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("message", "The topology has been cleared");
        return "util";
    }




}
