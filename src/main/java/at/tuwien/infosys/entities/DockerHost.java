package at.tuwien.infosys.entities;


import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class DockerHost {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String name;
    private String resourceProvider;
    private String url;
    private Double cores;
    private Integer memory;
    private Float storage;
    private Boolean scheduledForShutdown;
    private String flavour;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime BTUend;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime terminationTime;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> availableImages;

    private String usedPorts;

    public DockerHost() {
        this.availableImages = new ArrayList<>();
        this.usedPorts = "";
    }

    public DockerHost(String name) {
        this.name = name;
        this.availableImages = new ArrayList<>();
        this.usedPorts = "";
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

    public Boolean getScheduledForShutdown() {
        return scheduledForShutdown;
    }

    public void setScheduledForShutdown(Boolean scheduledForShutdown) {
        this.scheduledForShutdown = scheduledForShutdown;
    }

    public DateTime getTerminationTime() {
        return terminationTime;
    }

    public void setTerminationTime(DateTime terminationTime) {
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

    public List<String> getUsedPorts() {
        List<String> lUsedPorts = new ArrayList<String>();
        for (String port : Splitter.on(',').split(this.usedPorts)) {
            lUsedPorts.add(port);
        }
		return lUsedPorts;
	}

	public void setUsedPorts(List<String> usedPorts) {
	    this.usedPorts = Joiner.on(',').join(usedPorts);
	}

    public DateTime getBTUend() {
        return BTUend;
    }

    public void setBTUend(DateTime BTUend) {
        this.BTUend = BTUend;
    }

    public String getResourceProvider() {
        return resourceProvider;
    }

    public void setResourceProvider(String resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    @Override
    public String toString() {
        return "DockerHost{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceProvider='" + resourceProvider + '\'' +
                ", url='" + url + '\'' +
                ", cores=" + cores +
                ", memory=" + memory +
                ", storage=" + storage +
                ", scheduledForShutdown=" + scheduledForShutdown +
                ", flavour='" + flavour + '\'' +
                ", BTUend=" + BTUend +
                ", terminationTime=" + terminationTime +
                ", availableImages=" + availableImages +
                ", usedPorts='" + usedPorts + '\'' +
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
