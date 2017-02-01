package at.tuwien.infosys.entities;


import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;



@Entity
public class DockerContainer {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
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

    public String getContainerid() {
        return containerid;
    }

    public void setContainerid(String containerid) {
        this.containerid = containerid;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DateTime getTerminationTime() {
        return terminationTime;
    }

    public void setTerminationTime(DateTime terminationTime) {
        this.terminationTime = terminationTime;
    }

    public Double getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Double cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getStorage() {
        return storage;
    }

    public void setStorage(Integer storage) {
        this.storage = storage;
    }

    public DockerContainer() {
        this.previousCpuUsage = 0;
        this.previousSystemUsage = 0;
        this.cpuUsage = 0.0;
        this.memoryUsage = 0;
        this.previousMemoryUsage = 0;
        this.monitoringPort = "";
    }

    public double getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(double cpuUsagePercentage) {
		this.cpuUsage = cpuUsagePercentage;
	}

	public long getPreviousCpuUsage() {
		return previousCpuUsage;
	}

	public void setPreviousCpuUsage(long previousCpuUsage) {
		this.previousCpuUsage = previousCpuUsage;
	}

	public long getPreviousSystemUsage() {
		return previousSystemUsage;
	}

	public void setPreviousSystemUsage(long previousSystemUsage) {
		this.previousSystemUsage = previousSystemUsage;
	}

	public String getMonitoringPort() {
		return monitoringPort;
	}

	public void setMonitoringPort(String monitoringPort) {
		this.monitoringPort = monitoringPort;
	}

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public long getPreviousMemoryUsage() {
        return previousMemoryUsage;
    }

    public void setPreviousMemoryUsage(long previousMemoryUsage) {
        this.previousMemoryUsage = previousMemoryUsage;
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


    @Override
    public String toString() {
        return "DockerContainer{" +
                "id=" + id +
                ", containerid='" + containerid + '\'' +
                ", host='" + host + '\'' +
                ", image='" + image + '\'' +
                ", operator='" + operator + '\'' +
                ", status='" + status + '\'' +
                ", terminationTime='" + terminationTime + '\'' +
                ", cpuCores=" + cpuCores +
                ", memory=" + memory +
                ", storage=" + storage +
                '}';
    }
}
