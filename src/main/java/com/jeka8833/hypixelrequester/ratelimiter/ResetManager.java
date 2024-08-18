package com.jeka8833.hypixelrequester.ratelimiter;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ResetManager {
    private static final ScheduledExecutorService SCHEDULE = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.setName("Hypixel Rate Limiter Schedule");
        return t;
    });
    private final Collection<@NotNull ResetListener> updateCallback = new ArrayList<>();
    private final Collection<@NotNull ResetListener> resetCallback = new ArrayList<>();
    private final AtomicLong nextResetAtNanos = new AtomicLong();
    private long durationNanos;
    private long waitTimeNanos;
    private volatile boolean noInformation = true;
    private @Nullable ScheduledFuture<?> scheduledToNextCall;

    public ResetManager(Duration duration) {
        this(duration, Duration.ofSeconds(1));
    }

    public ResetManager(Duration duration, Duration waitTime) {
        setDuration(duration);
        setWaitTime(waitTime);
    }

    public static void shutdown() {
        SCHEDULE.shutdownNow();
    }

    @Blocking
    public void addResetAfter(int second) throws InterruptedException {
        long resetAtNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(second) + 999_999_999;

        nextResetAtNanos.getAndUpdate(oldValue -> {
            if (oldValue > resetAtNanos || resetAtNanos - oldValue > durationNanos / 2) return resetAtNanos;

            return oldValue;
        });

        noInformation = false;

        if (nextResetAtNanos.get() != resetAtNanos) return;

        for (ResetListener runnable : updateCallback) runnable.call(this);
        updateTimerSettings();
    }

    @Blocking
    private synchronized void updateTimerSettings() {
        if (scheduledToNextCall != null) scheduledToNextCall.cancel(true);

        scheduledToNextCall = SCHEDULE.scheduleAtFixedRate(() -> {
                    noInformation = true;

                    try {
                        for (ResetListener runnable : resetCallback) runnable.call(this);
                    } catch (InterruptedException ignore) {
                        // scheduledFuture is canceled
                    }
                }, nextResetAtNanos.get() - System.nanoTime() + waitTimeNanos,
                durationNanos + waitTimeNanos, TimeUnit.NANOSECONDS);
    }

    public void addResetCallback(@NotNull ResetListener callback) {
        resetCallback.add(callback);
    }

    public void addUpdateCallback(@NotNull ResetListener callback) {
        updateCallback.add(callback);
    }

    public long getResetAfter(TimeUnit unit) {
        return unit.convert(nextResetAtNanos.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public long getResetAfterOrDefault(TimeUnit unit) {
        if (noInformation) return unit.convert(durationNanos, TimeUnit.NANOSECONDS);

        return getResetAfter(unit);
    }

    public long getResetAtNanos() {
        return nextResetAtNanos.get();
    }

    public long getResetAtOrDefaultNanos() {
        if (noInformation) return System.nanoTime() + durationNanos + waitTimeNanos;

        return getResetAtNanos();
    }

    public long getDuration(TimeUnit unit) {
        return unit.convert(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void setDuration(Duration time) {
        if (time.isNegative() || time.isZero()) throw new IllegalArgumentException("duration is <= 0");

        this.durationNanos = time.toNanos();
    }

    public long getWaitTime(TimeUnit unit) {
        return unit.convert(waitTimeNanos, TimeUnit.NANOSECONDS);
    }

    public void setWaitTime(Duration time) {
        if (time.isNegative() || time.isZero()) throw new IllegalArgumentException("duration is <= 0");

        this.waitTimeNanos = time.toNanos();
    }

    public boolean isNoInformation() {
        return noInformation;
    }
}
