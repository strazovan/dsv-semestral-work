package cz.strazovan.dsv.sharedvariable.messaging;

import cz.strazovan.dsv.*;
import cz.strazovan.dsv.sharedvariable.clock.Clock;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;

import java.util.Set;

public class MessageFactory {


    public static RegisterNode createRegisterNodeMessage(String address, int port) {
        return RegisterNode.newBuilder()
                .setNodeId(NodeId.newBuilder()
                        .setIp(address)
                        .setPort(port)
                        .build())
                .setTime(Clock.INSTANCE.tick())
                .build();
    }

    public static RegisterNodeResponse createRegisterNodeResponseMessage(Set<NodeId> nodesToSend) {
        return RegisterNodeResponse.newBuilder()
                .addAllId(nodesToSend)
                .setTime(Clock.INSTANCE.tick())
                .build();
    }

    public static NewNode createNewNodeMessage(TopologyEntry node) {
        return NewNode.newBuilder()
                .setNodeId(fromTopologyEntry(node))
                .setTime(Clock.INSTANCE.tick())
                .build();
    }

    public static DataChange createDataChangeMessage(String text) {
        return DataChange.newBuilder()
                .setTime(Clock.INSTANCE.tick())
                .setData(text)
                .build();
    }

    public static LockRequest createLockRequestMessage(TopologyEntry ownTopologyEntry, long requestTs) {
        return LockRequest.newBuilder()
                .setId(fromTopologyEntry(ownTopologyEntry))
                .setRequestTimestamp(requestTs)
                .setTime(Clock.INSTANCE.tick())
                .build();
    }

    public static LockReply createLockReplyMessage(TopologyEntry ownTopologyEntry) {
        return LockReply.newBuilder()
                .setId(fromTopologyEntry(ownTopologyEntry))
                .setTime(Clock.INSTANCE.tick())
                .build();
    }

    public static DeadNodeDiscovered createDeadNodeDiscoveredMessage(TopologyEntry deadNode) {
        return DeadNodeDiscovered.newBuilder()
                .setTime(Clock.INSTANCE.tick())
                .setId(fromTopologyEntry(deadNode))
                .build();
    }

    private static NodeId fromTopologyEntry(TopologyEntry entry) {
        return NodeId.newBuilder()
                .setIp(entry.getAddressAsString())
                .setPort(entry.getPort())
                .build();
    }
}
