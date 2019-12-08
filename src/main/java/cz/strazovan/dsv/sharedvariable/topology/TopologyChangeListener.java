package cz.strazovan.dsv.sharedvariable.topology;

public interface TopologyChangeListener {

    void onNewNode(TopologyEntry nodeId);

    void onNodeRemoved(TopologyEntry nodeId);

}
