package at.tuwien.infosys;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.ProcessingDurationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations="classpath:application.properties")
public class EvaluationTests {

    @Autowired
    private ProcessingDurationRepository pcr;

    @Autowired
    private DockerContainerRepository scaling;


    @Before
    public void setup() {
    }

    @Test
    public void generate() {





    }




}
