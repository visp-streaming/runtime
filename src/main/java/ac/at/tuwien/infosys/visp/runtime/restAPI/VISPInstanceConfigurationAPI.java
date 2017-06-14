package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.common.resources.ResourcePoolUsage;
import ac.at.tuwien.infosys.visp.common.resources.VISPConnectionDTO;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPConnectionRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPInstanceRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPConnection;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPInstance;
import ac.at.tuwien.infosys.visp.runtime.monitoring.ResourceUsage;
import ac.at.tuwien.infosys.visp.runtime.utility.GenerateDataForDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/vispconfiguration")
@RestController
public class VISPInstanceConfigurationAPI {

    @Autowired
    private VISPConnectionRepository vcr;

    @Autowired
    private VISPInstanceRepository vir;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private ResourceUsage resourceUsage;

    @Autowired
    private GenerateDataForDB vispConnectionsGenerator;

    @RequestMapping(value = {"/listInstances"}, method = RequestMethod.GET)
    public List<VISPInstance> listInstances() {

        List<VISPInstance> instances = (List<VISPInstance>) vir.findAll();

        if (instances.isEmpty()) {
            vispConnectionsGenerator.generateInstances();
            instances = (List<VISPInstance>) vir.findAll();
        }

        return instances;
    }

    @RequestMapping(value = {"/listResourcePools"}, method = RequestMethod.GET)
    public List<ResourcePoolUsage> instantiatedServices() {
        return pvmr.findDistinctPoolnames()
                .stream().map(i -> resourceUsage.calculateUsageForPool(i)).collect(Collectors.toList());
    }

    @RequestMapping(value = {"/listConnections"}, method = RequestMethod.GET)
    public List<VISPConnectionDTO> connections() {

        vcr.deleteAll();
        vispConnectionsGenerator.generateConnections();

        return ((List<VISPConnection>) vcr.findAll()).stream()
                .map(i -> new VISPConnectionDTO(i.getStart(), i.getEnd(), i.getDelay(), i.getDataRate(), i.getAvailability()))
                .collect(Collectors.toList());
    }

}
