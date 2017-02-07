package ac.at.tuwien.infosys.visp.runtime;


import ac.at.tuwien.infosys.visp.runtime.topology.TopologyParser;
import org.junit.Test;

public class ParserTest {


    @Test
    public void parseTest() {
        TopologyParser parser = new TopologyParser();

        parser.loadTopology("topologyConfiguration/edoc.conf");

    }

}
