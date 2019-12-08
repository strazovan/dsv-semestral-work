package cz.strazovan.dsv.sharedvariable.messaging.client;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.sharedvariable.Component;
import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Client implements Component, TopologyChangeListener {

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final List<ManagedChannel> channels;
    private final Map<TopologyEntry, NodeEntry> nodes;

    public Client() {
        this.channels = new LinkedList<>();
        this.nodes = new LinkedHashMap<>();
    }

    public void sendMessage(TopologyEntry to, AbstractMessage message) {
        this.nodes.get(to).send(message);
    }

    @Override
    public void start() {
        logger.info("Client has started...");
    }

    @Override
    public void stop() {
        logger.info("Client is stopping...");
        this.channels.forEach(ManagedChannel::shutdownNow);
        logger.info("Client has stopped...");
    }

    @Override
    public void onNewNode(TopologyEntry nodeId) {
        final var channel = ManagedChannelBuilder.forAddress(nodeId.getAddressAsString(), nodeId.getPort())
                .usePlaintext()
                .build();
        this.channels.add(channel);
        this.nodes.put(nodeId, new NodeEntry(channel));
    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        final var entry = this.nodes.get(nodeId);
        entry.shutdown();
        this.channels.remove(entry.getChannel());
    }
}
