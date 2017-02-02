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
public class ScalingActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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

}
