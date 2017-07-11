package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class RuntimeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true, name = "visp_key")
    private String key;

    @Lob
    @Column(name = "visp_value")
    private String value;

    public RuntimeConfiguration(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public RuntimeConfiguration() {
    }
}
