package ac.at.tuwien.infosys.visp.runtime.resourceManagement;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.Sink;
import ac.at.tuwien.infosys.visp.common.operators.Source;
import ac.at.tuwien.infosys.visp.common.resources.ResourcePoolUsage;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.monitoring.ResourceUsage;
import ac.at.tuwien.infosys.visp.runtime.reasoner.ReasonerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@DependsOn("configurationprovider")
public class ManualOperatorManagement {

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private ResourceUsage resourceUsage;

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Autowired
    private Configurationprovider config;

    private static final Logger LOG = LoggerFactory.getLogger(ManualOperatorManagement.class);

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        pcm.removeContainerWhichAreFlaggedToShutdown();
    }

    public synchronized void addOperator(Operator op) {

        if (op instanceof Source) {
            //Do not spawn container for sources
            return;
        }

        if (op instanceof Sink) {
            //Do not spawn container for sinks
            return;
        }

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

    public synchronized void removeOperators(Operator op, String resourcePool) {
        pcm.removeAll(op, resourcePool);
    }

    /**
     *
     * @param ops List of operators which are intended to be deployed
     * @return either "ok" or a string containing error messages splitted by newlines
     */
    public String testDeployment(List<Operator> ops) {

        Boolean deploymentPossible = true;
        List<String> errorMessages = new ArrayList<>();

        Map<String, String> pools = resourceProvider.getResourceProviders();

        for (Operator op : ops) {
            //check if all operators are assigned to the correct runtime
            if (!op.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                errorMessages.add("Operator \"" + op.getName() + "\" is assigned to another VISP runtime \"" + op.getConcreteLocation().getIpAddress() + "\"");
                deploymentPossible = false;
            }

            //check if all resource pools are available
            if (!pools.containsKey(op.getConcreteLocation().getResourcePool())) {
                errorMessages.add("Resourcepool \"" + op.getConcreteLocation().getResourcePool() + "\" for operator \"" + op.getName() + "\"");
                deploymentPossible = false;
            }
        }

        if (!deploymentPossible) {
            return String.join(";", errorMessages);
        }

        //check the resource requirements for the new operators for each pool respectively
        for (String pool : resourceProvider.getResourceProviders().keySet()) {
            List<Operator> currentOps = new ArrayList<>();

            for (Operator op : ops) {
                if (op.getConcreteLocation().getResourcePool().equals(pool)) {
                    currentOps.add(op);
                }
            }

            ResourceTriple requiredResources = new ResourceTriple();
            for (Operator op : currentOps) {
                DockerContainer dc = opConfig.createDockerContainerConfiguration(op);
                requiredResources.increment(dc.getCpuCores(), dc.getMemory(), Float.valueOf(dc.getStorage()));
            }

            ResourcePoolUsage usage = resourceUsage.calculateUsageForPool(pool);

            /*
            //Activate when also considering already running nodes
            ResourceTriple availableResources = new ResourceTriple(
                    usage.getOverallResources().getCores() - usage.getPlannedResources().getCores(),
                    usage.getOverallResources().getMemory() - usage.getPlannedResources().getMemory(),
                    usage.getOverallResources().getStorage() - usage.getPlannedResources().getStorage());
            */

            ResourceTriple availableResources = usage.getOverallResources();

            if (availableResources.getCores()<requiredResources.getCores()) {
                errorMessages.add("Resourcepool \"" + pool + "\" has too little CPU resources.");
                deploymentPossible = false;
            }

            if (availableResources.getMemory()<requiredResources.getMemory()) {
                errorMessages.add("Resourcepool \"" + pool + "\" has too little memory resources.");
                deploymentPossible = false;
            }

            if (availableResources.getStorage()<requiredResources.getStorage()) {
                errorMessages.add("Resourcepool \"" + pool + "\" has too little storage resources.");
                deploymentPossible = false;
            }

            if (!deploymentPossible) {
                return String.join(";\n", errorMessages);
            }
        }

        return "ok";
    }


}
