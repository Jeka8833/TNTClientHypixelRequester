package com.jeka8833.hypixelrequester.ratelimiter;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ResetManager {
    private static final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                t.setDaemon(true);
                t.setName("Hypixel Rate Limiter Schedule");
                return t;
            });

    private long duration;
    private long waitTime;
    private volatile boolean noInformation = true;
    private @Nullable Future<?> scheduledToNextCall;

    private final Collection<@NotNull ResetListener> updateCallback = new ArrayList<>();
    private final Collection<@NotNull ResetListener> resetCallback = new ArrayList<>();

    private final AtomicLong resetAt = new AtomicLong();

    public ResetManager(Duration duration) {
        this(duration, Duration.ofSeconds(4));
    }

    public ResetManager(Duration duration, Duration waitTime) {
        setDuration(duration);
        setWaitTime(waitTime);
    }

    @Blocking
    public void addResetAfter(int second) throws InterruptedException {
        long resetAt = System.nanoTime() + second * 1_000_000_000L + 2 * 1_000_000_000L;

        this.resetAt.getAndUpdate(operand -> {
            if (operand > resetAt || resetAt - operand > duration / 2) return resetAt;

            return operand;
        });

        noInformation = false;

        if (this.resetAt.get() != resetAt) return;

        callUpdate();
        addResetTimer(resetAt);
    }

    @Blocking
    private synchronized void addResetTimer(long resetAt) {
        canselRetry();

        scheduledToNextCall = scheduledExecutorService.scheduleAtFixedRate(() -> {
            noInformation = true;

            try {
                callReset();
            } catch (InterruptedException ignore) {
                // scheduledFuture is canceled
            }
        }, resetAt - System.nanoTime(), duration + waitTime, TimeUnit.NANOSECONDS);
    }

    private void callReset() throws InterruptedException {
        for (ResetListener runnable : resetCallback) runnable.call(this);
    }

    private void callUpdate() throws InterruptedException {
        for (ResetListener runnable : updateCallback) runnable.call(this);
    }

    public void canselRetry() {
        if (scheduledToNextCall != null) scheduledToNextCall.cancel(true);
    }

    public void addResetCallback(@NotNull ResetListener callback) {
        resetCallback.add(callback);
    }

    public void addUpdateCallback(@NotNull ResetListener callback) {
        updateCallback.add(callback);
    }

    public long getResetAfter(TimeUnit unit) {
        return unit.convert(resetAt.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public long getResetAfterOrDefault(TimeUnit unit) {
        if (noInformation) return unit.convert(duration + waitTime, TimeUnit.NANOSECONDS);

        return unit.convert(resetAt.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public long getResetAtNanos() {
        return resetAt.get();
    }

    public long getResetAtMillis() {
        return System.currentTimeMillis() + getResetAfter(TimeUnit.MILLISECONDS);
    }

    public long getResetAtOrDefaultNanos() {
        if (noInformation) return System.nanoTime() + duration + waitTime;

        return resetAt.get();
    }

    public long getResetAtOrDefaultMillis() {
        return System.currentTimeMillis() + getResetAfterOrDefault(TimeUnit.MILLISECONDS);
    }

    public long getDuration(TimeUnit unit) {
        return unit.convert(duration, TimeUnit.NANOSECONDS);
    }

    public long getDurationOrDefault(TimeUnit unit) {
        if (noInformation) return unit.convert(duration + waitTime, TimeUnit.NANOSECONDS);

        // 1L - mathematical rounding
        return unit.convert(duration + 1L, TimeUnit.NANOSECONDS);
    }

    public void setDuration(Duration time) {
        if (time.isNegative() || time.isZero()) throw new IllegalArgumentException("duration is <= 0");

        this.duration = time.toNanos();
    }

    public long getWaitTime(TimeUnit unit) {
        return unit.convert(waitTime, TimeUnit.NANOSECONDS);
    }

    public void setWaitTime(Duration time) {
        if (time.isNegative() || time.isZero()) throw new IllegalArgumentException("duration is <= 0");

        this.waitTime = time.toNanos();
    }

    public boolean isNoInformation() {
        return noInformation;
    }

    public static void shutdown() {
        scheduledExecutorService.shutdownNow();
    }
}
