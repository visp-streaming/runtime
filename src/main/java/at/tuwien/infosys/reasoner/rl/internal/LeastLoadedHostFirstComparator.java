package at.tuwien.infosys.reasoner.rl.internal;

import java.util.Comparator;

import at.tuwien.infosys.entities.ResourceAvailability;

public class LeastLoadedHostFirstComparator implements Comparator<ResourceAvailability>{

	@Override
	public int compare(ResourceAvailability o1, ResourceAvailability o2) {
		
		if (o1.getAmountOfContainer() < o2.getAmountOfContainer())
			return -1;
		
		if (o1.getAmountOfContainer() > o2.getAmountOfContainer())
			return 1;
		
		return 0;
	}
	
}