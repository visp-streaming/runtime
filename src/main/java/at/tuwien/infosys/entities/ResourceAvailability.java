package at.tuwien.infosys.entities;

import at.tuwien.infosys.datasources.entities.DockerHost;
import lombok.Data;

@Data
public class ResourceAvailability implements Comparable<ResourceAvailability> {

    private DockerHost host;
    private Integer amountOfContainer;
    private Double cpuCores;
    private Integer memory;
    private Float storage;

    public ResourceAvailability() {
    }

    public ResourceAvailability(DockerHost host, Integer amountOfContainer, Double cpuCores, Integer memory, Float storage) {
        this.host = host;
        this.amountOfContainer = amountOfContainer;
        this.cpuCores = cpuCores;
        this.memory = memory;
        this.storage = storage;
    }


    @Override
    public int compareTo(ResourceAvailability o) {
        return ResourceComparator.AMOUNTOFCONTAINERASC.compare(this, o);
    }

    public ResourceAvailability clone() {
        ResourceAvailability ra = new ResourceAvailability();

        ra.host = this.host;
        ra.amountOfContainer = this.amountOfContainer;
        ra.cpuCores = this.cpuCores;
        ra.memory = this.memory;
        ra.storage = storage;

        return ra;
    }
}


