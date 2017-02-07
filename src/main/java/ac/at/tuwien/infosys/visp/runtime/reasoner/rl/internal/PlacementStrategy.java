package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import java.util.List;

import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourceAvailability;

public interface PlacementStrategy {

    public DockerHost computePlacement(DockerContainer container, List<ResourceAvailability> availableResources);
    
}
