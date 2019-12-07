package cz.strazovan.dsv.sharedvariable;

import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.ServiceImpl;
import io.grpc.ServerBuilder;

public class Main {

    public static void main(String[] args) {
        final int port;
        if (args.length == 0) {
            System.out.println("No port parameter provided. Server will be started on port 18080.");
            port = 8080;
        } else {
            port = Integer.parseInt(args[0]);
        }

        final var messageQueue = new MessageQueue();
        messageQueue.start();

        final var service = new ServiceImpl(messageQueue);

        final var server = ServerBuilder.forPort(port)
                .addService(service)
                .build();
        try {
            server.start();
            server.awaitTermination();
        } catch (Exception ex) {
            throw new RuntimeException(ex); // todo logger
        }

        Runtime.getRuntime().addShutdownHook(new Thread(messageQueue::stop));
    }
}
