package cz.strazovan.dsv.sharedvariable.messaging;

import cz.strazovan.dsv.*;
import io.grpc.stub.StreamObserver;

public class ServiceImpl extends SharedVariableServiceGrpc.SharedVariableServiceImplBase {

    @Override
    public void register(RegisterNode request, StreamObserver<Empty> responseObserver) {
        super.register(request, responseObserver);
    }

    @Override
    public void registrationDone(RegisterNodeResponse request, StreamObserver<Empty> responseObserver) {
        super.registrationDone(request, responseObserver);
    }

    @Override
    public void dataChange(DataChange request, StreamObserver<Empty> responseObserver) {
        super.dataChange(request, responseObserver);
    }

    @Override
    public void lockRequest(LockRequest request, StreamObserver<Empty> responseObserver) {
        super.lockRequest(request, responseObserver);
    }

    @Override
    public void lockReply(LockReply request, StreamObserver<Empty> responseObserver) {
        super.lockReply(request, responseObserver);
    }

    @Override
    public void reportDeadNode(DeadNodeDiscovered request, StreamObserver<Empty> responseObserver) {
        super.reportDeadNode(request, responseObserver);
    }
}
