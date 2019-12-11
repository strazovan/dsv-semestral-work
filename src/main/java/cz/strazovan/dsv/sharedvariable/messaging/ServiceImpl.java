package cz.strazovan.dsv.sharedvariable.messaging;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.*;
import cz.strazovan.dsv.sharedvariable.clock.Clock;
import io.grpc.stub.StreamObserver;

public class ServiceImpl extends SharedVariableServiceGrpc.SharedVariableServiceImplBase {

    private final MessageQueue messageQueue;

    public ServiceImpl(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public void register(RegisterNode request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }


    @Override
    public void disconnect(DisconnectNode request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    @Override
    public void registrationDone(RegisterNodeResponse request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    @Override
    public void newNode(NewNode request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    @Override
    public void dataChange(DataChange request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    @Override
    public void lockRequest(LockRequest request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    @Override
    public void lockReply(LockReply request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    @Override
    public void reportDeadNode(DeadNodeDiscovered request, StreamObserver<Empty> responseObserver) {
        this.processMessage(request, responseObserver, request.getTime());
    }

    private void processMessage(AbstractMessage request, StreamObserver<Empty> responseObserver, long messageTime) {
        Clock.INSTANCE.adjust(messageTime);
        this.messageQueue.enqueueMessage(request);
        responseObserver.onNext(this.buildEmptyResponse());
        responseObserver.onCompleted();
    }

    private Empty buildEmptyResponse() {
        return Empty.newBuilder().build();
    }
}
