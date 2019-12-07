package cz.strazovan.dsv.sharedvariable.locking;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CaRoDistributedLock implements DistributedLock {

    private final String ownName;
    private long myRequestTs;
    private long maxRequestTs;
    private boolean inUse;
    private Map<String, Boolean> requests;
    private Map<String, Boolean> grants;

    private static final Object _lock = new Object(); // just simple lock, we don't need ReentrantReadWriteLock here

    public CaRoDistributedLock(String ownName) {
        this.ownName = ownName;
        this.init();
    }

    private void init() {
        this.myRequestTs = 0;
        this.maxRequestTs = 0;
        this.inUse = false;
        this.requests = new ConcurrentHashMap<>();
        this.requests.put(this.ownName, false);
        this.grants = new ConcurrentHashMap<>();
    }

    @Override
    public void lock() {
        synchronized (_lock) {
            this.requests.put(this.ownName, true);
            this.myRequestTs = this.maxRequestTs + 1;
        }
        this.getAllOtherNodes().stream()
                .map(Map.Entry::getKey)
                .filter(nodeId -> this.grants.get(nodeId))
                .forEach(this::sendRequest);

        this.waitForAllGrants();

        this.requests.put(this.ownName, false);
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
        final var otherNodes = getAllOtherNodes();
        for (Map.Entry<String, Boolean> otherNode : otherNodes) {
            if (otherNode.getValue()) {
                this.requests.put(otherNode.getKey(), false);
                this.grants.put(otherNode.getKey(), false);
                this.sendReply(otherNode.getKey());
            }
        }
    }

    private List<Map.Entry<String, Boolean>> getAllOtherNodes() {
        return this.requests.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(this.ownName))
                .collect(Collectors.toList());
    }

    private void sendRequest(String nodeId) {
        // TODO
    }

    private void sendReply(String nodeId) {
        // TODO
    }
}
