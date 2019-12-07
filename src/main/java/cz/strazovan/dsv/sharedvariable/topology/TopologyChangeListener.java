package cz.strazovan.dsv.sharedvariable.topology;

public interface TopologyChangeListener {

    void onNewNode(String nodeId);

    void onNodeRemoved(String nodeId);

}
