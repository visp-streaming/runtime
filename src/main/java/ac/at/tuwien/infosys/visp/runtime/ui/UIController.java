package ac.at.tuwien.infosys.visp.runtime.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/")
public class UIController {

	@Value("${spring.rabbitmq.host}")
	private String rabbitMQHost;

	@RequestMapping("/")
	public String index(Model model) {

		model.addAttribute("rabbitmqMonitoring", "http://" + rabbitMQHost + ":15672");
		return "about";
	}

}
