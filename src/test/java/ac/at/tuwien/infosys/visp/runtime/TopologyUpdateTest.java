package ac.at.tuwien.infosys.visp.runtime;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SourcesUpdate;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;


public class TopologyUpdateTest {

    private Operator source = new ProcessingOperator();
    private Operator step1 = new ProcessingOperator();
    private Operator step2 = new ProcessingOperator();
    private Operator step3 = new ProcessingOperator();
    private Operator step4 = new ProcessingOperator();
    private Operator step5 = new ProcessingOperator();
    private Operator log = new ProcessingOperator();

    @Before
    public void init() {
        source.setName("source");
        step1.setName("step1");
        step2.setName("step2");
        step3.setName("step3");
        step4.setName("step4");
        step5.setName("step5");
        log.setName("log");
    }

    class TopologyPair {
        private Map<String, Operator> oldTopology;
        private Map<String, Operator> newTopology;
    }

    private TopologyPair getTopologiesFromFiles(String file1, String file2) {
        TopologyParser parser = new TopologyParser();
        TopologyParser.ParseResult oldTopology = parser.parseTopologyFromClasspath(file1);
        TopologyParser.ParseResult newTopology = parser.parseTopologyFromClasspath(file2);

        TopologyPair pair = new TopologyPair();
        pair.oldTopology = oldTopology.topology;
        pair.newTopology = newTopology.topology;

        return pair;

    }

    private List<TopologyUpdate> getUpdatesForFiles(String file1, String file2) {
        TopologyPair pair = getTopologiesFromFiles(file1, file2);

        TopologyUpdateHandler handler = new TopologyUpdateHandler();
        return handler.computeListOfUpdates(pair.oldTopology, pair.newTopology);
    }

    @Test
    public void testIdenticalTopologiesNoChanges() {
        List<TopologyUpdate> updatesToPerform = getUpdatesForFiles("topologyUpdateTest_01.conf", "topologyUpdateTest_01.conf");
        assertTrue(updatesToPerform.size() == 0);
    }

    @Test
    public void testChangeLocationOfOneOperatorMigrationIsPerformed() {
        List<TopologyUpdate> updatesToPerform = getUpdatesForFiles("topologyUpdateTest_01.conf", "topologyUpdateTest_02.conf");

        // in 02.conf, operatorType "step1" has a different allowedLocation (192.168.0.2)

        assertTrue(updatesToPerform.contains(new TopologyUpdate("192.168.0.1", TopologyUpdate.Action.REMOVE_OPERATOR, step1)));
        assertTrue(updatesToPerform.contains(new TopologyUpdate("192.168.0.2", TopologyUpdate.Action.ADD_OPERATOR, step1)));
    }

    @Test
    public void testAddNewOperatorOperatorIsAdded() {
        List<TopologyUpdate> updatesToPerform = getUpdatesForFiles("topologyUpdateTest_01.conf", "topologyUpdateTest_03.conf");

        assertTrue(updatesToPerform.contains(new TopologyUpdate("192.168.0.2", TopologyUpdate.Action.ADD_OPERATOR, step2)));
        assertTrue(updatesToPerform.size() == 1);
    }

    @Test
    public void test_removeOperator_operatorIsRemoved() {
        List<TopologyUpdate> updatesToPerform = getUpdatesForFiles("topologyUpdateTest_03.conf", "topologyUpdateTest_04.conf");

        assertTrue(updatesToPerform.contains(new TopologyUpdate("192.168.0.2", TopologyUpdate.Action.REMOVE_OPERATOR, step2)));
        assertTrue(updatesToPerform.size() == 1);
    }

    @Test
    public void testSourceIsAddedOperatorIsUpdated() {
        List<TopologyUpdate> updatesToPerform = getUpdatesForFiles("topologyUpdateTest_03.conf", "topologyUpdateTest_05.conf");
        assertTrue(updatesToPerform.size() == 1);
        TopologyUpdate tp = updatesToPerform.get(0);
        assertTrue(tp.getAffectedHost().equals("192.168.0.2"));
        assertTrue(tp.getAction() == TopologyUpdate.Action.UPDATE_OPERATOR);
        assertTrue(tp.getAffectedOperatorId().equals("step2"));
        assertTrue(tp.getUpdateType() == TopologyUpdate.UpdateType.UPDATE_SOURCE);
        assertTrue(((SourcesUpdate)tp.getChangeToBeExecuted()).getNewSources().size() == 2);
        assertTrue(((SourcesUpdate)tp.getChangeToBeExecuted()).getOldSources().size() == 1);
    }


}