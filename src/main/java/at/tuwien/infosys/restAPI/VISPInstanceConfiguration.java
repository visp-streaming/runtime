package at.tuwien.infosys.restAPI;

import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.datasources.VISPConnectionRepository;
import at.tuwien.infosys.datasources.VISPInstanceRepository;
import at.tuwien.infosys.datasources.entities.VISPInstance;
import at.tuwien.infosys.entities.ResourcePoolUsage;
import at.tuwien.infosys.datasources.entities.VISPConnection;
import at.tuwien.infosys.monitoring.ResourceUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/vispconfiguration")
@RestController
public class VISPInstanceConfiguration {

    @Autowired
    private VISPConnectionRepository vcr;

    @Autowired
    private VISPInstanceRepository vir;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private ResourceUsage resourceUsage;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = {"/registerVISPInstance/{instanceURI}"}, method = RequestMethod.PUT)
    public void registerInstance(@PathVariable String instanceURI) {
        VISPInstance vi = new VISPInstance(instanceURI);
        vir.save(vi);
    }

    @RequestMapping(value = {"/unregisterVISPInstance/{instanceURI}"}, method = RequestMethod.DELETE)
    public void unregisterInstance(@PathVariable String instanceURI) {
        VISPInstance vi = vir.findFirstByUri(instanceURI);
        vir.delete(vi);
    }


    @RequestMapping(value = {"/listVISPInstances"}, method = RequestMethod.GET)
    public List<VISPInstance> listInstances() {
        return (List<VISPInstance>) vir.findAll();
    }

    @RequestMapping(value = {"/listResourcePools"}, method = RequestMethod.GET)
    public List<ResourcePoolUsage> instantiatedServices() {
        List<ResourcePoolUsage> poolusage = new ArrayList<>();

        for (String pool : pvmr.findDistinctPoolnames()) {
            poolusage.add(resourceUsage.calculateUsageForPool(pool));
        }

        return poolusage;
    }

    @RequestMapping(value = {"/VISPconnections"}, method = RequestMethod.GET)
    public List<VISPConnection> connections() {

        //TODO implement probing component to gather the delays and datarates among different VISP instances

        List<VISPConnection> con = (List<VISPConnection>) vcr.findAll();

        if (con.isEmpty()) {
            con.add(new VISPConnection("dummy", "dummy", 2.0, 10.0));
        }

        return con;
    }



}
