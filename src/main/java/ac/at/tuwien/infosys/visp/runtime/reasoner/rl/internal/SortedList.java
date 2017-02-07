package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class SortedList<T> extends LinkedList<T> {

	private static final long serialVersionUID = 6248755089440330454L;
	private Comparator<? super T> comparator = null;

	public SortedList(Comparator<? super T> comparator) {
		super();
		this.comparator = comparator;
	}

	@Override
	public boolean add(T elem) {

		if (comparator == null)
			return super.add(elem);
		
		int insertionPoint = Collections.binarySearch(this, elem, comparator);
		if (insertionPoint < 0)
			insertionPoint = 0;
		
		super.add(insertionPoint, elem);
		return true;
		
	}

	@Override
	public boolean addAll(Collection<? extends T> elemCollection) {
		
		boolean result = super.addAll(elemCollection);
		Collections.sort(this, comparator);
		return result;
		
	}

}