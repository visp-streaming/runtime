package at.tuwien.infosys.entities;

import lombok.Data;

@Data
public class OperatorConfiguration {

    public OperatorConfiguration(String name) {
        this.name = name;
    }

    private String name;

    private ResourceTriple plannedResources;
    private ResourceTriple actualResources;

    //TODO add more information from the topology

}
