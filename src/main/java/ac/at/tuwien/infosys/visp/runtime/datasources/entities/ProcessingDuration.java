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
public class ProcessingDuration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime time;

    private String operatortype;
    private String operatorname;
    private Double duration;
    private String containerid;

    public ProcessingDuration() {
    }

    public ProcessingDuration(DateTime time, String operatortype, String operatorname, String containerid, Double duration) {
        this.time = time;
        this.operatortype = operatortype;
        this.operatorname = operatorname;
        this.duration = duration;
        this.containerid = containerid;
    }

}
