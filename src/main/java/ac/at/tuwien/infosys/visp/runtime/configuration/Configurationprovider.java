package ac.at.tuwien.infosys.visp.runtime.configuration;

import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

@Data
@Service
public class Configurationprovider {

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

    private String runtimeIP = null;
    private String infrastructureIP = null;
    private String openstackProcessingHostImage = null;
    private String processingNodeImage = null;
    private String reasoner = null;

    private static final Logger LOG = LoggerFactory.getLogger(Configurationprovider.class);

    @PostConstruct
    public void initialize() {
        this.runtimeIP = getData("runtimeIP");
        this.infrastructureIP = getData("infrastructureIP");
        this.openstackProcessingHostImage = getData("openstackProcessingHostImage");
        this.processingNodeImage = getData("processingNodeImage");
        this.reasoner = getData("reasoner");

        if (this.runtimeIP == null) {
            this.runtimeIP = getIp();
        }

        if (this.infrastructureIP == null) {
            this.infrastructureIP = testConnection("127.0.0.1");
        }

        if (this.openstackProcessingHostImage == null) {
            this.openstackProcessingHostImage = defaultOpenstackProcessingHostImage;
        }

        if (this.processingNodeImage == null) {
            this.processingNodeImage=defaultProcessingNodeImage;
        }

        if (this.reasoner == null) {
            this.reasoner = "none";
        }
    }

    private String testConnection(String infrastructureIP) {
        String databaseIP = null;
        try {
            return validateIPForRabbitMQHost(infrastructureIP);
        } catch(Exception e) {
            LOG.info("Connection to  " + infrastructureIP + " failed");
            try {
                // try to connect to database host
                    Path databaseConfigFile = Paths.get("runtimeConfiguration/database.properties");
                    if (Files.exists(databaseConfigFile)) {
                        databaseIP = new String(Files.readAllBytes(databaseConfigFile), StandardCharsets.UTF_8).replaceAll("[\\r\\n]", "").trim();
                    }
                return validateIPForRabbitMQHost(databaseIP);
            } catch(Exception e1) {
                throw new RuntimeException("Could neither connect to localhost nor to database IP");
            }
        }
    }

    private String validateIPForRabbitMQHost(String infrastructureIP) throws IOException, TimeoutException {
        LOG.info("Trying to connect to " + infrastructureIP);
        // try to connect to infrastructure host
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(infrastructureIP);
        factory.setUsername(rabbitmqUsername);
        factory.setPassword(rabbitmqPassword);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.close();
        connection.close();

        return infrastructureIP;
    }

    private String getData(String identifier) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        if (rc != null) {
            return rc.getValue();
        }
        return null;
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

    private String getIp() {
        //Try to identify IP for VISP runtime, if none is set
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()));
                return in.readLine();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
        return "127.0.0.1";
    }

    @PreDestroy
    public void storeDataToDB() {
        storeSingle("runtimeIP", this.runtimeIP);
        storeSingle("infrastructureIP", this.infrastructureIP);
        storeSingle("openstackProcessingHostImage", this.openstackProcessingHostImage);
        storeSingle("processingNodeImage", this.processingNodeImage);
        storeSingle("reasoner", this.reasoner);

        try {
            Files.write(Paths.get("runtimeConfiguration/database.properties"), this.infrastructureIP.getBytes());
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public String getRabbitMQHost() {
        return infrastructureIP;
    }

    public String getRedisHost() {
        return infrastructureIP;
    }
}

