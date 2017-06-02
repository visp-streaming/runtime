package ac.at.tuwien.infosys.visp.runtime.configuration;

import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
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
import java.nio.charset.StandardCharsets;
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

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    private static final Logger LOG = LoggerFactory.getLogger(Configurationprovider.class);

    @PostConstruct
    public void initialize() {
        this.runtimeIP = getData("runtimeIP");
        this.infrastructureIP = getData("infrastructureIP");
        this.openstackProcessingHostImage = getData("openstackProcessingHostImage");
        this.processingNodeImage = getData("processingNodeImage");
        this.reasoner = getData("reasoner");


        if (this.runtimeIP==null) {
                this.runtimeIP = getIp();
        }

        if (this.infrastructureIP==null) {
            this.infrastructureIP = "127.0.0.1";

            this.infrastructureIP = testcon(infrastructureIP);

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

    @SuppressWarnings("Duplicates")
    private String testcon(String infrastructureIP) {
        String databaseIp = null;

        try {
            LOG.info("Trying to connect to " + infrastructureIP);
            // try to connect to infrastructure host
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(infrastructureIP);
            factory.setUsername(rabbitmqUsername);
            factory.setPassword(rabbitmqPassword);
            Connection connection;
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.close();
            connection.close();
            return infrastructureIP;
        } catch(Exception e) {
            LOG.info("Connection to  " + infrastructureIP + " failed");
            try {
                // try to connect to database host
                try {
                    if (Files.exists(Paths.get("runtimeConfiguration/database.properties"))) {
                        databaseIp = new String(Files.readAllBytes(Paths.get("runtimeConfiguration/database.properties")), StandardCharsets.UTF_8);
                        databaseIp = databaseIp.replaceAll("[\\r\\n]", "").trim();
                    }
                } catch (IOException e2) {
                    LOG.error(e2.getLocalizedMessage(), e2);
                }
                LOG.info("Trying to connect to " + databaseIp);

                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(databaseIp);
                factory.setUsername(rabbitmqUsername);
                factory.setPassword(rabbitmqPassword);
                Connection connection;
                connection = factory.newConnection();
                Channel channel = connection.createChannel();
                channel.close();
                connection.close();
                return databaseIp;
            } catch(Exception e1) {
                throw new RuntimeException("Could neither connect to localhost nor to database IP");
            }
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
        lines.add(this.infrastructureIP);
        try {
            Files.write(Paths.get("runtimeConfiguration/database.properties"), lines);
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

    private String getIp() {
        //Try to identify IP for VISP runtime, if none is set
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));
                return in.readLine();
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

