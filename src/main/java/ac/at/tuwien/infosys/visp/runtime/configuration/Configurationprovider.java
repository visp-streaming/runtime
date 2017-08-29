package ac.at.tuwien.infosys.visp.runtime.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import ac.at.tuwien.infosys.visp.runtime.exceptions.ResourceException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
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
    private Integer btu = null;
    public Integer monitoringperiod = null;
    private Integer availabilitycheck = null;
    private Integer simulatestartup = null;
    private Integer shutdowngrace = null;
    private Integer reasoninginterval = null;
    private Integer upscalingthreshold = null;
    private Boolean openstackondemand = false;
    private Boolean publicip = false;
    private Boolean cleanupresourcepools = true;


    private static final Logger LOG = LoggerFactory.getLogger(Configurationprovider.class);

    @Bean
    public Integer monitoringperiod() {
        return monitoringperiod;
    }

    @Bean
    public Integer availabilitycheck() {
        return availabilitycheck;
    }

    @Bean
    public Integer reasoninginterval() {
        return reasoninginterval;
    }

    @PostConstruct
    public void initialize() {
        this.runtimeIP = getData("runtimeIP");
        this.infrastructureIP = getData("infrastructureIP");
        this.openstackProcessingHostImage = getData("openstackProcessingHostImage");
        this.processingNodeImage = getData("processingNodeImage");
        this.reasoner = getData("reasoner");
        this.btu = getDataAsInt("btu");
        this.monitoringperiod = getDataAsInt("monitoringperiod");
        this.availabilitycheck = getDataAsInt("availabilitycheck");
        this.simulatestartup = getDataAsInt("simulatestartup");
        this.shutdowngrace = getDataAsInt("shutdowngrace");
        this.reasoninginterval = getDataAsInt("resoninginterval");
        this.upscalingthreshold = getDataAsInt("upscalingthreshold");
        this.openstackondemand = getDataAsBoolean("openstackondemand");
        this.publicip = getDataAsBoolean("publicip");
        this.cleanupresourcepools = getDataAsBoolean("cleanupresourcepools");


        if (this.runtimeIP == null) {
            this.runtimeIP = getIp();
        }

        if (this.infrastructureIP == null) {
            this.infrastructureIP = "localhost";
        }

        if (this.openstackProcessingHostImage == null) {
            this.openstackProcessingHostImage = defaultOpenstackProcessingHostImage;
        }

        if (this.processingNodeImage == null) {
            this.processingNodeImage = defaultProcessingNodeImage;
        }

        if (this.reasoner == null) {
            this.reasoner = "none";
        }

        if (this.btu == null) {
            //default value = 600 sec
            this.btu = 600;
        }

        if (this.monitoringperiod == null) {
            //default value = 15000 millisec
            this.monitoringperiod = 15000;
        }

        if (this.availabilitycheck == null) {
            //default value = 60000 millisec
            this.availabilitycheck = 60000;
        }

        if (this.simulatestartup == null) {
            //default value = 15  sec
            this.simulatestartup = 15;
        }

        if (this.shutdowngrace == null) {
            //default value = 20  sec
            this.shutdowngrace = 20;
        }

        if (this.reasoninginterval == null) {
            //default value = 60000  millisec
            this.reasoninginterval = 60000;
        }

        if (this.upscalingthreshold == null) {
            //default value = 50
            this.upscalingthreshold = 50;
        }

        if (this.openstackondemand == null) {
            this.openstackondemand = false;
        }

        if (this.publicip == null) {
            this.publicip = false;
        }

        if (this.cleanupresourcepools == null) {
            this.cleanupresourcepools = true;
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
                throw new ResourceException("Could neither connect to localhost nor to database IP");
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

    private Integer getDataAsInt(String identifier) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        if (rc != null) {
            return Integer.parseInt(rc.getValue());
        }
        return null;
    }

    private Boolean getDataAsBoolean(String identifier) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        if (rc != null) {
            if ("true".equals(rc.getValue())) {
                return true;
            } else {
                return false;
            }
        }
        return false;
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

    private void storeSingle(String identifier, Integer value) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        if (rc == null) {
            rfr.saveAndFlush(new RuntimeConfiguration(identifier, String.valueOf(value)));
        } else {
            rc.setValue(String.valueOf(value));
            rfr.saveAndFlush(rc);
        }
    }

    private void storeSingle(String identifier, Boolean value) {
        RuntimeConfiguration rc = rfr.findFirstByKey(identifier);
        String stringvalue = "false";

        if (value) {
            stringvalue = "true";
        }

        if (rc == null) {
            rfr.saveAndFlush(new RuntimeConfiguration(identifier, stringvalue));
        } else {
            rc.setValue(stringvalue);
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
        return "localhost";
    }

    @PreDestroy
    public void storeDataToDB() {
        storeSingle("runtimeIP", this.runtimeIP);
        storeSingle("infrastructureIP", this.infrastructureIP);
        storeSingle("openstackProcessingHostImage", this.openstackProcessingHostImage);
        storeSingle("processingNodeImage", this.processingNodeImage);
        storeSingle("reasoner", this.reasoner);
        storeSingle("btu", this.btu);
        storeSingle("monitoringperiod", this.monitoringperiod);
        storeSingle("availabilitycheck", this.availabilitycheck);
        storeSingle("simulatestartup", this.simulatestartup);
        storeSingle("shutdowngrace", this.shutdowngrace);
        storeSingle("reasoninginterval", this.reasoninginterval);
        storeSingle("upscalingthreshold", this.upscalingthreshold);
        storeSingle("openstackondemand", this.openstackondemand);
        storeSingle("publicip", this.publicip);
        storeSingle("cleanupresourcepools", this.cleanupresourcepools);

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

    public String getRedisOperatorHost() {
        if ("localhost".equals(infrastructureIP)) {
            return runtimeIP;
        } else {
            return infrastructureIP;
        }
    }


}

