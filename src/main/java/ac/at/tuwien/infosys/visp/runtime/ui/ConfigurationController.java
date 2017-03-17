package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.ConfigurationForm;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ConfigurationController {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Configurationprovider config;

    @RequestMapping("/configuration")
    public String configuration(Model model) {

        ConfigurationForm configurationForm = new ConfigurationForm(config.getRuntimeIP(), config.getInfrastructureIP(), config.getOpenstackProcessingHostImage(), config.getProcessingNodeImage());

        //model.addAttribute("types", presets.getTypes());
        //model.addAttribute("patterns", presets.getPatterns());
        //model.addAttribute(configurationForm);


        model.addAttribute("configurationForm", configurationForm);
        model.addAttribute("message", null);
        return "configuration";
    }

    @RequestMapping(value="/configurationSaved", method= RequestMethod.POST)
    public String configurationSaved(@ModelAttribute ConfigurationForm form, Model model) throws SchedulerException {

        config.setRuntimeIP(form.getRuntimeip());
        config.setInfrastructureIP(form.getInfrastructureip());
        config.setOpenstackProcessingHostImage(form.getOpenstackhostimageid());
        config.setProcessingNodeImage(form.getProcessingimageid());

        config.storeDataToDB();



        //model.addAttribute("types", presets.getTypes());
        //model.addAttribute(form);

        //ecs.storeData(form);

        model.addAttribute("configurationForm", form);
        model.addAttribute("message", "The configuration has been updated - you need to restart the application for the configuration to be applied.");
        return "configuration";
    }

}
