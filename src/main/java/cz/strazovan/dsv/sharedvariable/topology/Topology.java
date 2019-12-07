package cz.strazovan.dsv.sharedvariable.topology;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Topology {

    private final String ownId;
    private final Set<String> nodes = new ConcurrentSkipListSet<>();
    private final List<TopologyChangeListener> listeners = new LinkedList<>();

    public Topology(String ownId) {
        this.ownId = ownId;
    }

    public String getOwnId() {
        return this.ownId;
    }

    public Set<String> getAllOtherNodes() {
        return Set.copyOf(this.nodes);
    }

    private void addNode(String nodeId) {
        this.nodes.add(nodeId);
        this.listeners.forEach(listener -> listener.onNewNode(nodeId));
    }

    private void removeNode(String nodeId) {
        this.nodes.remove(nodeId);
        this.listeners.forEach(listener -> listener.onNodeRemoved(nodeId));
    }

    public void registerListener(TopologyChangeListener listener) {
        this.listeners.add(listener);
    }


}
