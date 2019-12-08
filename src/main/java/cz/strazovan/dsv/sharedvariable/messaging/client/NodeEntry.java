package cz.strazovan.dsv.sharedvariable.messaging.client;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.*;
import io.grpc.ManagedChannel;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class NodeEntry {

    private final SharedVariableServiceGrpc.SharedVariableServiceBlockingStub stub;
    private final ManagedChannel channel;

    public NodeEntry(ManagedChannel channel) {
        this.channel = channel;
        this.stub = SharedVariableServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        this.channel.shutdownNow();
    }

    public void send(AbstractMessage abstractMessage) {
        throw new UnsupportedOperationException("Cannot send abstract message.");
    }

    public void send(RegisterNode registerNode) {
        this.stub.register(registerNode);
    }

    public void send(RegisterNodeResponse registerNodeResponse) {
        this.stub.registrationDone(registerNodeResponse);
    }

    public void send(DataChange dataChange) {
        this.stub.dataChange(dataChange);
    }

    public void send(LockRequest lockRequest) {
        this.stub.lockRequest(lockRequest);
    }

    public void send(LockReply lockReply) {
        this.stub.lockReply(lockReply);
    }

    public void send(DeadNodeDiscovered deadNodeDiscovered) {
        this.stub.reportDeadNode(deadNodeDiscovered);
    }

    public ManagedChannel getChannel() {
        return channel;
    }
}