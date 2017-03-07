package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.DockerContainerManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.CreatePooledvmForm;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.PooledVMDTO;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PoolController {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private OpenstackConnector opc;

    @Autowired
    private ResourceProvider rp;

    @Autowired
    private DockerContainerManagement dcm;

    @Value("${visp.runtime.ip}")
    private String runtimeip;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/pooledvms")
    public String index(Model model) throws SchedulerException {
        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("pools", vms);
        return "pooledvms";
    }

    private List<PooledVMDTO> checkAvailablilityOfPooledVMs() {
        List<PooledVMDTO> vms = new ArrayList<>();

        for (PooledVM pvm : pvmr.findAll()) {
            PooledVMDTO vm = new PooledVMDTO(pvm.getId(), pvm.getPoolname(), pvm.getName(), pvm.getUrl(), pvm.getFlavour(), true);

            if (!dcm.checkAvailabilityofDockerhost(pvm.getUrl())) {
                vm.setAvailable(false);
                LOG.error("The pooled VM with the URL " + pvm.getUrl() + " is not available.");
            };
            vms.add(vm);
        }
        return vms;
    }

    @RequestMapping("/pooledvms/addpooledvm")
    public String run(Model model) {

        CreatePooledvmForm form = new CreatePooledvmForm();
        form.setCost(1.5);
        form.setFlavour("m2.medium");

        model.addAttribute("flavours", opc.getFlavours());
        model.addAttribute(form);

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        return "createPooledvm";
    }

    @RequestMapping(value="/pooledvms/createPooledvm", method= RequestMethod.POST)
    public String userCreated(@ModelAttribute CreatePooledvmForm form, Model model) throws SchedulerException {

        DockerHost dh = new DockerHost("dockerhost");
        dh.setFlavour(form.getFlavour());

        dh = opc.startVM(dh);
        PooledVM pvm = new PooledVM();
        pvm.setName(form.getPoolname() + "-" + dh.getName());
        pvm.setPoolname(form.getPoolname());
        pvm.setUrl(dh.getUrl());
        pvm.setCores(dh.getCores());
        pvm.setMemory(dh.getMemory());
        pvm.setStorage(dh.getStorage());
        pvm.setFlavour(dh.getFlavour());
        pvm.setCost(form.getCost());
        //TODO configure
        pvm.setCpuFrequency(2400);
        pvmr.save(pvm);

        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("message", "A new pooledVM has beeen started.");
        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("pools", vms);

        rp.updateResourceProvider();
        return "pooledvms";
    }

    @RequestMapping("/pooledvms/kill/{id}")
    public String utilKillSingle(Model model, @PathVariable String id)  {

        PooledVM pvm = pvmr.findFirstById(Long.valueOf(id));

        if (dhr.findFirstByName(pvm.getName()) != null) {
            model.addAttribute("message", "The pooled VM cannot not be deleted because there are still instances running.");
        } else {
            pvmr.delete(pvm);
            model.addAttribute("message", "The pooled VM has been deleted.");
        }


        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("pools", vms);

        rp.updateResourceProvider();
        return "pooledvms";
    }

    @RequestMapping("/pooledvms/killall")
    public String utilKillAll(Model model)  {

        for (PooledVM pvm : pvmr.findAll()) {
            if (dhr.findFirstByName(pvm.getName()) != null) {
                model.addAttribute("message", "The pooled VMs cannot not be deleted because there are still instances running.");
                model.addAttribute("pools", pvmr.findAll());
                return "pooledvms";
            } else {
                pvmr.delete(pvm);
            }
        }

        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        model.addAttribute("pools", pvmr.findAll());
        model.addAttribute("message", "The pooled VMs have been deleted.");

        rp.updateResourceProvider();
        return "pooledvms";
    }

}
