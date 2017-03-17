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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
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

        if (this.runtimeIP==null) {
                this.runtimeIP = getIp();
                this.infrastructureIP = runtimeIP;
        }

        if (this.openstackProcessingHostImage ==null) {
            this.openstackProcessingHostImage = defaultOpenstackProcessingHostImage;
        }

        if (this.processingNodeImage ==null) {
            this.processingNodeImage=defaultProcessingNodeImage;
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
            rfr.save(new RuntimeConfiguration(identifier, value));
        } else {
            rc.setValue(value);
            rfr.save(rc);
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

    public String getIp()  {
        //Try to identify IP for VISP runtime, if none is set
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));
                String ip = in.readLine();
                return ip;
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOG.error(e.getLocalizedMessage());
                    }
                }
            }
        } catch (MalformedURLException e) {
            LOG.error(e.getLocalizedMessage());
        }
        return "127.0.0.1";
    }

}

