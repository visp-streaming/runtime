package at.tuwien.infosys.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class PooledVM {


    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String poolname;
    private String name;
    private String url;
    private Double cores;
    private Integer memory;
    private Float storage;
    private String flavour;
    private String linkedhost;

    public PooledVM() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getCores() {
        return cores;
    }

    public void setCores(Double cores) {
        this.cores = cores;
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

    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    public String getLinkedhost() {
        return linkedhost;
    }

    public void setLinkedhost(String linkedhost) {
        this.linkedhost = linkedhost;
    }

    public String getPoolname() {
        return poolname;
    }

    public void setPoolname(String poolname) {
        this.poolname = poolname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PooledVM that = (PooledVM) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
