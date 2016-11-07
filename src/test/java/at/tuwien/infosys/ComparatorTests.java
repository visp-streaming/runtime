package at.tuwien.infosys;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.ProcessingDurationRepository;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ProcessingDuration;
import at.tuwien.infosys.entities.ResourceAvailability;
import at.tuwien.infosys.entities.ResourceComparator;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:application.properties")
public class ComparatorTests {

    @Autowired
    private ProcessingDurationRepository pcr;

    @Autowired
    private DockerContainerRepository dcr;

    DockerHost dh = new DockerHost();

    //assumption all hosts are the same with 10 cores, 1000 ram and 1000 storage
    ResourceAvailability ra1 = new ResourceAvailability(dh, 1, 1.0, 900, 800F);
    ResourceAvailability ra2 = new ResourceAvailability(dh, 2, 2.0, 800, 800F);
    ResourceAvailability ra3 = new ResourceAvailability(dh, 3, 3.0, 700, 900F);
    ResourceAvailability ra4 = new ResourceAvailability(dh, 4, 4.0, 600, 900F);
    ResourceAvailability ra5 = new ResourceAvailability(dh, 5, 5.0, 500, 400F);
    ResourceAvailability ra6 = new ResourceAvailability(dh, 6, 6.0, 400, 400F);
    ResourceAvailability ra7 = new ResourceAvailability(dh, 7, 7.0, 300, 500F);
    ResourceAvailability ra8 = new ResourceAvailability(dh, 8, 8.0, 200, 600F);
    ResourceAvailability ra9 = new ResourceAvailability(dh, 9, 9.0, 100, 200F);
    ResourceAvailability ra10 = new ResourceAvailability(dh, 10, 10.0, 50, 100F);

    List<ResourceAvailability> raList = new ArrayList<>();

    @Before
    public void setup() {
        raList.add(ra1);
        raList.add(ra2);
        raList.add(ra3);
        raList.add(ra4);
        raList.add(ra5);
        raList.add(ra6);
        raList.add(ra7);
        raList.add(ra8);
        raList.add(ra9);
        raList.add(ra10);
    }

    @Test
    public void testComparator() {
        Collections.sort(raList, ResourceComparator.AMOUNTOFCONTAINERASC);
        raList.forEach(System.out::println);
    }



    @Test
    public void testRegression() {
        double[][] data = { { 1, 3 }, {2, 5 }, {3, 7 }, {4, 14 }, {5, 11 }};
        SimpleRegression regression = new SimpleRegression(false);
        regression.addData(data);
        System.out.println(regression.predict(6));

    }

    @Test
    public void testQuery() {
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), "aaa", 1.0));
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), "aaa", -2.0));
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), "aaa", 3.0));
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), "aaa", -4.0));
        pcr.save(new ProcessingDuration(new DateTime(DateTimeZone.UTC), "bbb", 1.0));

        List <ProcessingDuration> pds = pcr.findFirst5ByOperatorOrderByIdDesc("aaa");

        Integer count = 4;
        double[][] data = new double[5][2];
        for (ProcessingDuration pd : pds) {
            data[count][0] = count;
            data[count][1] = pd.getDuration();
            count--;
        }

        SimpleRegression regression = new SimpleRegression(false);
        regression.addData(data);
        System.out.println(regression.predict(6));


    }


}
