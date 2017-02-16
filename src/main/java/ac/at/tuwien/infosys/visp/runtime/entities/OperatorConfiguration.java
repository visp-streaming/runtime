package ac.at.tuwien.infosys.visp.runtime.entities;

import lombok.Data;

@Data
public class OperatorConfiguration {

    public OperatorConfiguration(String name) {
        this.name = name;
        //TODO make this dynamic
        this.frequency = 2400;
    }

    private String name;
    private Integer frequency;

    private ResourceTriple plannedResources;
    private ResourceTriple actualResources;

    //TODO add more information from the topology

}
