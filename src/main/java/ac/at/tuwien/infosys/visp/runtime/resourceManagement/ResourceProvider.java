package ac.at.tuwien.infosys.visp.runtime.resourceManagement;


import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.ResourceConnector;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.ResourcePoolConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn("configurationprovider")
public class ResourceProvider {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private OpenstackConnector openstackConnector;

    @Autowired
    private ResourcePoolConnector resourcePoolConnector;

    private Map<String, String> resourceProviders = new HashMap<>();

    @Autowired
    private Configurationprovider config;

    @PostConstruct
    public void init() {
        resourceProviders = new HashMap<>();

        if (config.getOpenstackondemand()) {
            resourceProviders.put("openstack", "openstack");
        }

        for (String pool : pvmr.findDistinctPoolnames()) {
            resourceProviders.put(pool, "pool");
        }
    }

    public ResourceConnector get(String identifier) {

        String type = resourceProviders.get(identifier);

        if ("openstack".equals(type)) {
            return openstackConnector;
        }

        if ("pool".equals(type)) {
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
