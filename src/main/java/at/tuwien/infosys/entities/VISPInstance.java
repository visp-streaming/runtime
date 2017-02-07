package at.tuwien.infosys.entities;

import lombok.Data;

import java.util.List;

@Data
public class VISPInstance {
    private String URI;
    private List<ResourcePool> resourcePools;


}
