package cz.strazovan.dsv.sharedvariable.topology;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.DeadNodeDiscovered;
import cz.strazovan.dsv.RegisterNode;
import cz.strazovan.dsv.sharedvariable.messaging.MessageListener;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Topology implements MessageListener {

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
        if (this.ownTopologyEntry == null) {
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


    @Override
    public void processMessage(AbstractMessage message) {
        if (message instanceof RegisterNode) {
            final var id = ((RegisterNode) message).getNodeId();
            this.addNode(new TopologyEntry(id.getIp(), id.getPort()));
        } else if (message instanceof DeadNodeDiscovered) {
            final var id = ((DeadNodeDiscovered) message).getId();
            this.removeNode(new TopologyEntry(id.getIp(), id.getPort()));
        }
    }

    @Override
    public void register(MessageQueue queue) {
        queue.register(RegisterNode.class, this);
        queue.register(DeadNodeDiscovered.class, this);
    }
}
