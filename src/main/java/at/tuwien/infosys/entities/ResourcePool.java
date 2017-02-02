package at.tuwien.infosys.entities;

import lombok.Data;

@Data
public class ResourcePool {

    public ResourcePool(String name) {
        this.name = name;
    }

    private String name;

    private ResourceTriple overallResources;
    private ResourceTriple plannedResources;
    private ResourceTriple actualResources;

}
