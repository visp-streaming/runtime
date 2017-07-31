package ac.at.tuwien.infosys.visp.runtime.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class Datasourceconfig {

    private static final Logger LOG = LoggerFactory.getLogger(Datasourceconfig.class);

    @Autowired
    private CredentialProperties credentialProperties;

    @Bean
    public DataSource dataSource() {
        String IP = null;

        try {
            if (Files.exists(Paths.get("runtimeConfiguration/database.properties"))) {
                IP = new String(Files.readAllBytes(Paths.get("runtimeConfiguration/database.properties")), StandardCharsets.UTF_8);
                IP = IP.replaceAll("databaseIP=", "").trim();
                IP = IP.replaceAll("database=", "").trim();
                IP = IP.replaceAll("[\\r\\n]", "").trim();
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }

        if ((IP == null) || ("".equals(IP))) {
            IP = "localhost";
        }

        LOG.info("Using database at IP " + IP);

        String uri = "jdbc:mariadb://" + IP + ":3306/visp?verifyServerCertificate=false&useSSL=false&requireSSL=false";

        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(uri);
        dataSourceBuilder.username(credentialProperties.getProperty("spring.datasource.username"));
        dataSourceBuilder.password(credentialProperties.getProperty("spring.datasource.password"));
        return dataSourceBuilder.build();
    }


}
