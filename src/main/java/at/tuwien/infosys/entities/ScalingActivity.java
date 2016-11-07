package at.tuwien.infosys.entities;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ScalingActivity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private String type;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime time;

    private String operator;
    private String scalingActivity;
    private String host;

    public ScalingActivity() {
    }

    public ScalingActivity(String type, DateTime time, String operator, String scalingActivity, String host) {
        this.type = type;
        this.time = time;
        this.operator = operator;
        this.scalingActivity = scalingActivity;
        this.host = host;
    }

    @Override
    public String toString() {
        return "ScalingActivity{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", time='" + time.toString() + '\'' +
                ", operator='" + operator + '\'' +
                ", scalingActivity='" + scalingActivity + '\'' +
                ", host='" + host + '\'' +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getScalingActivity() {
        return scalingActivity;
    }

    public void setScalingActivity(String scalingActivity) {
        this.scalingActivity = scalingActivity;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
