package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class VISPConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String start;
    private String end;
    private Double delay;
    private Double dataRate;
    private Double availability;

    //TODO make dynamic or retrieve from actual configuration
    public VISPConnection() {
        this.availability = 0.5;
    }

    public VISPConnection(String start, String end, Double delay, Double dataRate) {
        this.start = start;
        this.end = end;
        this.delay = delay;
        this.dataRate = dataRate;
        //TODO make dynamic
        this.availability = 0.5;
    }
}
