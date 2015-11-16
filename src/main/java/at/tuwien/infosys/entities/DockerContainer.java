package at.tuwien.infosys.entities;


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
    private String terminationTime;

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

    public String getTerminationTime() {
        return terminationTime;
    }

    public void setTerminationTime(String terminationTime) {
        this.terminationTime = terminationTime;
    }

    public DockerContainer() {
    }

    public DockerContainer(String containerid, String host, String image, String operator) {
        this.containerid = containerid;
        this.host = host;
        this.image = image;
        this.operator = operator;
        this.status = "running";
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
                '}';
    }
}
