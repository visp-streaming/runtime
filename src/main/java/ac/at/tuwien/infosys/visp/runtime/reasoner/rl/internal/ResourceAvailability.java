package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ResourceAvailability {

    private DockerHost host;

    private Integer amountOfContainer;
    private Double cores;
    private Integer memory;
    private Float storage;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAvailability.class);


    public ResourceAvailability(DockerHost dh, ResourceTriple rt) {
        this.host = dh;
        this.amountOfContainer = 3;
        this.cores = rt.getCores();
        this.memory = rt.getMemory();
        this.storage = rt.getStorage();
    }

    public ResourceAvailability(DockerHost dh) {
        this.host = dh;
        this.amountOfContainer = 0;
        this.cores = 0.0;
        this.memory = 0;
        this.storage = 0F;
    }

    public ResourceAvailability() {
    }

    public ResourceAvailability(DockerHost host, Integer amountOfContainer, Double cores, Integer memory, Float storage) {
        this.host = host;
        this.amountOfContainer = amountOfContainer;
        this.cores = cores;
        this.memory = memory;
        this.storage = storage;
    }


    public void increment(Double cores, Integer memory, Float storage) {
        incrementCores(cores);
        incrementMemory(memory);
        incrementStorage(storage);
    }

    public void decrement(Double cores, Integer memory, Float storage) {
        decrementCores(cores);
        decrementMemory(memory);
        decrementStorage(storage);
    }

    public void incrementAmountOfContainer() {
        this.amountOfContainer++;
    }

    public void incrementCores(Double cores) {
        this.cores += cores;
    }

    public void incrementMemory(Integer memory) {
        this.memory += memory;
    }

    public void incrementStorage(Float storage) {
        this.storage += storage;
    }

    public void decrementAmountOfContainer() {
        this.amountOfContainer--;
    }

    public void decrementCores(Double cores) {
        this.cores -= cores;
        if (this.cores < 0){
            LOG.error("Too little cpu cores.");
        }
    }

    public void decrementMemory(Integer memory) {
        this.memory -= memory;
        if (this.memory < 0){
            LOG.error("Too little memory.");
        }
    }

    public void decrementStorage(Float storage) {
        this.storage -= storage;
        if (this.storage < 0){
            LOG.error("Too little storage.");
        }
    }

    public ResourceAvailability clone() {
        ResourceAvailability ra = new ResourceAvailability();

        ra.host = this.host;
        ra.amountOfContainer = this.amountOfContainer;
        ra.cores = this.cores;
        ra.memory = this.memory;
        ra.storage = storage;

        return ra;
    }
}


