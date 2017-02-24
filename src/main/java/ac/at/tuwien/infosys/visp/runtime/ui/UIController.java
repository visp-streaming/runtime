package ac.at.tuwien.infosys.visp.runtime.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/")
public class UIController {


	@RequestMapping("/")
	public String index() {
		return "about";
	}



}
