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

    private String operatortype;
    private String operatorname;
    private String containerid;

    private double cpuUsage;
    private double memoryUsage;
    private double networkUpload;
    private double networkDownload;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime timestamp;

    public DockerContainerMonitor(String operatorType, String operatorName, String containerid) {
        this.operatortype = operatorType;
        this.operatorname = operatorName;
        this.containerid = containerid;
        this.timestamp = new DateTime(DateTimeZone.UTC);
    }

    public DockerContainerMonitor() {
    }
}
