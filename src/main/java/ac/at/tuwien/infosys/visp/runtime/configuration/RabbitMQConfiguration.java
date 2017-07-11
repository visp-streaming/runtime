package ac.at.tuwien.infosys.visp.runtime.configuration;


import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@DependsOn("configurationprovider")
public class RabbitMQConfiguration {

    @Autowired
    private Configurationprovider config;

    @Autowired
    private CredentialProperties credentialProperties;

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(config.getRabbitMQHost(), 5672);

        factory.setUsername(credentialProperties.getProperty("spring.rabbitmq.username"));
        factory.setPassword(credentialProperties.getProperty("spring.rabbitmq.password"));

        return factory;
    }
}
