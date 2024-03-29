package cz.strazovan.dsv.sharedvariable.locking;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.LockReply;
import cz.strazovan.dsv.LockRequest;
import cz.strazovan.dsv.sharedvariable.clock.Clock;
import cz.strazovan.dsv.sharedvariable.messaging.MessageFactory;
import cz.strazovan.dsv.sharedvariable.messaging.MessageListener;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.messaging.client.Client;
import cz.strazovan.dsv.sharedvariable.topology.Topology;
import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CaRoDistributedLock implements DistributedLock, TopologyChangeListener, MessageListener {

    private static Logger logger = LoggerFactory.getLogger(CaRoDistributedLock.class);

    private final Topology topology;
    private final Client client;
    private final TopologyEntry ownTopologyEntry;
    private long myRequestTs;
    private volatile long maxRequestTs;
    private boolean inUse;
    private Map<TopologyEntry, Boolean> requests;
    private Map<TopologyEntry, Boolean> grants;

    private List<LockStateListener> listeners;

    private static final Object _lock = new Object(); // just simple lock, we don't need ReentrantReadWriteLock here

    public CaRoDistributedLock(Topology topology, Client client) {
        this.listeners = new LinkedList<>();
        this.topology = topology;
        this.ownTopologyEntry = this.topology.getOwnTopologyEntry();
        this.client = client;
        this.init();
        this.topology.registerListener(this);

    }

    private void init() {
        this.myRequestTs = 0;
        this.maxRequestTs = 0;
        this.inUse = false;
        this.requests = new ConcurrentHashMap<>();
        this.requests.put(this.ownTopologyEntry, false);
        this.grants = new ConcurrentHashMap<>();
    }

    @Override
    public void lock() {
        Clock.INSTANCE.inMDCCWithTime(() -> logger.info("Trying to acquire lock..."));
        synchronized (_lock) {
            this.requests.put(this.ownTopologyEntry, true);
            this.myRequestTs = this.maxRequestTs + 1;
        }
        this.topology.getAllOtherNodes().stream()
                .filter(nodeId -> !this.grants.get(nodeId))
                .forEach(this::sendRequest);

        this.waitForAllGrants();

        this.requests.put(this.ownTopologyEntry, false);
        Clock.INSTANCE.inMDCCWithTime(() -> logger.info("Lock acquired..."));
        this.inUse = true;
    }

    /**
     * Blocks until we get all grants we need to lock.
     */
    private void waitForAllGrants() {
        Clock.INSTANCE.inMDCCWithTime(() -> logger.info("Waiting for all grands..."));
        while (!this.grants.values().stream().allMatch(granted -> granted)) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ex) {
                throw new RuntimeException(); // todo
            }
        }
    }

    @Override
    public void withLock(Runnable task) {
        this.lock();
        task.run();
        this.unlock();
    }

    @Override
    public void unlock() {
        if (!this.inUse) throw new IllegalStateException("Calling unlock when not locked");
        this.inUse = false;
        Clock.INSTANCE.inMDCCWithTime(() -> logger.info("Lock freed..."));
        this.topology.getAllOtherNodes().forEach(nodeId -> {
            if (this.requests.get(nodeId)) {
                this.requests.computeIfPresent(nodeId, (entry, grant) -> false);
                this.grants.computeIfPresent(nodeId, (entry, grant) -> false);
                this.sendReply(nodeId);
            }
        });
    }

    @Override
    public void registerStateListener(LockStateListener listener) {
        this.listeners.add(listener);
    }


    private void sendRequest(TopologyEntry nodeId) {
        final var message = MessageFactory.createLockRequestMessage(this.ownTopologyEntry, this.myRequestTs);
        this.client.sendMessage(nodeId, message);
    }

    private void sendReply(TopologyEntry nodeId) {
        final var message = MessageFactory.createLockReplyMessage(this.ownTopologyEntry);
        this.client.sendMessage(nodeId, message);
    }

    @Override
    public void processMessage(AbstractMessage message) {
        if (message instanceof LockReply) {
            this.handleLockReply(((LockReply) message));
        } else if (message instanceof LockRequest) {
            this.handleLockRequest(((LockRequest) message));
        } else {
            throw new UnsupportedOperationException("Can not handle " + message.getClass() + " message.");
        }

        this.listeners.forEach(LockStateListener::onLockStateChange);
    }

    @Override
    public void register(MessageQueue queue) {
        queue.register(LockReply.class, this);
        queue.register(LockRequest.class, this);
    }

    private void handleLockRequest(LockRequest message) {
        final var id = message.getId();
        final var otherNode = new TopologyEntry(id.getIp(), id.getPort());

        Clock.INSTANCE.inMDCCWithTime(() -> logger.info("Received lock request from " + otherNode));

        final boolean delay;
        synchronized (_lock) {
            this.maxRequestTs = Math.max(this.maxRequestTs, message.getRequestTimestamp());
            delay = message.getRequestTimestamp() > this.myRequestTs
                    || (message.getRequestTimestamp() == this.myRequestTs
                    && otherNode.compareTo(this.ownTopologyEntry) > 0);

        }

        if (this.inUse || (this.requests.get(this.ownTopologyEntry) && delay)) {
            this.requests.computeIfPresent(otherNode, (entry, grant) -> true);
        }

        if (!(this.inUse || this.requests.get(this.ownTopologyEntry))
                || ((this.requests.get(this.ownTopologyEntry) && !this.grants.get(otherNode)) && !delay)) {
            this.grants.computeIfPresent(otherNode, (entry, grant) -> false);
            this.sendReply(otherNode);
        }

        if (this.requests.get(this.ownTopologyEntry) && this.grants.get(otherNode) && !delay) {
            this.grants.computeIfPresent(otherNode, (entry, grant) -> false);
            this.sendReply(otherNode);
            this.sendRequest(otherNode);
        }
    }

    private void handleLockReply(LockReply message) {
        final var id = message.getId();
        final var entry = new TopologyEntry(id.getIp(), id.getPort());
        Clock.INSTANCE.inMDCCWithTime(() -> logger.info("Received lock reply from " + entry));
        this.grants.computeIfPresent(entry, (e, grant) -> true);
    }

    @Override
    public void onNewNode(TopologyEntry nodeId) {
        this.requests.put(nodeId, false);
        this.grants.put(nodeId, false);
    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        this.requests.remove(nodeId);
        this.grants.remove(nodeId);
    }


    public boolean hasGrantFrom(TopologyEntry entry) {
        return this.grants.getOrDefault(entry, false);
    }

    public boolean hasRequestFrom(TopologyEntry entry) {
        return this.requests.getOrDefault(entry, false);
    }

}
