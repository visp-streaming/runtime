package at.tuwien.infosys.entities;

public class ResourcePool {

    private String name;
    private Integer CPUfrequency;
    private Double costPerCore;
    private Boolean availability;

    public ResourcePool(String name) {
        this.name = name;
        this.CPUfrequency = 2400;
        this.costPerCore = 10.0;
        this.availability = true;
    }

    public ResourcePool() {
    }
}
