package com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntConsumer;

public final class RequesterStrategyRefill implements RefillStrategy {
    private final int maxAvailable;
    private final int maxThreads;

    public RequesterStrategyRefill(@Range(from = 0, to = Integer.MAX_VALUE) int maxAvailable,
                                   @Range(from = 0, to = Integer.MAX_VALUE) int maxThreads) {
        this.maxAvailable = maxAvailable;
        this.maxThreads = maxThreads;
    }

    @Override
    public int refill(@Range(from = 0, to = Integer.MAX_VALUE) int remaining, long durationNanos,
                      @NotNull IntConsumer addAvailableCounts, @NotNull ScheduledExecutorService executorService) {
        return remaining;
    }

    @Override
    public int atFirstRequest(int approximateWaitingThreadCount) {
        return maxAvailable - maxThreads - approximateWaitingThreadCount;
    }

    @Override
    public void stopAll() {
    }
}
