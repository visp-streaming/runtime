package ac.at.tuwien.infosys.visp.runtime.resourceManagement;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
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
public class ManualOperatorManagement {

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private ResourceUsage resourceUsage;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Value("${visp.reasoner}")
    private String reasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ManualOperatorManagement.class);

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        pcm.removeContainerWhichAreFlaggedToShutdown();
    }

    public synchronized void addOperator(Operator op) {

        try {
            if (op.getSize() == null) {
                pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
            } else {


                switch (op.getSize()) {
                    case SMALL: pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op); break;
                    case MEDIUM:
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op); break;
                    case LARGE:
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        break;
                    case UNKNOWN: pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op); break;
                    default:
                        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        break;

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


}
