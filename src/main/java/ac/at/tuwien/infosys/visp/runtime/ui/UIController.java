package ac.at.tuwien.infosys.visp.runtime.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/")
public class UIController {

	@Value("${visp.runtime.ip}")
	private String runtimeip;

	@RequestMapping("/")
	public String index(Model model) {

		model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
		model.addAttribute("rabbitmqMonitoring", "http://" + runtimeip + ":15672");
		return "about";
	}

}
