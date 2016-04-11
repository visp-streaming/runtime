package at.tuwien.infosys.entities;

public class ResourceAvailability implements Comparable<ResourceAvailability>{

    private String hostId;
    private Integer amountOfContainer;
    private Double cpuCores;
    private Integer ram;
    private Float storage;
    private String url;

    public ResourceAvailability() {
    }

    public ResourceAvailability(String hostId, Integer amountOfContainer, Double cpuCores, Integer ram, Float storage, String url) {
        this.hostId = hostId;
        this.amountOfContainer = amountOfContainer;
        this.cpuCores = cpuCores;
        this.ram = ram;
        this.storage = storage;
        this.url = url;
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

    public Float getStorage() {
        return storage;
    }

    public void setStorage(Float storage) {
        this.storage = storage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ResourceAvailability{" +
                "hostId='" + hostId + '\'' +
                ", amountOfContainer=" + amountOfContainer +
                ", cpuCores=" + cpuCores +
                ", ram=" + ram +
                ", storage=" + storage +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public int compareTo(ResourceAvailability o) {
        return ResourceComparator.AMOUNTOFCONTAINERASC.compare(this, o);
    }
}


