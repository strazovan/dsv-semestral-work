package cz.strazovan.dsv.sharedvariable;

import cz.strazovan.dsv.sharedvariable.locking.CaRoDistributedLock;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.server.Server;
import cz.strazovan.dsv.sharedvariable.topology.Topology;

import javax.swing.*;

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

        final var topology = new Topology("127.0.0.1", port);
        topology.register(messageQueue);
        final var lock = new CaRoDistributedLock(topology);
        lock.register(messageQueue);

        final var server = new Server(port, messageQueue);
        server.start();

        final var frame = new JFrame();
        frame.setName("Distributed todo list");
        frame.setSize(800, 800);
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            messageQueue.stop();
            server.stop();
        }));
    }
}
