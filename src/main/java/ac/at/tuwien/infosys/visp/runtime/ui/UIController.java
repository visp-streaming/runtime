package ac.at.tuwien.infosys.visp.runtime.ui;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/")
@DependsOn("configurationprovider")
public class UIController {

	@Autowired
	private Configurationprovider config;

	@RequestMapping("/")
	public String index(Model model) {

		model.addAttribute("pagetitle", "VISP Runtime - " + config.getRuntimeIP());
		model.addAttribute("rabbitmqMonitoring", "http://" + config.getRuntimeIP() + ":15672");
		return "about";
	}

}
