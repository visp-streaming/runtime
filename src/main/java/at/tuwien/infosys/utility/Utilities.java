package at.tuwien.infosys.utility;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.*;
import at.tuwien.infosys.datasources.entities.DockerContainer;
import at.tuwien.infosys.datasources.entities.PooledVM;
import at.tuwien.infosys.entities.operators.Operator;
import at.tuwien.infosys.reasoner.ReasonerBasic;
import at.tuwien.infosys.reporting.ReportingScalingActivities;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.resourceManagement.ResourcePoolConnector;
import at.tuwien.infosys.resourceManagement.ResourceProvider;
import at.tuwien.infosys.topology.TopologyManagement;
import at.tuwien.infosys.topology.TopologyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class Utilities {

    @Autowired
    private ReportingScalingActivities rsa;

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
    private ApplicationQoSMetricsRepository appMetRepos;
    
    @Autowired
    private OperatorQoSMetricsRepository opeMetRepos;
    
    @Autowired
    private OperatorReplicationReportRepository opeReplRepos;    

    @Autowired
    private ScalingActivityRepository sar;

    @Autowired
    private StringRedisTemplate template;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Value("${visp.topology}")
    private String topology;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private ResourcePoolConnector rpc;

    @Autowired
    private ReasonerBasic basicReasoner;
    
    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public void initializeTopology(String infrastructureHost) {
        for (Operator op : parser.getTopology().values()) {
            if (op.getName().contains("source")) {
                continue;
            }
            DockerContainer dc = opConfig.createDockerContainerConfiguration(op.getName());

            processingNodeManagement.scaleup(dc, basicReasoner.selectSuitableDockerHost(dc, null), infrastructureHost);
        }
    }

    @PostConstruct
    public void createInitialStatus() {

    	LOG.info("Deleting old configurations");
        parser.loadTopology("topologyConfiguration/" + topology + ".conf");
        resetPooledVMs();
        dhr.deleteAll();
        dcr.deleteAll();
        qmr.deleteAll();
        pcr.deleteAll();

        appMetRepos.deleteAll();
        opeMetRepos.deleteAll();
        opeReplRepos.deleteAll();

        resetPooledVMs();
        sar.deleteAll();

        template.getConnectionFactory().getConnection().flushAll();
        topologyMgmt.cleanup(infrastructureHost);

        LOG.info("Cleanup Completed");

        topologyMgmt.createMapping(infrastructureHost);
        initializeTopology(infrastructureHost);
    }

    @PreDestroy
    public void exportData() {
        rsa.generateCSV();
    }

    private void resetPooledVMs() {
        for(PooledVM vm : pvmr.findAll()) {
            if (dhr.findFirstByName(vm.getLinkedhost()) != null) {
                rpc.stopDockerHost(dhr.findFirstByName(vm.getLinkedhost()));
            }

            vm.setLinkedhost(null);
            pvmr.save(vm);
        }
    }

}
