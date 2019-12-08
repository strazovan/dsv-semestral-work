package cz.strazovan.dsv.sharedvariable.messaging.client;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.sharedvariable.Component;
import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
        try {
            this.nodes.
                    computeIfAbsent(to, entry -> new NodeEntry(this.buildChannel(entry)))
                    .send(message);
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                logger.error("Dead node detected (" + to + ")");
            } else
                logger.error("An error has occurred while sending the message", ex);
        }
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
        final var channel = this.buildChannel(nodeId);
        this.channels.add(channel);
        this.nodes.put(nodeId, new NodeEntry(channel));
    }

    private ManagedChannel buildChannel(TopologyEntry nodeId) {
        return ManagedChannelBuilder.forAddress(nodeId.getAddressAsString(), nodeId.getPort())
                .usePlaintext()
                .build();
    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        final var entry = this.nodes.get(nodeId);
        entry.shutdown();
        this.channels.remove(entry.getChannel());
    }
}
