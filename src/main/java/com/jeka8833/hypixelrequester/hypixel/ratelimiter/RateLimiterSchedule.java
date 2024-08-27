package com.jeka8833.hypixelrequester.hypixel.ratelimiter;

import com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy.RefillStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiterSchedule {
    protected final long retryAfterFailNanos;
    protected final @NotNull RateLimiterLock rateLimiterLock;
    protected final @NotNull RefillStrategy refillStrategy;
    protected final @NotNull ScheduledExecutorService executorService;

    protected final Lock lock = new ReentrantLock();

    protected boolean waitFirstAnswer = true;
    protected volatile boolean knownRemaining = false;

    protected @Nullable Future<?> firstCallTimer;

    public RateLimiterSchedule(long retryAfterFailNanos, @NotNull RateLimiterLock rateLimiterLock,
                               @NotNull RefillStrategy refillStrategy,
                               @NotNull ScheduledExecutorService executorService) {
        this.retryAfterFailNanos = retryAfterFailNanos;
        this.rateLimiterLock = rateLimiterLock;
        this.refillStrategy = refillStrategy;
        this.executorService = executorService;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void setStatusCodeAndAvailable(int statusCode, OptionalInt remaining, OptionalInt reset) {
        int remainingValue = remaining.orElse(0);
        if (statusCode == 403) {            // Invalid API key
            rateLimiterLock.setRemaining(0);
            refillStrategy.stopAll();

            knownRemaining = false;

            return;
        } else if (statusCode == 429) {     // Too many requests
            rateLimiterLock.setRemaining(0);
            refillStrategy.stopAll();

            knownRemaining = false;

            remainingValue = 0;
        }

        long resetValue = reset.isPresent() ?
                TimeUnit.SECONDS.toNanos(reset.getAsInt()) + 999_999_999L : retryAfterFailNanos;

        setFirstCallValue(remainingValue, resetValue);
    }

    protected void setFirstCallValue(int remaining, long durationNanos) {
        if (waitFirstAnswer) {
            waitFirstAnswer = false;

            if (remaining == 0) {
                rateLimiterLock.setRemaining(remaining);
            } else {
                Thread currentThread = Thread.currentThread();

                int value = refillStrategy.refill(remaining, durationNanos, remaining1 -> {
                    if (knownRemaining) {
                        rateLimiterLock.addRemaining(remaining1);
                    } else if (Thread.currentThread().equals(currentThread)) {
                        rateLimiterLock.setRemaining(remaining1);
                    }
                }, executorService);

                rateLimiterLock.setRemaining(value);
            }

            knownRemaining = true;
        }

        lock.lock();
        try {
            if (firstCallTimer == null || firstCallTimer.isDone()) {
                firstCallTimer = executorService.schedule(() -> {
                    knownRemaining = false;
                    refillStrategy.stopAll();

                    try {
                        rateLimiterLock.requestFirstCall();
                        waitFirstAnswer = true;
                    } catch (InterruptedException ignored) {
                    }
                }, durationNanos, TimeUnit.NANOSECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isKnownRemaining() {
        return knownRemaining;
    }

    public void endCall() {
        if (waitFirstAnswer) {
            waitFirstAnswer = false;
            setFirstCallValue(0, retryAfterFailNanos);
        }
    }
}
