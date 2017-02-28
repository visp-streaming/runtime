package ac.at.tuwien.infosys.visp.runtime.utility;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.datasources.*;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.reporting.ReportingScalingActivities;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ManualOperatorManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.ResourcePoolConnector;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
@DependsOn("resourceProvider")
public class Utilities {

    @Autowired
    private ReportingScalingActivities rsa;

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private TopologyParser parser;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerMonitorRepository dcmr;

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

    @Autowired
    private ReportingCompressor compressor;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Value("${visp.topology}")
    private String topology;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private ResourcePoolConnector rpc;

    @Autowired
    private ManualOperatorManagement rpp;

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public void initializeTopology() {
        for (Operator op : parser.getTopology().values()) {
            if (op.getName().contains("source")) {
                continue;
            }
            rpp.addOperator(op);
        }
    }

    @PostConstruct
    public void createInitialStatus() {
        LOG.info("Deleting old configurations");
        parser.loadTopologyFromClasspath("topologyConfiguration/" + topology + ".conf");
        resetPooledVMs();
        dhr.deleteAll();
        dcr.deleteAll();
        qmr.deleteAll();
        pcr.deleteAll();
        dcmr.deleteAll();

        appMetRepos.deleteAll();
        opeMetRepos.deleteAll();
        opeReplRepos.deleteAll();

        resetPooledVMs();
        sar.deleteAll();

        template.getConnectionFactory().getConnection().flushAll();
        topologyMgmt.cleanup(infrastructureHost);

        LOG.info("Cleanup Completed");

        topologyMgmt.createMapping(infrastructureHost);
        //initializeTopology();
    }

    @PreDestroy
    public void exportData() {
        rsa.generateCSV();
        compressor.zipIt();
        compressor.cleanup();
    }

    private void resetPooledVMs() {
        for (PooledVM vm : pvmr.findAll()) {
            if (dhr.findFirstByName(vm.getLinkedhost()) != null) {
                rpc.stopDockerHost(dhr.findFirstByName(vm.getLinkedhost()));
            }
            vm.setLinkedhost(null);
            pvmr.save(vm);
        }
    }

}
