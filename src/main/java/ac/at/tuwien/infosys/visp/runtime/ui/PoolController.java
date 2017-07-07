package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.DockerContainerManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.AddCustomHostForm;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.CreateOpenStackVMForm;
import ac.at.tuwien.infosys.visp.runtime.ui.dto.PooledVMDTO;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@DependsOn("configurationprovider")
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

    @Autowired
    private ResourceProvider rpp;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/pooledvms")
    public String index(Model model) throws SchedulerException {
        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("hosts", vms);
        return "pooledvms";
    }

    private List<PooledVMDTO> checkAvailablilityOfPooledVMs() {
        List<PooledVMDTO> vms = new ArrayList<>();

        for (PooledVM pvm : pvmr.findAll()) {
            PooledVMDTO vm = new PooledVMDTO(pvm.getId(), pvm.getPoolname(), pvm.getName(), pvm.getType(), pvm.getUrl(), pvm.getFlavour(), true);

            if (!dcm.checkAvailabilityofDockerhost(pvm.getUrl())) {
                vm.setAvailable(false);
                LOG.error("The VM with the URL " + pvm.getUrl() + " is not available.");
            }
            vms.add(vm);
        }
        return vms;
    }

    @RequestMapping("/pooledvms/addOpenStackVM")
    public String addOpenStackVM(Model model) {

        CreateOpenStackVMForm form = new CreateOpenStackVMForm();
        form.setCost(1.5);
        form.setFrequency(2400);
        form.setFlavour("m2.medium");
        form.setInstanceName("dockerhost");

        model.addAttribute("flavours", opc.getFlavours());
        model.addAttribute(form);

        return "createOpenStackVM";
    }

    @RequestMapping("/pooledvms/addCustomHost")
    public String addCustomHost(Model model) {

        AddCustomHostForm form = new AddCustomHostForm();
        form.setCost(1.5);
        form.setFrequency(2400);

        model.addAttribute(form);

        return "addCustomHost";
    }

    @RequestMapping(value="/pooledvms/createCustomHost", method= RequestMethod.POST)
    public String createCustomHost(@ModelAttribute AddCustomHostForm form, Model model) throws SchedulerException {

        PooledVM pvm = new PooledVM();
        pvm.setName(form.getInstanceName() + "--" + UUID.randomUUID().toString());
        pvm.setPoolname(form.getPoolname());
        pvm.setUrl(form.getUrl());
        pvm.setCores(form.getCores());
        pvm.setMemory(form.getMemory());
        pvm.setStorage(form.getStorage());
        pvm.setFlavour("custom");
        pvm.setCost(form.getCost());
        pvm.setType("custom");
        pvm.setCpuFrequency(form.getFrequency());
        pvmr.save(pvm);

        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("message", "A new custom host has been added.");
        model.addAttribute("hosts", vms);

        rp.updateResourceProvider();
        return "pooledvms";
    }

    @RequestMapping(value="/pooledvms/createOpenStackVM", method= RequestMethod.POST)
    public String createOpenStackVM(@ModelAttribute CreateOpenStackVMForm form, Model model) throws SchedulerException {

        DockerHost dh = new DockerHost(form.getInstanceName());
        dh.setFlavour(form.getFlavour());

        dh = opc.startVM(dh);
        PooledVM pvm = new PooledVM();
        pvm.setName(dh.getName());
        pvm.setPoolname(form.getPoolname());
        pvm.setUrl(dh.getUrl());
        pvm.setCores(dh.getCores());
        pvm.setMemory(dh.getMemory());
        pvm.setStorage(dh.getStorage());
        pvm.setFlavour(dh.getFlavour());
        pvm.setCost(form.getCost());
        pvm.setCpuFrequency(form.getFrequency());
        pvm.setType("openstack");
        dhr.delete(dh); //delete host again from docker hosts table
        pvmr.save(pvm);

        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("message", "A new OpenStack VM has been started.");
        model.addAttribute("hosts", vms);

        rp.updateResourceProvider();
        return "pooledvms";
    }

    @RequestMapping("/pooledvms/kill/{id}")
    public String utilKillSingle(Model model, @PathVariable String id)  {

        PooledVM pvm = pvmr.findFirstById(Long.valueOf(id));

        if (dhr.findFirstByName(pvm.getName()) != null) {
            model.addAttribute("message", "The pooled VM cannot not be deleted because there are still instances running.");
        } else {
            if (pvm.getType().equals("openstack")) {
                opc.stopDockerHost(pvm.getName());
            }
            pvmr.delete(pvm);
            model.addAttribute("message", "The host has been removed.");
        }

        rpp.updateResourceProvider();
        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("hosts", vms);

        rp.updateResourceProvider();
        return "pooledvms";
    }

    @RequestMapping("/pooledvms/killall")
    public String utilKillAll(Model model)  {

        for (PooledVM pvm : pvmr.findAll()) {
            if (dhr.findFirstByName(pvm.getName()) != null) {
                model.addAttribute("message", "The hosts cannot not be removed because there are still operators running.");
                List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();
                model.addAttribute("hosts", vms);
                return "pooledvms";
            } else {
                if (pvm.getType().equals("openstack")) {
                    opc.stopDockerHost(pvm.getName());
                }
                pvmr.delete(pvm);
            }
        }

        rpp.updateResourceProvider();
        List<PooledVMDTO> vms = checkAvailablilityOfPooledVMs();

        model.addAttribute("hosts", vms);
        model.addAttribute("message", "All hosts have been removed.");

        rp.updateResourceProvider();
        return "pooledvms";
    }

}
