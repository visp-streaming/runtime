package at.tuwien.infosys.entities;

public class ResourceAvailability implements Comparable<ResourceAvailability>{

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

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
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
                ", memory=" + memory +
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
        ra.memory = new Integer(this.memory);
        ra.storage = new Float(storage);
        
        return ra;
    }
}


