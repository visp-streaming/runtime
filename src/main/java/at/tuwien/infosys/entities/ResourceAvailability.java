package at.tuwien.infosys.entities;

public class ResourceAvailability implements Comparable<ResourceAvailability>{

    private DockerHost host;
    private Integer amountOfContainer;
    private Double cpuCores;
    private Integer ram;
    private Float storage;

    public ResourceAvailability() {
    }

    public ResourceAvailability(DockerHost host, Integer amountOfContainer, Double cpuCores, Integer ram, Float storage) {
        this.host = host;
        this.amountOfContainer = amountOfContainer;
        this.cpuCores = cpuCores;
        this.ram = ram;
        this.storage = storage;
    }

    public DockerHost getHost() {
        return host;
    }

    public void setHost(DockerHost host) {
        this.host = host;
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

    @Override
    public String toString() {
        return "ResourceAvailability{" +
                "host=" + host.getName() +
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
    
    public ResourceAvailability clone() {
    	ResourceAvailability ra = new ResourceAvailability();
    	
    	ra.host = this.host;
        ra.amountOfContainer = new Integer(this.amountOfContainer);
        ra.cpuCores = new Double(this.cpuCores);
        ra.ram = new Integer(this.ram);
        ra.storage = new Float(storage);
        
        return ra;
    }
}


