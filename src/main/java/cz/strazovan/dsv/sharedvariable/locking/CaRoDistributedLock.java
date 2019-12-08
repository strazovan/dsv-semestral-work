package cz.strazovan.dsv.sharedvariable.locking;

import com.google.protobuf.AbstractMessage;
import cz.strazovan.dsv.LockReply;
import cz.strazovan.dsv.LockRequest;
import cz.strazovan.dsv.sharedvariable.messaging.MessageListener;
import cz.strazovan.dsv.sharedvariable.messaging.MessageQueue;
import cz.strazovan.dsv.sharedvariable.topology.Topology;
import cz.strazovan.dsv.sharedvariable.topology.TopologyChangeListener;
import cz.strazovan.dsv.sharedvariable.topology.TopologyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CaRoDistributedLock implements DistributedLock, TopologyChangeListener, MessageListener {

    private static Logger logger = LoggerFactory.getLogger(CaRoDistributedLock.class);

    private final Topology topology;
    private final TopologyEntry ownTopologyEntry;
    private long myRequestTs;
    private long maxRequestTs;
    private boolean inUse;
    private Map<TopologyEntry, Boolean> requests;
    private Map<TopologyEntry, Boolean> grants;

    private static final Object _lock = new Object(); // just simple lock, we don't need ReentrantReadWriteLock here

    public CaRoDistributedLock(Topology topology) {
        this.topology = topology;
        this.ownTopologyEntry = this.topology.getOwnTopologyEntry();
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
        logger.info("Trying to acquire lock...");
        synchronized (_lock) {
            this.requests.put(this.ownTopologyEntry, true);
            this.myRequestTs = this.maxRequestTs + 1;
        }
        this.topology.getAllOtherNodes().stream()
                .filter(nodeId -> this.grants.get(nodeId))
                .forEach(this::sendRequest);

        this.waitForAllGrants();

        this.requests.put(this.ownTopologyEntry, false);
        logger.info("Lock acquired...");
        this.inUse = true;
    }

    /**
     * Blocks until we get all grants we need to lock.
     */
    private void waitForAllGrants() {
        logger.info("Waiting for all grands...");
        while (this.grants.values().stream().anyMatch(granted -> !granted)) {
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
        logger.info("Lock freed...");
        this.topology.getAllOtherNodes().forEach(nodeId -> {
            if (this.requests.get(nodeId)) {
                this.requests.put(nodeId, false);
                this.grants.put(nodeId, false);
                this.sendReply(nodeId);
            }
        });
    }


    private void sendRequest(TopologyEntry nodeId) {
        // TODO
    }

    private void sendReply(TopologyEntry nodeId) {
        // TODO
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
    }

    @Override
    public void register(MessageQueue queue) {
        queue.register(LockReply.class, this);
        queue.register(LockRequest.class, this);
    }

    private void handleLockRequest(LockRequest message) {
        final var id = message.getId();
        final var otherNode = new TopologyEntry(id.getIp(), id.getPort());

        logger.info("Received lock request from " + otherNode);

        final boolean delay;
        synchronized (_lock) {
            delay = message.getRequestTimestamp() > this.myRequestTs
                    || (message.getRequestTimestamp() == this.myRequestTs
                    && otherNode.compareTo(this.ownTopologyEntry) > 0);

        }

        if (this.inUse || (this.requests.get(this.ownTopologyEntry) && delay)) {
            this.requests.put(otherNode, true);
        }

        if (!(this.inUse || this.requests.get(this.ownTopologyEntry))
                || (this.requests.get(this.ownTopologyEntry) && !this.grants.get(otherNode)) && !delay) {
            this.sendReply(otherNode);
        }

        if (this.requests.get(this.ownTopologyEntry) && this.grants.get(otherNode) && !delay) {
            this.grants.put(otherNode, false);
            this.sendReply(otherNode);
            this.sendRequest(otherNode);
        }
    }

    private void handleLockReply(LockReply message) {
        final var id = message.getId();
        final var entry = new TopologyEntry(id.getIp(), id.getPort());
        logger.info("Received lock reply from " + entry);
        this.grants.put(entry, true);
    }

    @Override
    public void onNewNode(TopologyEntry nodeId) {
        this.requests.put(nodeId, false);
        this.grants.put(nodeId, true);
    }

    @Override
    public void onNodeRemoved(TopologyEntry nodeId) {
        this.requests.remove(nodeId);
        this.grants.remove(nodeId);
    }

}
