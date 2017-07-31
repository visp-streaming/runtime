package ac.at.tuwien.infosys.visp.runtime.configuration;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisShardInfo;

@Configuration
@DependsOn("configurationprovider")
public class SpringRedisConfig {

    @Autowired
    private Configurationprovider config;

    @Bean
    public JedisConnectionFactory connectionFactory() {
        JedisShardInfo jedisconfig = new JedisShardInfo(config.getInfrastructureIP(), 6379);


        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(jedisconfig);
        return connectionFactory;
    }

    @Bean
    public RedisTemplate redisTemplate() {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory());
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate strRedisTemplate() {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory());
        return redisTemplate;
    }
}
