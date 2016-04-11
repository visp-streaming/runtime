package at.tuwien.infosys;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.resourceManagement.DockerContainerManagement;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.reasoner.Reasoner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VispApplication.class)
@TestPropertySource(locations="classpath:test.properties")
public class ReasonerTests {

    @Autowired
    private DockerContainerManagement dcm;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private Reasoner reasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerTests.class);

    private DockerHost spaceForThreeContainerHost1 = new DockerHost();
    private DockerHost spaceforOneContainerHost1 = new DockerHost();
    private DockerHost spaceforNoContainerHost1 = new DockerHost();


    private DockerContainer container1 = new DockerContainer();

    @Before
    public void prepareAndCleanup() {
        dhr.deleteAll();
        dcr.deleteAll();

        assertFalse(dhr.findAll().iterator().hasNext());
        assertFalse(dcr.findAll().iterator().hasNext());


        spaceForThreeContainerHost1.setCores(4.0);
        spaceForThreeContainerHost1.setRam(1000);
        spaceForThreeContainerHost1.setName("bigHost1");
        spaceForThreeContainerHost1.setStorage(40F);
        spaceForThreeContainerHost1.setUrl("bigHostURL1");
        spaceForThreeContainerHost1.setScheduledForShutdown(false);

        spaceforOneContainerHost1.setCores(2.0);
        spaceforOneContainerHost1.setRam(1100);
        spaceforOneContainerHost1.setName("smallhost1");
        spaceforOneContainerHost1.setStorage(10F);
        spaceforOneContainerHost1.setUrl("smallHostURL1");
        spaceforOneContainerHost1.setScheduledForShutdown(false);

        spaceforNoContainerHost1.setCores(0.5);
        spaceforNoContainerHost1.setRam(200);
        spaceforNoContainerHost1.setName("fullHost1");
        spaceforNoContainerHost1.setStorage(10F);
        spaceforNoContainerHost1.setUrl("fullHostURL1");
        spaceforNoContainerHost1.setScheduledForShutdown(false);


        container1.setContainerid("container1");
        container1.setImage("imageID");
        container1.setCpuCores(1.0);
        container1.setStorage(5);
        container1.setRam(1000);
        container1.setOperator("container1");


    }

    @Test
    public void szenario1() {
        dhr.save(spaceForThreeContainerHost1);

        assertEquals("bigHostURL1",reasoner.selectSuitableDockerHost(container1));
    }


    @Test
    public void szenario2() {
        dhr.save(spaceForThreeContainerHost1);
        dhr.save(spaceforOneContainerHost1);

        //for binpacking Strategy
        assertEquals("smallHostURL1",reasoner.selectSuitableDockerHost(container1));

        //for equaldistribution Strategy
        //assertEquals("bigHostURL1",reasoner.selectSuitableDockerHost(container1));
    }

    @Test
    public void szenario3() {
        dhr.save(spaceforNoContainerHost1);
        dhr.save(spaceForThreeContainerHost1);
        dhr.save(spaceforOneContainerHost1);

        //for binpacking Strategy
        assertEquals("smallHostURL1",reasoner.selectSuitableDockerHost(container1));

        //for equaldistribution Strategy
        //assertEquals("bigHostURL1",reasoner.selectSuitableDockerHost(container1));
    }

    /**
     * 1 voller Host
     * 1 container soll geloescht werden
     *
     */
    @Test
    public void szenario4() {

    }

    /**
     * 1 leerer Host
     * 1 halbvoller Host
     * 1 host soll gelöscht weden
     *
     */
    @Test
    public void szenario5() {

    }

    /**
     * 2 fast leerer Host
     * consolidate hosts
     * 1 host soll gelöscht weden
     *
     */
    @Test
    public void szenario6() {

    }

    //TODO consider already shutting down hosts
}
