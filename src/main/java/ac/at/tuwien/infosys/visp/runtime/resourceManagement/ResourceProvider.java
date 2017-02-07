package ac.at.tuwien.infosys.visp.runtime.resourceManagement;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResourceProvider {

    @Autowired
    protected OpenstackConnector openstackConnector;

    @Autowired
    protected ResourcePoolConnector resourcePoolConnector;

    private Map<String, String> resourceProviders = new HashMap<>();

    @Autowired
    public ResourceProvider(@Value("${visp.computational.resources.openstack}") Boolean openstackUsage, @Value("${visp.computational.resources.pools}") String pools) {

        if (openstackUsage) {
            resourceProviders.put("openstack", "openstack");
        }

        for (String pool : pools.split(",")) {
            resourceProviders.put(pool, "pool");
        }
    }

    public ResourceConnector get(String identifier) {

        String type = resourceProviders.get(identifier);

        if (type.equals("openstack")) {
            return openstackConnector;
        }

        if (type.equals("pool")) {
            resourcePoolConnector.setRessourcePoolName(identifier);
            return resourcePoolConnector;
        }

        return null;
    }

    public DockerHost createContainerSkeleton() {
        DockerHost dh = new DockerHost("additionaldockerhost");
        dh.setFlavour("m2.medium");
        return dh;
    }

    public Map<String, String> getResourceProviders() {
        return resourceProviders;
    }

}
