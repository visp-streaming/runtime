package ac.at.tuwien.infosys.visp.runtime.ui.dto;

import lombok.Data;

@Data
public class PooledVMDTO {
    private long id;

    private String poolname;
    private String name;
    private String url;
    private String flavour;
    private Boolean available;

    public PooledVMDTO(long id, String poolname, String name, String url, String flavour, Boolean available) {
        this.id = id;
        this.poolname = poolname;
        this.name = name;
        this.url = url;
        this.flavour = flavour;
        this.available = available;
    }
}
