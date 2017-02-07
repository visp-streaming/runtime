package at.tuwien.infosys.datasources.entities;

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

    private String uri;

    public VISPInstance() {
    }

    public VISPInstance(String uri) {
        this.uri = uri;
    }
}

