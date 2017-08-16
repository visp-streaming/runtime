package ac.at.tuwien.infosys.visp.runtime.reasoner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.BTULoggingRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.QueueMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.BTULogging;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ProcessingDuration;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.QueueMonitor;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.exceptions.ResourceException;
import ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal.LeastLoadedHostFirstComparator;
import ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal.ResourceAvailability;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReasonerUtility {

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private TopologyManagement tmgmt;

    @Autowired
    private ProcessingDurationRepository pcr;

    @Autowired
    private ScalingActivityRepository scr;

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private QueueMonitorRepository qmr;

    @Autowired
    private BTULoggingRepository btur;

    @Autowired
    private ScalingActivityRepository sar;


    @Value("${visp.relaxationfactor}")
    private Double relaxationfactor;

    @Value("${visp.penaltycosts}")
    private Double penaltycosts;


    private static final Logger LOG = LoggerFactory.getLogger(ReasonerUtility.class);

    public Boolean checkDeployment(DockerContainer dc, DockerHost dh) {
        if (dh.getScheduledForShutdown()) {
            return false;
        }
        ResourceTriple ra = calculateFreeResourcesforHost(dh);
        return 1 <= Math.min(ra.getCores() / dc.getCpuCores(), ra.getMemory() / dc.getMemory());
    }


    public ResourceTriple calculateFreeResourcesforHost(DockerHost dh) {
        ResourceTriple rc = new ResourceTriple(dh.getCores(), dh.getMemory(), dh.getStorage());

        dcr.findByHost(dh.getName()).stream()
                .forEach(i -> rc.decrement(i.getCpuCores(), i.getMemory(), Float.valueOf(i.getStorage())));

        return rc;
    }

    public Map<DockerHost, ResourceTriple> calculateFreeResourcesforHosts(DockerHost blacklistedHost) {
        Map<DockerHost, ResourceTriple> freeResources = new HashMap<>();

        Map<String, ResourceTriple> hostResourceUsage = StreamSupport.stream(dhr.findAll().spliterator(), false)
                .collect(Collectors.toMap(i -> i.getName(), i -> new ResourceTriple(i.getCores(), i.getMemory(), i.getStorage())));

        //collect current usage of cloud resources
        for (DockerContainer dc : dcr.findAll()) {
            if (dc.getStatus().equals("stopping") || dc.getOperatorType().equals("sink")) {
                continue;
            }

            ResourceTriple rc = hostResourceUsage.get(dc.getHost());
            if (rc != null) {
                rc.decrement(dc.getCpuCores(), dc.getMemory(), Float.valueOf(dc.getStorage()));
                hostResourceUsage.put(dc.getHost(), rc);
            }
        }

        //calculate how much resources are left on a specific host

        for (Map.Entry<String, ResourceTriple> entry : hostResourceUsage.entrySet()) {
            DockerHost dh = dhr.findFirstByName(entry.getKey());

            if (dh.getScheduledForShutdown()) {
                LOG.info("Omitted host: " + dh.getName() + " for scheduling, since it is scheduled to shut down.");
                continue;
            }

            if (blacklistedHost != null) {
                if (dh.getName().equals(blacklistedHost.getName())) {
                    LOG.info("Omitted host: " + dh.getName() + " for scheduling, since it is targeted to be shut down.");
                    continue;
                }
            }
            freeResources.put(dh, entry.getValue());
        }

        return freeResources;
    }


    /**
     * utility function optimization
     */
    public DockerHost selectSuitableHostforContainer(DockerContainer dc, DockerHost blacklistedHost) {
        LOG.info("##### select suitable host for Container (" + dc.getOperatorType() + ") started. ####");
        Double value = Double.MAX_VALUE;
        DockerHost selectedHost = null;

        for (Map.Entry<DockerHost, ResourceTriple> ras : calculateFreeResourcesforHosts(blacklistedHost).entrySet()) {
            DockerHost dh = ras.getKey();
            ResourceTriple ra = ras.getValue();

            Double feasibilityThreshold = Math.min(ra.getCores() / dc.getCpuCores(), ra.getMemory() / dc.getMemory());

            if (feasibilityThreshold < 1) {
                continue;
            }

            Integer remainingMemory = ra.getMemory() - dc.getMemory();
            Double remainingCpuCores = ra.getCores() - dc.getCpuCores();

            Double difference = Math.abs((remainingMemory / dh.getMemory()) - (remainingCpuCores / dh.getCores()));

            Double suitablility = difference / feasibilityThreshold;

            if (!dh.getAvailableImages().contains(dc.getImage())) {
                suitablility = suitablility / 100;
            }

            if (suitablility < value) {
                if (value > 0) {
                    value = suitablility;
                    selectedHost = dh;
                }
            }
        }

        LOG.info("##### select suitable host for Container (" + dc.getOperatorType() + ") finished with host (" + selectedHost +"). ####");
        return selectedHost;
    }


    public TreeMap<String, Double> selectOperatorTobeScaledDown() {
        TreeMap<String, Double> selectedOperators = new TreeMap<>();

        //get max/min of all operator instances
        Integer maxInstances = Integer.MIN_VALUE;
        Integer minInstances = 0;
        Map<String, Integer> operatorInstances = new HashMap<>();

        // select all instances that have more than one instances
        for (String operator : tmgmt.getOperatorsAsList()) {
            Integer amount = dcr.findByOperatorNameAndStatus(operator, "running").size();
            if (amount < 2) {
                continue;
            }
            operatorInstances.put(operator, amount);
            if (amount > maxInstances) {
                maxInstances = amount;
            }
            if ((amount < minInstances) && (amount > 0)) {
                minInstances = amount;
            }
        }

        if (operatorInstances.isEmpty()) {
            return null;
        }

        Long totalScalingActions = scr.count();
        Double selectionValue = 0.0;

        for (Map.Entry<String, Integer> operatortype : operatorInstances.entrySet()) {
            String op = operatortype.getKey();
            // calculate instances impact factor
            Double instancefactor = (double)(operatortype.getValue() - minInstances) / (double)(maxInstances - minInstances) * 1.0;
            LOG.debug("InstanceFactor: # = " + operatortype.getValue() + ", " + "min = " + minInstances + ", " + "max = " + maxInstances);

            //calculate qos impact factor
            List<ProcessingDuration> pds = pcr.findFirst5ByOperatortypeOrderByIdDesc(op);

            Double avgDuration = 0.0;
            Integer counter = 0;
            Boolean outdated = false;
            for (ProcessingDuration pd : pds) {
                if (pd.getTime().isBefore(new DateTime().minusMinutes(2))) {
                    outdated = true;
                }
                avgDuration += pd.getDuration();
                counter++;
            }
            avgDuration = avgDuration / counter;

            if (outdated) {
                avgDuration = 1.0;
            }

            //calculate delayfactor
            Double expectedDuration = ((ProcessingOperator)topologyMgmt.getOperatorByIdentifier(operatortype.getKey())).getExpectedDuration();
            if (expectedDuration == Double.NaN) {
                expectedDuration = 100.0;
            }

            Double delayFactor = (avgDuration / expectedDuration * relaxationfactor) * (1 + penaltycosts);
            LOG.debug("DurationFactor: avgDuration = " + avgDuration + ", " + "expectedDuration = " + expectedDuration + ", " + "relaxation = " + relaxationfactor + ", " + "penaltycost = " + penaltycosts);

            if (delayFactor == Double.NaN) {
                delayFactor = 0.0;
            }

            //calculate scaling actions factor
            Long scalings = scr.countByOperator(operatortype.getKey());
            Double scalingFactor = Double.valueOf(scalings) / Double.valueOf(totalScalingActions);
            LOG.debug("ScalingFactor: scalingOperations = " + scalings + ", " + "totalScalings = " + totalScalingActions);

            QueueMonitor qm = qmr.findFirstByOperatorOrderByIdDesc(op);
            Integer queueFactor = 0;
            if (qm!=null) {
                if (qm.getAmount() < 1) {
                    queueFactor = 100;
                }
            }

            Double instanceFactorWeighted = instancefactor * 1.0;
            Double delayFactorWeighted = delayFactor * 1.0;
            Double scalingFactorWeighted = scalingFactor * 1.0;


            //scaling prohibition factor: if upscaling occured within last 3 minutes - do not scale down

            Double stopScaling = 0.0;
            if (!checkLastScalingForDownscaling(op)) {
                stopScaling = 200.0;
            }


            Double overallFactor = 1 + instanceFactorWeighted - delayFactorWeighted - scalingFactorWeighted + queueFactor - stopScaling;
            LOG.info("Downscaling - overallfactor for " + op + " : overall = " + overallFactor + ", " + "instanceFactor = " + instancefactor + "(w=" + instancefactor * 1.0 + ")" + ", " + "delayFactor = " + delayFactor + "(w=" + delayFactor * 1.0 + ")" + ", " + "scalingFactor = " + scalingFactor + "(w=" + scalingFactor * 1.0 + ")" + "queuefactor = " + queueFactor + "(w=" + queueFactor * 1.0 + ")");


            try {
                BTULogging btuLogging = new BTULogging(op, overallFactor, instancefactor, instanceFactorWeighted, operatortype.getValue(), maxInstances, minInstances, delayFactor, delayFactorWeighted, expectedDuration, avgDuration, relaxationfactor, penaltycosts, scalingFactor, scalingFactorWeighted, scalings, totalScalingActions, queueFactor, qm.getAmount());
                btur.save(btuLogging);
            } catch (Exception e) {
                LOG.error("could not save BTU logging information");
            }

            if (overallFactor < 0) {
                continue;
            }

            if (overallFactor > selectionValue) {
                selectionValue = overallFactor;
                selectedOperators.put(op, selectionValue);
            }
        }

        return selectedOperators;
    }

    
    public Map<DockerContainer, DockerHost> canRelocateHostedContainers(ResourceAvailability resource, List<ResourceAvailability> availableResources){
    	
    	Map<DockerContainer, DockerHost> relocationMap = new HashMap<>();
    	boolean canRelocate = false;
    	
    	/* Retrieve containers to be relocated */
    	List<DockerContainer> containers = dcr.findByHost(resource.getHost().getName());
    	
    	/* Simulate container relocation */
    	List<ResourceAvailability> resources = new ArrayList<>();
    	for (ResourceAvailability ra : availableResources)
    		resources.add(ra.clone());
    	
    	/* Check if every hosted container can be relocated */
    	for (DockerContainer container : containers){
    		
    		canRelocate = false;

    		/* Sort resources w.r.t. already hosted containers */
    		resources.sort(new LeastLoadedHostFirstComparator());
    		
    		for (ResourceAvailability otherResource : resources){
    			
    			if (resource.equals(otherResource))
    				continue;
    			
    			/* Check resources */
    			if ((otherResource.getCores() - container.getCpuCores()) > 0 &&
    				(otherResource.getMemory() - container.getMemory()) > 0 &&
    				(otherResource.getStorage() - container.getStorage()) > 0){
    				
        			/* Simulate relocation */
    				canRelocate = true;
    				resource.setAmountOfContainer(resource.getAmountOfContainer() + 1);
    				resource.setCores(resource.getCores() + container.getCpuCores());
    				resource.setMemory(resource.getMemory() + container.getMemory());
    				resource.setStorage(resource.getStorage() + container.getStorage());
    				
    				/* Save relocation on the relocation map */
    				relocationMap.put(container, resource.getHost());
    				
    				break;
    			}
    		}

    		/* Current container cannot be relocated */
    		if (!canRelocate)
    			return null;
    		
    	}
    	
    	/* If every container can be relocate, return the relocation map */
    	if (canRelocate)
    		return relocationMap;
    	    	
    	return null;
    	
    }


    /**
     * simple selection mechanism to select the next best dockerhost for a current VISP instance considering all resources pools, which are assigne to this instance
     *
     */
    public DockerHost selectSuitableDockerHost(Operator op) throws ResourceException {
        DockerContainer dc = opConfig.createDockerContainerConfiguration(op);

        for (DockerHost dh : dhr.findByResourcepool(op.getConcreteLocation().getResourcePool())) {
            if (checkDeployment(dc, dh)) {
                return dh;
            }
        }
        DockerHost dh =  resourceProvider.get(op.getConcreteLocation().getResourcePool()).startVM(null);
        if (dh != null) {
            return dh;
        }

        throw new ResourceException("not enough resources available");
    }


    /*
    check if operator has been scaled up just recently and require a 3 minute cooldown phase for downscaling
     */
    public Boolean checkLastScalingForDownscaling(String operator) {
        ScalingActivity sa = sar.findFirstByOperatorAndScalingActivityOrderByIdDesc(operator, "scaleup");

        if (sa == null) {
            return true;
        }

        if (sa.getTime().plusMinutes(3).isAfter(new DateTime(DateTimeZone.UTC))) {
            return false;
        }

        return true;
        }


}
