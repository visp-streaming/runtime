package at.tuwien.infosys.utility;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.datasources.ProcessingDurationRepository;
import at.tuwien.infosys.datasources.QueueMonitorRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.PooledVM;
import at.tuwien.infosys.entities.operators.Operator;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.resourceManagement.ResourcePoolConnector;
import at.tuwien.infosys.resourceManagement.ResourceProvider;
import at.tuwien.infosys.topology.TopologyManagement;
import at.tuwien.infosys.topology.TopologyParser;

@Service
public class Utilities {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    ProcessingNodeManagement processingNodeManagement;

    @Autowired
    TopologyParser parser;

    @Autowired
    ResourceProvider resourceprovider;

    @Autowired
    OperatorConfiguration opConfig;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private QueueMonitorRepository qmr;

    @Autowired
    private ProcessingDurationRepository pcr;

    @Autowired
    private ScalingActivityRepository sar;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Value("${visp.topology}")
    private String topology;

    @Value("${visp.simulation}")
    private Boolean SIMULATION;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private ResourcePoolConnector rpc;
    
    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public void initializeTopology(DockerHost dh, String infrastructureHost) {
        for (Operator op : parser.getTopology().values()) {
            if (op.getName().equals("source")) {
                continue;
            }
            DockerContainer dc = opConfig.createDockerContainerConfiguration(op.getName());
            processingNodeManagement.scaleup(dc, dh, infrastructureHost);
        
        }
    }

    @PostConstruct
    public void createInitialStatus() {

        parser.loadTopology("topologyConfiguration/" + topology + ".conf");
        resetPooledVMs();
        dhr.deleteAll();
        dcr.deleteAll();
        qmr.deleteAll();
        pcr.deleteAll();
        sar.deleteAll();


        topologyMgmt.cleanup(infrastructureHost);
        topologyMgmt.createMapping(infrastructureHost);

        if (!SIMULATION) {

            DockerHost dh = new DockerHost("initialhost");
            dh.setFlavour("m2.medium");
            dh = resourceprovider.get().startVM(dh);

            initializeTopology(dh, infrastructureHost);
        }
    }
    
    private void resetPooledVMs() {
        for(PooledVM vm : pvmr.findAll()) {
            if (!dhr.findByName(vm.getLinkedhost()).isEmpty()) {
                rpc.stopDockerHost(dhr.findByName(vm.getLinkedhost()).get(0));
            }

            vm.setLinkedhost(null);
            pvmr.save(vm);
        }
    }

}
