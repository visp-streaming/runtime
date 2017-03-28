package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import java.util.Comparator;

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