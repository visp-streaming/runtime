package ac.at.tuwien.infosys.visp.runtime.ui;


import java.util.Arrays;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.ConfigurationForm;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@DependsOn("configurationprovider")
public class ConfigurationController {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Configurationprovider config;

    @RequestMapping("/configuration")
    public String configuration(Model model) {

        ConfigurationForm configurationForm = new ConfigurationForm();
        configurationForm.setRuntimeip(config.getRuntimeIP());
        configurationForm.setInfrastructureip(config.getInfrastructureIP());
        configurationForm.setOpenstackhostimageid(config.getOpenstackProcessingHostImage());
        configurationForm.setProcessingimageid(config.getProcessingNodeImage());
        configurationForm.setReasoner(config.getReasoner());
        configurationForm.setBtu(config.getBtu());
        configurationForm.setMonitoringperiod(config.getMonitoringperiod());
        configurationForm.setAvailabilitycheck(config.getAvailabilitycheck());
        configurationForm.setSimulatestartup(config.getSimulatestartup());
        configurationForm.setShutdowngrace(config.getShutdowngrace());
        configurationForm.setReasoninginterval(config.getReasoninginterval());
        configurationForm.setUpscalingthreshold(config.getUpscalingthreshold());




        /**
         * use
         *         private Integer monitoringperiod;
         private Integer availabilitycheck;
         private Integer simulatestartup;
         private Integer shutdowngrace;
         private Integer resoninginterval;
         private  Integer upscalingthreshold;
         */


        model.addAttribute("reasoners", Arrays.asList("none", "basic", "btu", "rl"));
        model.addAttribute("configurationForm", configurationForm);
        return "configuration";
    }

    @RequestMapping(value="/configurationSaved", method= RequestMethod.POST)
    public String configurationSaved(@ModelAttribute ConfigurationForm form, Model model) throws SchedulerException {

        config.setRuntimeIP(form.getRuntimeip());
        config.setInfrastructureIP(form.getInfrastructureip());
        config.setOpenstackProcessingHostImage(form.getOpenstackhostimageid());
        config.setProcessingNodeImage(form.getProcessingimageid());
        config.setReasoner(form.getReasoner());
        config.setBtu(form.getBtu());
        config.setMonitoringperiod(form.getMonitoringperiod());
        config.setAvailabilitycheck(form.getAvailabilitycheck());
        config.setSimulatestartup(form.getSimulatestartup());
        config.setShutdowngrace(form.getShutdowngrace());
        config.setReasoninginterval(form.getReasoninginterval());
        config.setUpscalingthreshold(form.getUpscalingthreshold());


        config.storeDataToDB();

        LOG.info("Configuration updated via web UI.");

        model.addAttribute("reasoners", Arrays.asList("none", "basic", "btu", "rl"));
        model.addAttribute("configurationForm", form);
        model.addAttribute("message", "The configuration has been updated - you need to restart the application for the configuration to be applied.");
        return "configuration";
    }

}
