package ac.at.tuwien.infosys.visp.runtime.configuration;

import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class Configurationprovider {

    private String runtimeIP = null;
    private String infrastructureIP = null;

    private String openstackProcessingHostImage = null;
    private String processingNodeImage = null;
    private String reasoner = null;

    @Autowired
    private RuntimeConfigurationRepository rfr;

    @Value("${visp.default.openstack.processingHostImage}")
    private String defaultOpenstackProcessingHostImage;

    @Value("${visp.default.processingNodeImage}")
    private String defaultProcessingNodeImage;

    private static final Logger LOG = LoggerFactory.getLogger(Configurationprovider.class);

    @PostConstruct
    public void initialize() {
        this.runtimeIP = getData("runtimeIP");
        this.infrastructureIP = getData("infrastructureIP");
        this.openstackProcessingHostImage = getData("openstackProcessingHostImage");
        this.processingNodeImage = getData("processingNodeImage");
        this.reasoner = getData("reasoner");


        if (this.runtimeIP==null) {
                this.runtimeIP = "127.0.0.1";
                this.infrastructureIP = runtimeIP;
        }

        if (this.openstackProcessingHostImage ==null) {
            this.openstackProcessingHostImage = defaultOpenstackProcessingHostImage;
        }

        if (this.processingNodeImage ==null) {
            this.processingNodeImage=defaultProcessingNodeImage;
        }

        if (this.reasoner == null) {
            this.reasoner = "none";
        }
    }

    private String getData(String identifier) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        if (rc != null) {
            return rc.getValue();
        }
        return null;
    }

    @PreDestroy
    public void storeDataToDB() {
        storeSingle("runtimeIP", this.runtimeIP);
        storeSingle("infrastructureIP", this.infrastructureIP);
        storeSingle("openstackProcessingHostImage", this.openstackProcessingHostImage);
        storeSingle("processingNodeImage", this.processingNodeImage);
        storeSingle("reasoner", this.reasoner);


        List<String> lines = new ArrayList<>();
        lines.add(this.runtimeIP);
        try {
            Files.write(Paths.get("database.properties"), lines);
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    private void storeSingle(String identifier, String value) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        if (rc == null) {
            rfr.saveAndFlush(new RuntimeConfiguration(identifier, value));
        } else {
            rc.setValue(value);
            rfr.saveAndFlush(rc);
        }
    }


    public String getRedisHost() {
        return this.getInfrastructureIP();
    }

    public String getRabbitMQHost() {
        return this.infrastructureIP;
    }

    public String getRuntimeIP() {
        return runtimeIP;
    }

    public void setRuntimeIP(String runtimeIP) {
        this.runtimeIP = runtimeIP;
    }

    public String getInfrastructureIP() {
        return infrastructureIP;
    }

    public void setInfrastructureIP(String infrastructureIP) {
        this.infrastructureIP = infrastructureIP;
    }

    public String getOpenstackProcessingHostImage() {
        return openstackProcessingHostImage;
    }

    public void setOpenstackProcessingHostImage(String openstackProcessingHostImage) {
        this.openstackProcessingHostImage = openstackProcessingHostImage;
    }

    public String getProcessingNodeImage() {
        return processingNodeImage;
    }

    public void setProcessingNodeImage(String processingNodeImage) {
        this.processingNodeImage = processingNodeImage;
    }

    public String getReasoner() {
        return reasoner;
    }

    public void setReasoner(String reasoner) {
        this.reasoner = reasoner;
    }


}

