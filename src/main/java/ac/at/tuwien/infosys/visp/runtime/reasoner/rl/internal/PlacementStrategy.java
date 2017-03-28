package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;

import java.util.List;

public interface PlacementStrategy {

    DockerHost computePlacement(DockerContainer container, List<ResourceAvailability> availableResources);
    
}
