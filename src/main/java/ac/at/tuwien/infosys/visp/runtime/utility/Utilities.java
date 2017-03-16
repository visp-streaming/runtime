package ac.at.tuwien.infosys.visp.runtime.utility;

import ac.at.tuwien.infosys.visp.runtime.datasources.*;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.reporting.ReportingScalingActivities;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.ResourcePoolConnector;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import com.spotify.docker.client.exceptions.DockerRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.InternalServerErrorException;
import java.util.LinkedHashMap;

@Service
@DependsOn("resourceProvider")
public class Utilities {

    @Autowired
    private ReportingScalingActivities rsa;

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

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private RuntimeConfigurationRepository rcr;

    @Value("${visp.infrastructure.ip}")
    private String infrastructureIp;

    @Value("${visp.topology}")
    private String topology;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private ResourcePoolConnector rpc;

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    @PostConstruct
    public void init() {
        topologyManagement.createMapping(infrastructureIp);
        topologyManagement.setTopology(new LinkedHashMap<>());

        // if the topology has been restarted, try to restore topology

        if(!topologyManagement.restoreTopologyFromPeers()) {
            topologyManagement.restoreTopologyFromDatabase();
        }
    }

    public void clearAll() {

        //this operation is a hard reset and also
        //removed the docker containers there

        try {

            LOG.info("Deleting old configurations");
            resetPooledVMs();
            dhr.deleteAll();
            dcr.deleteAll();
            qmr.deleteAll();
            pcr.deleteAll();
            dcmr.deleteAll();

            appMetRepos.deleteAll();
            opeMetRepos.deleteAll();
            opeReplRepos.deleteAll();

            sar.deleteAll();

            template.getConnectionFactory().getConnection().flushAll();
            topologyManagement.cleanup(infrastructureIp);
            topologyManagement.setTopology(new LinkedHashMap<>());

            try {
                rcr.delete(rcr.findFirstByKey("last_topology_file").getId());
            } catch(Exception e) {

            }
        } catch(InternalServerErrorException e) {
            LOG.error(e.getLocalizedMessage(), e.getCause());
        }
        LOG.info("Cleanup Completed");
    }

    public void exportData() {
        //TODO provide HTML interface to export Data
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
