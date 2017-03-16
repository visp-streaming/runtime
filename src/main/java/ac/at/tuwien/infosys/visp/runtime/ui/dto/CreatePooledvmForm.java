package ac.at.tuwien.infosys.visp.runtime.ui.dto;


public class CreatePooledvmForm {

    public String poolname;
    public String flavour;
    public Double cost;

    public String getPoolname() {
        return poolname;
    }

    public void setPoolname(String poolname) {
        this.poolname = poolname;
    }

    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }
}
