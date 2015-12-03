package at.tuwien.infosys.entities;

public class ResourceAvailability implements Comparable<ResourceAvailability>{

    private String hostId;
    private Integer amountOfContainer;
    private Double cpuCores;
    private Integer ram;
    private Integer storage;

    public ResourceAvailability() {
    }

    public ResourceAvailability(String hostId, Integer amountOfContainer, Double cpuCores, Integer ram, Integer storage) {
        this.hostId = hostId;
        this.amountOfContainer = amountOfContainer;
        this.cpuCores = cpuCores;
        this.ram = ram;
        this.storage = storage;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public Integer getAmountOfContainer() {
        return amountOfContainer;
    }

    public void setAmountOfContainer(Integer amountOfContainer) {
        this.amountOfContainer = amountOfContainer;
    }

    public Double getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Double cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getRam() {
        return ram;
    }

    public void setRam(Integer ram) {
        this.ram = ram;
    }

    public Integer getStorage() {
        return storage;
    }

    public void setStorage(Integer storage) {
        this.storage = storage;
    }

    @Override
    public String toString() {
        return "ResourceUsage{" +
                "hostId='" + hostId + '\'' +
                ", amountOfContainer=" + amountOfContainer +
                ", cpuCores=" + cpuCores +
                ", ram=" + ram +
                ", storage=" + storage +
                '}';
    }

    @Override
    public int compareTo(ResourceAvailability o) {
        return ResourceComparator.AMOUNTOFCONTAINERASC.compare(this, o);
    }
}


