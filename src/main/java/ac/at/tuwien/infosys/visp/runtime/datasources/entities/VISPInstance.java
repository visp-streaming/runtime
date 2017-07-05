package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class VISPInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String ip;

    public VISPInstance() {
    }

    public VISPInstance(String ip) {
        this.ip = ip;
    }
}

