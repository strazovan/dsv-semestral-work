package cz.strazovan.dsv.sharedvariable.messaging;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.sharedvariable.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MessageQueue implements Runnable, Component {

    private static Logger logger = LoggerFactory.getLogger(MessageQueue.class);
    private final BlockingQueue<AbstractMessage> messages = new LinkedBlockingQueue<>();
    private final Map<Class<? extends AbstractMessage>, List<MessageListener>> messageListeners = new LinkedHashMap<>();

    final ExecutorService notificationsService = Executors.newSingleThreadExecutor();

    public MessageQueue() {
    }

    public void register(Class<? extends AbstractMessage> messageType, MessageListener listener) {
        this.messageListeners.computeIfAbsent(messageType, aClass -> new LinkedList<>()).add(listener);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.consume();
            } catch (InterruptedException ex) {
                logger.error("Consumption was interrupted", ex);
            }
        }
    }

    private void consume() throws InterruptedException {
        final var message = this.messages.take();
        this.messageListeners.get(message.getClass())
                .forEach(messageListener -> messageListener.processMessage(message));
    }

    @Override
    public void start() {
        logger.info("Message queue is starting...");
        this.notificationsService.submit(this);
    }

    @Override
    public void stop() {
        logger.info("Message queue is stopping...");
        this.notificationsService.shutdownNow();
        try {
            this.notificationsService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.warn("Notification service shutdown was interrupted.", ex);
        }
    }
}
