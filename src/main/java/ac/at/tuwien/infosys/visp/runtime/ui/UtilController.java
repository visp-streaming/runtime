package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UtilController {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private Utilities utilities;


    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/util")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("message", null);
        return "util";
    }

    @RequestMapping("/util/reinitialize")
    public String reinitialize(Model model) throws SchedulerException {

        utilities.createInitialStatus();

        model.addAttribute("message", "The topology has been reinitialized");
        return "util";
    }


}
