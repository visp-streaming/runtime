package at.tuwien.infosys;


import at.tuwien.infosys.topology.TopologyParser;
import org.junit.Test;

public class ParserTest {


    @Test
    public void parseTest() {
        TopologyParser parser = new TopologyParser();

        parser.loadTopology("topologyConfiguration/edoc.conf");

    }

}
