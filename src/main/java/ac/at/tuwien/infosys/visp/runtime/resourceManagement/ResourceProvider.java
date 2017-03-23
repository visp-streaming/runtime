package ac.at.tuwien.infosys.visp.runtime.resourceManagement;


import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.ResourceConnector;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.ResourcePoolConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResourceProvider {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private OpenstackConnector openstackConnector;

    @Autowired
    private ResourcePoolConnector resourcePoolConnector;

    @Value("${visp.computational.resources.openstack}")
    private Boolean openstackUsage;

    private Map<String, String> resourceProviders = new HashMap<>();

    @PostConstruct
    public void init() {

        if (openstackUsage) {
            resourceProviders.put("openstack", "openstack");
        }

        for (String pool : pvmr.findDistinctPoolnames()) {
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

    public void updateResourceProvider() {
            init();
    }


}
