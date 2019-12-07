package cz.strazovan.dsv.sharedvariable;

import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;

public class Main {

    public static void main(String[] args) {
        final var messageQueue = new MessageQueue();
        messageQueue.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> messageQueue.stop()));
    }
}
