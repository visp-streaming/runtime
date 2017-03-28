package ac.at.tuwien.infosys.visp.runtime.topology;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.ChangeToBeExecuted;

public class TopologyUpdate {
    private String affectedHost; // which host is affected by this update

    public enum Action {ADD_OPERATOR, REMOVE_OPERATOR, UPDATE_OPERATOR}

    private Action action; // what kind of update is performed

    public enum UpdateType {UPDATE_SIZE, UPDATE_SOURCE}

    private UpdateType updateType;

    private ChangeToBeExecuted changeToBeExecuted;

    private boolean changeTopologyMessageFlow; // false if only a migration to another resource pool is happening

    private Operator affectedOperator;

    public Operator getAffectedOperator() {
        return affectedOperator;
    }

    public void setAffectedOperator(Operator affectedOperator) {
        this.affectedOperator = affectedOperator;
    }

    public ChangeToBeExecuted getChangeToBeExecuted() {
        return changeToBeExecuted;
    }

    public void setChangeToBeExecuted(ChangeToBeExecuted changeToBeExecuted) {
        this.changeToBeExecuted = changeToBeExecuted;
    }


    public TopologyUpdate(String affectedHost, Action action, Operator affectedOperator) {
        this.affectedHost = affectedHost;
        this.action = action;
        this.affectedOperator = affectedOperator;
        this.updateType = null;
        this.changeTopologyMessageFlow = true;
    }

    public TopologyUpdate(String affectedHost, Action action, UpdateType updateType, Operator affectedOperator) {
        if(!action.equals(Action.UPDATE_OPERATOR)) {
            throw new RuntimeException("Invalid constructor used for action " + action.toString());
        }
        this.affectedHost = affectedHost;
        this.action = action;
        this.updateType = updateType;
        this.affectedOperator = affectedOperator;
        this.changeTopologyMessageFlow = true;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TopologyUpdate that = (TopologyUpdate) o;

        if (!affectedHost.equals(that.affectedHost)) {
            return false;
        }
        if (action != that.action) {
            return false;
        }
        return affectedOperator.getName().equals(that.getAffectedOperator().getName());

    }

    @Override
    public int hashCode() {
        int result = affectedHost.hashCode();
        result = 31 * result + action.hashCode();
        result = 31 * result + affectedOperator.getName().hashCode();
        return result;
    }
    public UpdateType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType;
    }
    public String getAffectedHost() {
        return affectedHost;
    }

    public void setAffectedHost(String affectedHost) {
        this.affectedHost = affectedHost;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getAffectedOperatorId() {
        return affectedOperator.getName();
    }

    public boolean shouldChangeTopologyMessageFlow() {
        return changeTopologyMessageFlow;
    }

    public void setChangeTopologyMessageFlow(boolean changeTopologyMessageFlow) {
        this.changeTopologyMessageFlow = changeTopologyMessageFlow;
    }

    @Override
    public String toString() {
        return "TopologyUpdate{" +
                "affectedHost='" + affectedHost + '\'' +
                ", action=" + action +
                ", updateType=" + updateType +
                ", changeToBeExecuted=" + changeToBeExecuted +
                ", affectedOperatorId='" + getAffectedOperatorId() + '\'' +
                '}';
    }

}
