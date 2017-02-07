package ac.at.tuwien.infosys.visp.runtime.entities;

import lombok.Data;

@Data
public class ResourcePoolUsage {

    public ResourcePoolUsage(String name) {
        this.name = name;
    }

    private String name;
    private Double cost;
    private Integer cpuFrequency;
    private Boolean availability;

    private ResourceTriple overallResources;
    private ResourceTriple plannedResources;
    private ResourceTriple actualResources;

    public ResourcePoolUsage() {
        this.availability = true;
    }
}
