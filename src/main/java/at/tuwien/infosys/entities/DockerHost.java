package at.tuwien.infosys.entities;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class DockerHost {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String hostid;
    private String url;
    private Double cores;
    private Integer ram;
    private Integer storage;
    private Boolean scheduledForShutdown;
    private String terminationTime;


    public DockerHost() {
    }

    public DockerHost(String hostid, String url, Double cores, Integer ram, Integer storage) {
        this.hostid = hostid;
        this.url = url;
        this.cores = cores;
        this.ram = ram;
        this.storage = storage;
        this.scheduledForShutdown = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostid() {
        return hostid;
    }

    public void setHostid(String hostid) {
        this.hostid = hostid;
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

    public Integer getStorage() {
        return storage;
    }

    public void setStorage(Integer storage) {
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

    @Override
    public String toString() {
        return "DockerHost{" +
                "id=" + id +
                ", hostid='" + hostid + '\'' +
                ", url='" + url + '\'' +
                ", cores=" + cores +
                ", ram=" + ram +
                ", storage=" + storage +
                ", scheduledForShutdown=" + scheduledForShutdown +
                ", terminationTime='" + terminationTime + '\'' +
                '}';
    }
}
