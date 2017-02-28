package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ProcessingDuration;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourceAvailability;
import ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal.LeastLoadedHostFirstComparator;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

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

    @Value("${visp.relaxationfactor}")
    private Double relaxationfactor;

    @Value("${visp.penaltycosts}")
    private Double penaltycosts;


    private static final Logger LOG = LoggerFactory.getLogger(ReasonerUtility.class);

    //TODO combine with resource monitor

    public ResourceAvailability calculateFreeresources(List<ResourceAvailability> resources) {
        ResourceAvailability all = new ResourceAvailability();
        all.setAmountOfContainer(0);
        all.setCpuCores(0.0);
        all.setMemory(0);
        all.setStorage(0.0F);


        LOG.info("###### free resources ######");
        for (ResourceAvailability ra : resources) {
            all.setAmountOfContainer(all.getAmountOfContainer() + ra.getAmountOfContainer());
            all.setCpuCores(all.getCpuCores() + ra.getCpuCores());
            all.setMemory(all.getMemory() + ra.getMemory());
            all.setStorage(all.getStorage() + ra.getStorage());

            LOG.info(ra.getHost().getName() + " - Container: " + ra.getAmountOfContainer() + " - CPU: " + ra.getCpuCores() + " - RAM: " + ra.getMemory() + " - Storage: " + ra.getStorage());
        }
        LOG.info("###### free resources ######");

        return all;
    }

    public Boolean checkDeployment(DockerContainer dc, DockerHost dh) {
        ResourceAvailability ra = calculateFreeResourcesforHost(dh);

        if (Math.min(ra.getCpuCores() / dc.getCpuCores(), ra.getMemory() / dc.getMemory()) < 1) {
            return false;
        }
        return true;
    }



    public ResourceAvailability calculateFreeResourcesforHost(DockerHost dh) {

        ResourceAvailability rc = new ResourceAvailability(dh, 0, 0.0, 0, 0.0F);

        for (DockerContainer dc : dcr.findByHost(dh.getName())) {
            rc.setAmountOfContainer(rc.getAmountOfContainer() + 1);
            rc.setCpuCores(rc.getCpuCores() + dc.getCpuCores());
            rc.setMemory(rc.getMemory() + dc.getMemory());
            rc.setStorage(rc.getStorage() + dc.getStorage());
        }

        ResourceAvailability availability = new ResourceAvailability();
        availability.setHost(dh);
        availability.setAmountOfContainer(rc.getAmountOfContainer());
        availability.setCpuCores(dh.getCores() - rc.getCpuCores());
        availability.setMemory(dh.getMemory() - rc.getMemory());
        availability.setStorage(dh.getStorage() - rc.getStorage());

        return availability;
    }


    public List<ResourceAvailability> calculateFreeResourcesforHosts(DockerHost blacklistedHost) {
        Map<String, ResourceAvailability> hostResourceUsage = new HashMap<>();
        List<ResourceAvailability> freeResources = new ArrayList<>();

        for (DockerHost dh : dhr.findAll()) {
            ResourceAvailability rc = new ResourceAvailability(dh, 0, 0.0, 0, 0.0F);
            hostResourceUsage.put(dh.getName(), rc);
        }

        //collect current usage of cloud resources
        for (DockerContainer dc : dcr.findAll()) {
            if (dc.getStatus().equals("stopping")) {
                continue;
            }

            ResourceAvailability rc = hostResourceUsage.get(dc.getHost());
            rc.setAmountOfContainer(rc.getAmountOfContainer() + 1);
            rc.setCpuCores(rc.getCpuCores() + dc.getCpuCores());
            rc.setMemory(rc.getMemory() + dc.getMemory());
            rc.setStorage(rc.getStorage() + dc.getStorage());
            hostResourceUsage.put(dc.getHost(), rc);
        }

        //calculate how much resources are left on a specific host

        for (Map.Entry<String, ResourceAvailability> entry : hostResourceUsage.entrySet()) {
            String name = entry.getKey();
            ResourceAvailability usage = entry.getValue();
            DockerHost dh = dhr.findFirstByName(name);

            if (blacklistedHost != null) {
                if (dh.getName().equals(blacklistedHost.getName())) {
                    LOG.info("Omitted host: " + dh.getName() + " for scheduling, since it is targeted to be shut down.");
                    continue;
                }
            }

            if (dh.getScheduledForShutdown()) {
                LOG.info("Omitted host: " + dh.getName() + " for scheduling, since it is scheduled to shut down.");
                continue;
            }

            ResourceAvailability availability = new ResourceAvailability();
            availability.setHost(dh);
            availability.setAmountOfContainer(usage.getAmountOfContainer());
            availability.setCpuCores(dh.getCores() - usage.getCpuCores());
            availability.setMemory(dh.getMemory() - usage.getMemory());
            availability.setStorage(dh.getStorage() - usage.getStorage());
            freeResources.add(availability);
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

        for (ResourceAvailability ra : calculateFreeResourcesforHosts(blacklistedHost)) {
            Double feasibilityThreshold = Math.min(ra.getCpuCores() / dc.getCpuCores(), ra.getMemory() / dc.getMemory());

            if (feasibilityThreshold < 1) {
                continue;
            }

            Integer remainingMemory = ra.getMemory() - dc.getMemory();
            Double remainingCpuCores = ra.getCpuCores() - dc.getCpuCores();

            Double difference = Math.abs((remainingMemory / ra.getHost().getMemory()) - (remainingCpuCores / ra.getHost().getCores()));

            Double suitablility = difference / feasibilityThreshold;

            if (!ra.getHost().getAvailableImages().contains(dc.getImage())) {
                suitablility = suitablility / 100;
            }

            if (suitablility < value) {
                if (value > 0) {
                    value = suitablility;
                    selectedHost = ra.getHost();
                }
            }
        }

        LOG.info("##### select suitable host for Container (" + dc.getOperatorType() + ") finished with host (" + selectedHost +"). ####");
        return selectedHost;
    }



    public String selectOperatorTobeScaledDown() {
        LOG.info("##### select operatorType to be scaled down initialized. ####");
        String selectedOperator = null;

        //get all Instances
        Integer maxInstances = Integer.MIN_VALUE;
        Integer minInstances = 0;
        Map<String, Integer> operatorAmount = new HashMap<>();

        // select all instances that have more than one instances
        for (String operator : tmgmt.getOperatorsAsList()) {
            Integer amount = dcr.findByOperatorName(operator).size();
            if (amount < 2) {
                continue;
            }
            operatorAmount.put(operator, amount);
            if (amount > maxInstances) {
                maxInstances = amount;
            }
            if ((amount < minInstances) && (amount > 0)) {
                minInstances = amount;
            }
        }

        if (operatorAmount.isEmpty()) {
            return null;
        }

        Long totalScalingActions = scr.count();
        Double selectionValue = 0.0;

        for (Map.Entry<String, Integer> entry : operatorAmount.entrySet()) {
            String op = entry.getKey();
            LOG.debug("### Suitability for Operator: " + op + " ###");


            // calculate instances impact factor
            Integer instancefactor = (entry.getValue() - minInstances) / (maxInstances - minInstances);
            LOG.debug("InstanceFactor: # = " + entry.getValue() + ", " + "min = " + minInstances + ", " + "max = " + maxInstances);

            //calculate qos impact factor
            List<ProcessingDuration> pds = pcr.findFirst5ByOperatorOrderByIdDesc(op);

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
            double expectedDuration = ((ProcessingOperator)topologyMgmt.getOperatorByIdentifier(entry.getKey())).getExpectedDuration();
            Double delayFactor = (avgDuration / expectedDuration * relaxationfactor) * (1 + penaltycosts);
            LOG.debug("DurationFactor: avgDuration = " + avgDuration + ", " + "expectedDuration = " + expectedDuration + ", " + "relaxation = " + relaxationfactor + ", " + "penaltycost = " + penaltycosts);


            //calculate scaling actions factor
            Long scalings = scr.countByOperator(entry.getKey());
            Long scalingFactor =  scalings / totalScalingActions;
            LOG.debug("ScalingFactor: scalingOperations = " + scalings + ", " + "totalScalings = " + totalScalingActions);


            Double overallFactor = instancefactor * 2 - delayFactor - scalingFactor * 0.5;
            LOG.debug("OverallFactor: overall = " + overallFactor + ", " + "instanceFactor = " + instancefactor + "(w=" + instancefactor * 2 + ")" + ", " + "delayFactor = " + delayFactor + "(w=" + delayFactor + ")" + ", " + "scalingFactor = " + scalingFactor + "(w=" + scalingFactor * 0.5 + ")");

            LOG.info("###############");

            if (overallFactor < 0) {
                continue;
            }

            if (overallFactor > selectionValue) {
                selectionValue = overallFactor;
                selectedOperator = op;
            }
        }


        LOG.info("##### select operatorType to be scaled down finished. ####");
        return selectedOperator;
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
    		Collections.sort(resources, new LeastLoadedHostFirstComparator());
    		
    		for (ResourceAvailability otherResource : resources){
    			
    			if (resource.equals(otherResource))
    				continue;
    			
    			/* Check resources */
    			if ((otherResource.getCpuCores() - container.getCpuCores()) > 0 && 
    				(otherResource.getMemory() - container.getMemory()) > 0 &&
    				(otherResource.getStorage() - container.getStorage()) > 0){
    				
        			/* Simulate relocation */
    				canRelocate = true;
    				resource.setAmountOfContainer(resource.getAmountOfContainer() + 1);
    				resource.setCpuCores(resource.getCpuCores() + container.getCpuCores());
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
    public DockerHost selectSuitableDockerHost(Operator op) throws Exception {
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

        throw new Exception("not enough resources available");
    }



}
