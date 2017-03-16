package ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates;

import ac.at.tuwien.infosys.visp.common.operators.Operator;

import java.util.List;

public class SizeUpdate extends ChangeToBeExecuted {
    /**
     * updates oldSize to newSize
     */
    Operator.Size oldSize;
    Operator.Size newSize;

    public SizeUpdate(Operator.Size oldSize, Operator.Size newSize) {
        this.oldSize = oldSize;
        this.newSize = newSize;
    }

    public SizeUpdate() {
    }

    public Operator.Size getOldSize() {
        return oldSize;
    }

    public void setOldSize(Operator.Size oldSize) {
        this.oldSize = oldSize;
    }

    public Operator.Size getNewSize() {
        return newSize;
    }

    public void setNewSize(Operator.Size newSize) {
        this.newSize = newSize;
    }
}