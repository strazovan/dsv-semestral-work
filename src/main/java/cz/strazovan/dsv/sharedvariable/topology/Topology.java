package cz.strazovan.dsv.sharedvariable.topology;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Topology {

    private final String ownId;
    private final int ownPort;
    private final Set<TopologyEntry> nodes = new ConcurrentSkipListSet<>();
    private final List<TopologyChangeListener> listeners = new LinkedList<>();

    private TopologyEntry ownTopologyEntry = null; // lets make this lazy

    public Topology(String ownId, int ownPort) {
        this.ownId = ownId;
        this.ownPort = ownPort;
    }

    public String getOwnId() {
        return this.ownId;
    }

    public int getOwnPort() {
        return ownPort;
    }

    public TopologyEntry getOwnTopologyEntry() {
        if (this.ownTopologyEntry != null) {
            this.ownTopologyEntry = new TopologyEntry(this.ownId, this.ownPort);
        }
        return this.ownTopologyEntry;
    }

    public Set<TopologyEntry> getAllOtherNodes() {
        return Set.copyOf(this.nodes);
    }

    private void addNode(TopologyEntry nodeId) {
        this.nodes.add(nodeId);
        this.listeners.forEach(listener -> listener.onNewNode(nodeId));
    }

    private void removeNode(TopologyEntry nodeId) {
        this.nodes.remove(nodeId);
        this.listeners.forEach(listener -> listener.onNodeRemoved(nodeId));
    }

    public void registerListener(TopologyChangeListener listener) {
        this.listeners.add(listener);
    }


}
