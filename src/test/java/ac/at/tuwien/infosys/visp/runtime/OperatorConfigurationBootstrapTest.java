package ac.at.tuwien.infosys.visp.runtime;


import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class OperatorConfigurationBootstrapTest {

    private OperatorConfigurationBootstrap operatorConfigurationBootstrap = new OperatorConfigurationBootstrap(false);

    String correctInput = "{\"operators\" : [\n" +
            "    {\n" +
            "    \"name\" : \"calculateperformance\",\n" +
            "    \"cores\" : 0.4,\n" +
            "    \"memory\" : 534,\n" +
            "    \"storage\" : 300\n" +
            "    },\n" +
            "    {\n" +
            "    \"name\" : \"calculateavailability\",\n" +
            "    \"cores\" : 0.56,\n" +
            "    \"memory\" : 417,\n" +
            "    \"storage\" : 300\n" +
            "    },\n" +
            "    {\n" +
            "    \"name\" : \"calculatequality\",\n" +
            "    \"cores\" : 0.42,\n" +
            "    \"memory\" : 527,\n" +
            "    \"storage\" : 300\n" +
            "    }\n" +
            "]\n}";

    String faultyInput = "{\"operators\" : [\n" +
            "    {\n" +
            "    \"name\" : \"calculateperformance\",\n" +
            "    \"cores\" : \"xxx\",\n" +
            "    \"memory\" : 534,\n" +
            "    \"storage\" : 300\n" +
            "    }\n" +
            "]\n}";

    @Test
    public void testDataParserPasses() throws IOException {
        assertEquals(true, operatorConfigurationBootstrap.getOperatorConfiguration().isEmpty());
        operatorConfigurationBootstrap.parseOperatorConfigurationData(correctInput);
        assertEquals(3, operatorConfigurationBootstrap.getOperatorConfiguration().size());

        ResourceTriple rt = operatorConfigurationBootstrap.getOperatorConfiguration().get("calculateperformance");
        assertEquals(Double.valueOf(0.4), rt.getCores());
        assertEquals(Integer.valueOf(534), rt.getMemory());
        assertEquals(Float.valueOf(300), rt.getStorage());
    }

    @Test(expected = NumberFormatException.class)
    public void testDataParserFails() throws IOException {
        assertEquals(true, operatorConfigurationBootstrap.getOperatorConfiguration().isEmpty());
        operatorConfigurationBootstrap.parseOperatorConfigurationData(faultyInput);
    }


}
