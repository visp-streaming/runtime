package ac.at.tuwien.infosys.visp.runtime.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Configuration
public class Datasourceconfig {

    private static final Logger LOG = LoggerFactory.getLogger(Datasourceconfig.class);

    @Bean
    public DataSource dataSource() {
        String IP = null;

        try {

        if (Files.exists(Paths.get("database.properties"))) {
            IP = new String(Files.readAllBytes(Paths.get("database.properties")), StandardCharsets.UTF_8);
            IP = IP.replaceAll("\\r|\\n", "").trim();
        }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }

        if (IP ==null) {
            IP = getIp();
        }

        String uri = "jdbc:mysql://" + IP + ":3306/visp?verifyServerCertificate=false&useSSL=false&requireSSL=false";

        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("credential.properties"));
        } catch (IOException e) {
            LOG.error("Could not load credential properties.", e);
        }

        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(uri);
        dataSourceBuilder.username(prop.getProperty("spring.datasource.username"));
        dataSourceBuilder.password(prop.getProperty("spring.datasource.password"));
        return dataSourceBuilder.build();
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
