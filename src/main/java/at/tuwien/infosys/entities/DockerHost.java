package at.tuwien.infosys.entities;


import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class DockerHost {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String name;
    private String url;
    private Double cores;
    private Integer ram;
    private Float storage;
    private Boolean scheduledForShutdown;
    private String terminationTime;
    private String flavour;
    private String BTUend;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> availableImages;


    public DockerHost() {
        this.availableImages = new ArrayList<>();
    }

    public DockerHost(String name) {
        this.name = name;
        this.availableImages = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public Boolean getScheduledForShutdown() {
        return scheduledForShutdown;
    }

    public void setScheduledForShutdown(Boolean scheduledForShutdown) {
        this.scheduledForShutdown = scheduledForShutdown;
    }

    public String getTerminationTime() {
        return terminationTime;
    }

    public void setTerminationTime(String terminationTime) {
        this.terminationTime = terminationTime;
    }

    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    public List<String> getAvailableImages() {
        return availableImages;
    }

    public void setAvailableImages(List<String> availableImages) {
        this.availableImages = availableImages;
    }

    public String getBTUend() {
        return BTUend;
    }

    public void setBTUend(String BTUend) {
        this.BTUend = BTUend;
    }

    @Override
    public String toString() {
        return "DockerHost{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", cores=" + cores +
                ", ram=" + ram +
                ", storage=" + storage +
                ", scheduledForShutdown=" + scheduledForShutdown +
                ", terminationTime='" + terminationTime + '\'' +
                ", flavour='" + flavour + '\'' +
                ", BTUend='" + BTUend + '\'' +
                ", availableImages=" + availableImages +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DockerHost that = (DockerHost) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
