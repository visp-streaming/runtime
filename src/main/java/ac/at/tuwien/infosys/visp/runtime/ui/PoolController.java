package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.ui.entities.CreatePooledvmForm;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());


    @RequestMapping("/pooledvms")
    public String index(Model model) throws SchedulerException {

        model.addAttribute("pools", pvmr.findAll());
        return "pooledvms";
    }

    @RequestMapping("/pooledvms/addpooledvm")
    public String run(Model model) {

        CreatePooledvmForm form = new CreatePooledvmForm();

        model.addAttribute("flavours", opc.getFlavours());
        model.addAttribute("cost", 1.5);
        model.addAttribute(form);

        model.addAttribute("message", null);
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

        model.addAttribute("message", "A new pooledVM has beeen started.");
        model.addAttribute("pools", pvmr.findAll());

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

        model.addAttribute("pools", pvmr.findAll());
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

        model.addAttribute("message", "The pooled VMs have been deleted.");
        model.addAttribute("pools", pvmr.findAll());
        rp.updateResourceProvider();
        return "pooledvms";
    }

}
