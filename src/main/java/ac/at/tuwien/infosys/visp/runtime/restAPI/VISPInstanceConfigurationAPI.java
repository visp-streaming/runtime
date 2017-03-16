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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

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
        List<ResourcePoolUsage> poolusage = new ArrayList<>();

        for (String pool : pvmr.findDistinctPoolnames()) {
            poolusage.add(resourceUsage.calculateUsageForPool(pool));
        }

        return poolusage;
    }

    @RequestMapping(value = {"/listConnections"}, method = RequestMethod.GET)
    public List<VISPConnectionDTO> connections() {

        List<VISPConnection> cons = (List<VISPConnection>) vcr.findAll();

        if (cons.isEmpty()) {
            vispConnectionsGenerator.generateConnections();
            cons = (List<VISPConnection>) vcr.findAll();
        }

        List<VISPConnectionDTO> result = new ArrayList<>();

        for (VISPConnection con : cons) {
            result.add(new VISPConnectionDTO(con.getStart(), con.getEnd(), con.getDelay(), con.getDataRate(), con.getAvailability()));
        }

        return result;
    }



}
