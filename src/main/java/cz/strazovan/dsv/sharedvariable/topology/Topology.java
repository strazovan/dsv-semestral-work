package cz.strazovan.dsv.sharedvariable.topology;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.*;
import cz.strazovan.dsv.sharedvariable.messaging.MessageListener;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class Topology implements MessageListener {


    private static Logger logger = LoggerFactory.getLogger(Topology.class);

    private final String ownId;
    private final int ownPort;
    private final Set<TopologyEntry> nodes = new ConcurrentSkipListSet<>();
    private final List<TopologyChangeListener> listeners = new LinkedList<>();
    private Client client;

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
        if (nodeId.equals(this.ownTopologyEntry)) // there is no point in registering ourselves
            return;
        logger.info("New node has registered " + nodeId);
        this.nodes.add(nodeId);
        this.listeners.forEach(listener -> listener.onNewNode(nodeId));
    }

    private void removeNode(TopologyEntry nodeId) {
        logger.info("Node has disconnected " + nodeId);
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
            final TopologyEntry node = new TopologyEntry(id.getIp(), id.getPort());
            this.addNode(node);
            this.broadcastNewClient(node);
            this.sendRegistrationReply(node);
        } else if (message instanceof NewNode) {
            final var id = ((NewNode) message).getNodeId();
            this.addNode(new TopologyEntry(id.getIp(), id.getPort()));
        } else if (message instanceof RegisterNodeResponse) {
            final var registerResponse = ((RegisterNodeResponse) message);
            registerResponse.getIdList()
                    .stream()
                    .map(nodeId -> new TopologyEntry(nodeId.getIp(), nodeId.getPort()))
                    .forEach(this::addNode);
        } else if (message instanceof DeadNodeDiscovered) {
            final var id = ((DeadNodeDiscovered) message).getId();
            this.removeNode(new TopologyEntry(id.getIp(), id.getPort()));
        }
    }

    private void broadcastNewClient(TopologyEntry node) {
        final var message = NewNode.newBuilder()
                .setNodeId(NodeId.newBuilder()
                        .setIp(node.getAddressAsString())
                        .setPort(node.getPort())
                        .build())
                .build();
        this.client.broadcast(message);
    }

    private void sendRegistrationReply(TopologyEntry node) {
        final Set<NodeId> nodesToSend = this.getAllExcept(node);

        nodesToSend.add(NodeId.newBuilder()
                .setIp(this.ownId)
                .setPort(this.ownPort)
                .build());
        final var reply = RegisterNodeResponse.newBuilder()
                .addAllId(nodesToSend)
                .build();
        this.client.sendMessage(node, reply);
    }

    private Set<NodeId> getAllExcept(TopologyEntry node) {
        return this.nodes
                .stream()
                .filter(entry -> !entry.equals(node))
                .map(entry -> NodeId.newBuilder()
                        .setIp(entry.getAddressAsString())
                        .setPort(entry.getPort())
                        .build())
                .collect(Collectors.toSet());
    }

    @Override
    public void register(MessageQueue queue) {
        queue.register(RegisterNode.class, this);
        queue.register(RegisterNodeResponse.class, this);
        queue.register(NewNode.class, this);
        queue.register(DeadNodeDiscovered.class, this);
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
