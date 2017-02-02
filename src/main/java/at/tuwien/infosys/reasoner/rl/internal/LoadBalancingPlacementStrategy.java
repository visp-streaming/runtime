package at.tuwien.infosys.reasoner.rl.internal;

import at.tuwien.infosys.datasources.entities.DockerContainer;
import at.tuwien.infosys.datasources.entities.DockerHost;
import at.tuwien.infosys.entities.ResourceAvailability;

import java.util.List;

public class LoadBalancingPlacementStrategy implements PlacementStrategy {

	@Override
	public DockerHost computePlacement(DockerContainer container,
			List<ResourceAvailability> availableResources) {
		
        SortedList<ResourceAvailability> candidates = new SortedList<>(new LeastLoadedHostFirstComparator());
        
        for (ResourceAvailability ra : availableResources) {
            if (ra.getCpuCores() <= container.getCpuCores()) {
                continue;
            }
            if (ra.getMemory() <= container.getMemory()) {
                continue;
            }
            if (ra.getStorage() <= container.getStorage()) {
                continue;
            }

            candidates.add(ra);
        }
        
        if (candidates.isEmpty())
        	return null;
        
        return candidates.get(0).getHost();

	}
	
}
