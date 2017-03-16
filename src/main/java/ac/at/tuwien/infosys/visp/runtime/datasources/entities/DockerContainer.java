package ac.at.tuwien.infosys.visp.runtime.datasources.entities;


import lombok.Data;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


@Data
@Entity
public class DockerContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String containerid;
    private String host;
    private String image;
    private String operatorType;
    private String operatorName;
    private String status;
    private Double cpuCores;
    private Integer memory;
    private Integer storage;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime terminationTime;

    private String monitoringPort;

    public DockerContainer(String operatorType, String operatorName, Double cpuCores, Integer memory, Integer storage) {
        this.operatorType = operatorType;
        this.operatorName = operatorName;
        this.cpuCores = cpuCores;
        this.memory = memory;
        this.storage = storage;
        this.status = "running";
        this.monitoringPort = "";
    }

    public DockerContainer() {
    }
}
