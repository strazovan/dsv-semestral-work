package cz.strazovan.dsv.sharedvariable.locking;

public interface DistributedLock {

    void lock();

    // utility method for convenient usage
    void withLock(Runnable task);

    void unlock();
}
