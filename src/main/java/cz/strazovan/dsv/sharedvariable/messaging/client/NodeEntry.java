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
        if (abstractMessage instanceof RegisterNode)
            this.send(((RegisterNode) abstractMessage));
        else if(abstractMessage instanceof DisconnectNode) {
            this.send(((DisconnectNode) abstractMessage));
        }
        else if (abstractMessage instanceof RegisterNodeResponse)
            this.send(((RegisterNodeResponse) abstractMessage));
        else if (abstractMessage instanceof NewNode) {
            this.send(((NewNode) abstractMessage));
        } else if (abstractMessage instanceof DataChange)
            this.send(((DataChange) abstractMessage));
        else if (abstractMessage instanceof LockRequest)
            this.send(((LockRequest) abstractMessage));
        else if (abstractMessage instanceof LockReply)
            this.send(((LockReply) abstractMessage));
        else if (abstractMessage instanceof DeadNodeDiscovered)
            this.send(((DeadNodeDiscovered) abstractMessage));
        else {
            throw new IllegalArgumentException("Unsupported message type");
        }

    }

    private void send(RegisterNode registerNode) {
        this.stub.register(registerNode);
    }

    private void send(DisconnectNode disconnectNode){
        this.stub.disconnect(disconnectNode);
    }

    private void send(RegisterNodeResponse registerNodeResponse) {
        this.stub.registrationDone(registerNodeResponse);
    }

    private void send(NewNode newNode) {
        this.stub.newNode(newNode);
    }

    private void send(DataChange dataChange) {
        this.stub.dataChange(dataChange);
    }

    private void send(LockRequest lockRequest) {
        this.stub.lockRequest(lockRequest);
    }

    private void send(LockReply lockReply) {
        this.stub.lockReply(lockReply);
    }

    private void send(DeadNodeDiscovered deadNodeDiscovered) {
        this.stub.reportDeadNode(deadNodeDiscovered);
    }

    public ManagedChannel getChannel() {
        return channel;
    }
}
