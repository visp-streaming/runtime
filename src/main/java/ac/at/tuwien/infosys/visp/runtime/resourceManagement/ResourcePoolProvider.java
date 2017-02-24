package ac.at.tuwien.infosys.visp.runtime.resourceManagement;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.monitoring.ResourceUsage;
import ac.at.tuwien.infosys.visp.runtime.reasoner.ReasonerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourcePoolProvider {

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private ResourceUsage resourceUsage;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Autowired
    private ResourceProvider resourceProvider;

    @Value("${visp.reasoner}")
    private String reasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ResourcePoolProvider.class);


    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        pcm.removeContainerWhichAreFlaggedToShutdown();
    }

    public synchronized void addOperator(Operator op) {

        try {
            if (op.getSize()!= null) {
                switch (op.getSize()) {
                    case SMALL: pcm.scaleup(selectSuitableDockerHost(op), op); break;
                    case MEDIUM:
                        pcm.scaleup(selectSuitableDockerHost(op), op);
                        pcm.scaleup(selectSuitableDockerHost(op), op); break;
                    case LARGE:
                        pcm.scaleup(selectSuitableDockerHost(op), op);
                        pcm.scaleup(selectSuitableDockerHost(op), op);
                        pcm.scaleup(selectSuitableDockerHost(op), op);
                        pcm.scaleup(selectSuitableDockerHost(op), op);
                        break;
                    case UNKNOWN: pcm.scaleup(selectSuitableDockerHost(op), op); break;
                }
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public synchronized void removeOperators(Operator op) {
        pcm.removeAll(op);
    }

    public Boolean testDeploment(String resourcepool, List<Operator> ops) {
        ResourceTriple usage = resourceUsage.calculateUsageForPool(resourcepool).getPlannedResources();

        for (Operator op : ops) {
            DockerContainer dc = opConfig.createDockerContainerConfiguration(op.getType());
            try {
                usage.decrementCores(dc.getCpuCores());
                usage.decrementMemory(dc.getMemory());
                usage.decrementStorage(Float.valueOf(dc.getStorage()));
            } catch (Exception e) {
                LOG.warn(e.getLocalizedMessage());
                return false;
            }
        }
        return true;
    }

    private DockerHost selectSuitableDockerHost(Operator op) throws Exception {
        DockerContainer dc = opConfig.createDockerContainerConfiguration(op.getType());

        for (DockerHost dh : dhr.findByResourcepool(op.getConcreteLocation().getResourcePool())) {
            if (reasonerUtility.checkDeployment(dc, dh)) {
                return dh;
            }
        }
        DockerHost dh =  resourceProvider.get(op.getConcreteLocation().getResourcePool()).startVM(null);
        if (dh!=null) {
            return dh;
        }

        throw new Exception("not enough resources available");
    }

}
