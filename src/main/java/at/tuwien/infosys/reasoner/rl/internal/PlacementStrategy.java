package at.tuwien.infosys.reasoner.rl.internal;

import java.util.List;

import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ResourceAvailability;

public interface PlacementStrategy {

    public DockerHost computePlacement(DockerContainer container, List<ResourceAvailability> availableResources);
    
}
