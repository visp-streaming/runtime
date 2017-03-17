package ac.at.tuwien.infosys.visp.runtime.configuration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;
import java.util.Properties;

@Configuration
@DependsOn("configurationprovider")
public class RabbitMQConfiguration {

    @Autowired
    private Configurationprovider config;

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConfiguration.class);

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(config.getRabbitMQHost(), 5672);

        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("credential.properties"));
        } catch (IOException e) {
            LOG.error("Could not load credential properties.", e);
        }

        factory.setUsername(prop.getProperty("spring.rabbitmq.username"));
        factory.setPassword(prop.getProperty("spring.rabbitmq.password"));

        return factory;
    }
}
