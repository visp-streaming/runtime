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
    private Integer cores;
    private Integer ram;

    public DockerHost() {
    }

    public DockerHost(String hostid, String url, Integer cores, Integer ram) {
        this.hostid = hostid;
        this.url = url;
        this.cores = cores;
        this.ram = ram;
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

    public Integer getCores() {
        return cores;
    }

    public void setCores(Integer cores) {
        this.cores = cores;
    }

    public Integer getRam() {
        return ram;
    }

    public void setRam(Integer ram) {
        this.ram = ram;
    }

    @Override
    public String toString() {
        return "DockerHost{" +
                "id=" + id +
                ", hostid='" + hostid + '\'' +
                ", url='" + url + '\'' +
                ", cores=" + cores +
                ", ram=" + ram +
                '}';
    }
}
