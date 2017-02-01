package at.tuwien.infosys.datasources.entities;


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
    private String operator;
    private String status;
    private Double cpuCores;
    private Integer memory;
    private Integer storage;

    private String monitoringPort;

    /* Monitoring Information */
    private double cpuUsage;
    private long previousCpuUsage;
    private long previousSystemUsage;

    private long memoryUsage;
    private long previousMemoryUsage;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime terminationTime;

    public DockerContainer() {
        this.previousCpuUsage = 0;
        this.previousSystemUsage = 0;
        this.cpuUsage = 0.0;
        this.memoryUsage = 0;
        this.previousMemoryUsage = 0;
        this.monitoringPort = "";
    }

    public DockerContainer(String operator, Double cpuCores, Integer memory, Integer storage) {
        this.operator = operator;
        this.cpuCores = cpuCores;
        this.memory = memory;
        this.storage = storage;
        this.status = "running";
        this.previousCpuUsage = 0;
        this.previousSystemUsage = 0;
        this.cpuUsage = 0.0;
        this.memoryUsage = 0;
        this.previousMemoryUsage = 0;
        this.monitoringPort = "";
    }
}
