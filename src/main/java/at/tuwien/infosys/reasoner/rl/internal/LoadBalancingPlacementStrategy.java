package at.tuwien.infosys.reasoner.rl.internal;

import java.util.List;

import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ResourceAvailability;

public class LoadBalancingPlacementStrategy implements PlacementStrategy {

	@Override
	public DockerHost computePlacement(DockerContainer container,
			List<ResourceAvailability> availableResources) {
		
        SortedList<ResourceAvailability> candidates = new SortedList<ResourceAvailability>(new LeastLoadedHostFirstComparator());
        
        for (ResourceAvailability ra : availableResources) {
            if (ra.getCpuCores() <= container.getCpuCores()) {
                continue;
            }
            if (ra.getRam() <= container.getRam()) {
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
