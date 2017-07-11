package ac.at.tuwien.infosys.visp.runtime.monitoring;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import ac.at.tuwien.infosys.visp.runtime.exceptions.ResourceException;
import ac.at.tuwien.infosys.visp.runtime.reasoner.ReasonerUtility;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.DockerContainerManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ProcessingNodeManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;

@Service
public class AvailabilityWatchdog {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityWatchdog.class);

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private Configurationprovider config;

    @Autowired
    private DockerContainerManagement dcm;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Autowired
    private RuntimeConfigurationRepository rcr;

    @Autowired
    private TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    private Utilities utilities;

    @Scheduled(fixedRateString = "${visp.checkavilability.period}")
    public void checkAvailablitiyOfContainer() {
        for (Operator op : topologyMgmt.getOperatorsForAConcreteLocation(config.getRuntimeIP())) {
            if (op instanceof ProcessingOperator) {
                for (DockerContainer dc : dcr.findByOperatorNameAndStatus(op.getName(), "running")) {
                    if (!dcm.checkIfContainerIsRunning(dc)) {
                        LOG.info("Container \"" + dc.getContainerid() + "\" for operator \"" + op.getName() + "\" could not be reached.");
                        String compensation = ((ProcessingOperator) op).getCompensation();
                        if (compensation == null) {
                            compensation = "redeploySingle";
                        }
                        if ("redeploySingle".equals(compensation)) {
                            dcm.removeContainer(dc);
                            try {
                                pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                                LOG.info("Redeployed container for operator \"" + op.getName() + "\".");
                            } catch (ResourceException e) {
                                LOG.error(e.getMessage());
                            }
                            continue;
                        }

                        if ("redeployTopology".equals(compensation)) {
                            try {
                                RuntimeConfiguration rc = rcr.findFirstByKey("last_topology_file");
                                utilities.clearAll();
                                topologyUpdateHandler.handleUpdateFromUser(rc.getValue());
                                LOG.info("Redeployed complete topology.");
                            } catch (Exception e) {
                                LOG.error(e.getMessage());
                            }
                            continue;
                        }

                        if (compensation.startsWith("mailto:")) {
                            try {
                                String mailaddress = compensation.replace("mailto:", "");
                                //TODO send mail
                                LOG.info("Redeployed complete topology.");
                            } catch (Exception e) {
                                LOG.error(e.getMessage());
                            }
                            continue;
                        }

                        if (compensation.startsWith("deploy:")) {
                            try {
                                URL topology = new URL(compensation.replace("deploy:", ""));
                                String newTopology = IOUtils.toString(IOUtils.toByteArray(topology), "UTF-8");
                                topologyUpdateHandler.handleUpdateFromUser(newTopology);
                                LOG.info("Deployed new topology from URI.");
                            } catch (Exception e) {
                                LOG.error(e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }
}






