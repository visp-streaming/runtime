package at.tuwien.infosys;

import at.tuwien.infosys.entities.ResourceAvailability;
import at.tuwien.infosys.entities.ResourceComparator;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComparatorTests {

    //assumption all hosts are the same with 10 cores, 1000 ram and 1000 storage
    ResourceAvailability ra1 = new ResourceAvailability("host1", 1, 1.0, 900, 800F, "", "");
    ResourceAvailability ra2 = new ResourceAvailability("host2", 2, 2.0, 800, 800F, "", "");
    ResourceAvailability ra3 = new ResourceAvailability("host3", 3, 3.0, 700, 900F, "", "");
    ResourceAvailability ra4 = new ResourceAvailability("host4", 4, 4.0, 600, 900F, "", "");
    ResourceAvailability ra5 = new ResourceAvailability("host5", 5, 5.0, 500, 400F, "", "");
    ResourceAvailability ra6 = new ResourceAvailability("host6", 6, 6.0, 400, 400F, "", "");
    ResourceAvailability ra7 = new ResourceAvailability("host7", 7, 7.0, 300, 500F, "", "");
    ResourceAvailability ra8 = new ResourceAvailability("host8", 8, 8.0, 200, 600F, "", "");
    ResourceAvailability ra9 = new ResourceAvailability("host9", 9, 9.0, 100, 200F, "", "");
    ResourceAvailability ra10 = new ResourceAvailability("host10", 10, 10.0, 50, 100F, "", "");

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
        for(ResourceAvailability ra : raList) {
            System.out.println(ra);
        }
    }

}
