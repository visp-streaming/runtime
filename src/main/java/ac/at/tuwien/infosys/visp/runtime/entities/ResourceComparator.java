package ac.at.tuwien.infosys.visp.runtime.entities;

import java.util.Comparator;

public class ResourceComparator {
    public static final Comparator<ResourceAvailability> AMOUNTOFCONTAINERASC = Comparator.comparing(ResourceAvailability::getAmountOfContainer);

    public static final Comparator<ResourceAvailability> FREECPUCORESASC = Comparator.comparing(ResourceAvailability::getCores);

    public static final Comparator<ResourceAvailability> FREERAMASC = Comparator.comparing(ResourceAvailability::getMemory);

    public static final Comparator<ResourceAvailability> FREESTORAGEASC = Comparator.comparing(ResourceAvailability::getStorage);

    public static final Comparator<ResourceAvailability> AMOUNTOFCONTAINERDESC = (o1, o2) -> o2.getAmountOfContainer().compareTo(o1.getAmountOfContainer());

    public static final Comparator<ResourceAvailability> FREECPUCORESDESC = (o1, o2) -> o2.getCores().compareTo(o1.getCores());

    public static final Comparator<ResourceAvailability> FREERAMDESC = (o1, o2) -> o2.getMemory().compareTo(o1.getMemory());

    public static final Comparator<ResourceAvailability> FREESTORAGEDESC = (o1, o2) -> o2.getStorage().compareTo(o1.getStorage());

}
