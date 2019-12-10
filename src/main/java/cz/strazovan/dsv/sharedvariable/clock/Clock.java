package cz.strazovan.dsv.sharedvariable.clock;

import org.slf4j.MDC;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public enum Clock {
    INSTANCE;

    private final AtomicLong timer = new AtomicLong(0);
    private List<ClockListener> listeners = new LinkedList<ClockListener>();

    public synchronized long tick() {
        final long newTime = timer.addAndGet(1);
        listeners.forEach(clockListener -> clockListener.onTimeChange(newTime));
        return newTime;
    }

    public long getCurrentTime() {
        return timer.get();
    }

    public synchronized void adjust(long otherTime) {
        final var max = Math.max(timer.get(), otherTime);
        timer.set(max + 1);
        listeners.forEach(clockListener -> clockListener.onTimeChange(max + 1));
    }

    public synchronized void inMDCCWithTime(Runnable loggingRunnable) {
        try (var ignored = MDC.putCloseable("logical-time", String.valueOf(this.getCurrentTime()))) {
            loggingRunnable.run();
        }
    }

    public void registerClockListener(ClockListener listener) {
        this.listeners.add(listener);
    }

}
