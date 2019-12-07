package cz.strazovan.dsv.sharedvariable.locking;

import cz.strazovan.dsv.sharedvariable.topology.Topology;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CaRoDistributedLock implements DistributedLock {


    private final Topology topology;
    private long myRequestTs;
    private long maxRequestTs;
    private boolean inUse;
    private Map<String, Boolean> requests;
    private Map<String, Boolean> grants;

    private static final Object _lock = new Object(); // just simple lock, we don't need ReentrantReadWriteLock here

    public CaRoDistributedLock(Topology topology) {
        this.topology = topology;
        this.init();
    }

    private void init() {
        this.myRequestTs = 0;
        this.maxRequestTs = 0;
        this.inUse = false;
        this.requests = new ConcurrentHashMap<>();
        this.requests.put(this.topology.getOwnId(), false);
        this.grants = new ConcurrentHashMap<>();
    }

    @Override
    public void lock() {
        synchronized (_lock) {
            this.requests.put(this.topology.getOwnId(), true);
            this.myRequestTs = this.maxRequestTs + 1;
        }
        this.topology.getAllOtherNodes().stream()
                .filter(nodeId -> this.grants.get(nodeId))
                .forEach(this::sendRequest);

        this.waitForAllGrants();

        this.requests.put(this.topology.getOwnId(), false);
        this.inUse = true;
    }

    /**
     * Blocks until we get all grants we need to lock.
     */
    private void waitForAllGrants() {
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
        // todo handle invalid calls to this method
        this.inUse = false;
        this.topology.getAllOtherNodes().forEach(nodeId -> {
            if (this.requests.get(nodeId)) {
                this.requests.put(nodeId, false);
                this.grants.put(nodeId, false);
                this.sendReply(nodeId);
            }
        });
    }


    private void sendRequest(String nodeId) {
        // TODO
    }

    private void sendReply(String nodeId) {
        // TODO
    }
}
