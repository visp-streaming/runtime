package ac.at.tuwien.infosys.visp.runtime.ui;

import java.util.List;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.DockerContainerManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/")
@DependsOn("configurationprovider")
public class ContainerController {

	@Autowired
	private Configurationprovider config;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerContainerManagement dcm;

    @RequestMapping("/container")
    public String containerOverview(Model model) {

        List<DockerContainer> dcs = dcr.findAll();

        model.addAttribute("containers", dcs);
        return "container";
    }

    @RequestMapping("/container/name/{name}")
    public String containerOverviewName(Model model, @PathVariable String name) {

        List<DockerContainer> dcs = dcr.findByOperatorName(name);

        model.addAttribute("containers", dcs);
        return "container";
    }

	@RequestMapping("/container/host/{id}")
	public String containerOverviewHost(Model model, @PathVariable String id) {

	    List<DockerContainer> dcs = dcr.findByHost(id);

        model.addAttribute("containers", dcs);
        return "container";
	}

    @RequestMapping("/container/kill/{id}")
    public String killContainer(Model model, @PathVariable Long id) {

        DockerContainer dc = dcr.findFirstById(id);

        dcm.removeContainer(dc);

        List<DockerContainer> dcs = dcr.findAll();

        model.addAttribute("message", "Container with name " + dc.getOperatorName() + " has been removed");
        model.addAttribute("containers", dcs);
        return "container";
    }


}
