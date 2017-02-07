package at.tuwien.infosys.entities;

import lombok.Data;

@Data
public class ResourceTriple {
    private Double cores = 0.0;
    private Integer memory = 0;
    private Float storage = 0.0F;

    public ResourceTriple() {
        this.cores = 0.0;
        this.memory = 0;
        this.storage = 0.0F;
    }

    public ResourceTriple(Double cores, Integer memory, Float storage) {
        this.cores = cores;
        this.memory = memory;
        this.storage = storage;
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


    public void divideForMultipleRecordings(Integer counter) {
        this.cores = cores / counter;
        this.memory = memory / counter;
        this.storage = storage / counter;
    }

}