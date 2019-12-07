package cz.strazovan.dsv.sharedvariable.clock;

import java.util.concurrent.atomic.AtomicLong;

public class Clock {

    private final AtomicLong timer = new AtomicLong(0);

    long tick() {
        return timer.addAndGet(1);
    }

    long getCurrentTime() {
        return timer.get();
    }

    synchronized void adjust(long otherTime) {
        final var max = Math.max(timer.get(), otherTime);
        timer.set(max + 1);
    }

}
