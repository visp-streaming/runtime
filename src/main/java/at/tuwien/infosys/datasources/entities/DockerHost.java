package at.tuwien.infosys.datasources.entities;


import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
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

}
