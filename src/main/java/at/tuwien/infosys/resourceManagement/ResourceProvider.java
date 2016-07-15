package at.tuwien.infosys.resourceManagement;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResourceProvider {


    @Value("${visp.computational.resources}")
    private String RESOURCES;

    @Autowired
    OpenstackConnector openstackConnector;

    @Autowired
    ResourcePoolConnector resourcePoolConnector;


    public ResourceConnector get() {

        if (RESOURCES.equals("openstack")) {
            return openstackConnector;
        }

        if (RESOURCES.equals("pool")) {
            return resourcePoolConnector;
        }

        return null;
    }

}
