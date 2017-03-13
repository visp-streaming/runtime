package ac.at.tuwien.infosys.visp.runtime.datasources.entities;


import lombok.Data;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


@Data
@Entity
public class DockerContainerMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String containerid;
    private String operator;
    private String operatorid;

    private double cpuUsage;
    private double memoryUsage;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime timestamp;

    public DockerContainerMonitor(String containerid, String operator, String operatorid) {
        this.containerid = containerid;
        this.operator = operator;
        this.operatorid = operatorid;
        this.timestamp = new DateTime(DateTimeZone.UTC);
    }

    public DockerContainerMonitor() {
    }
}
