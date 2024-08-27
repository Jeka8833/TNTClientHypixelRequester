package com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntConsumer;

public final class FullRefill implements RefillStrategy {
    private final int maxAvailable;

    public FullRefill(@Range(from = 0, to = Integer.MAX_VALUE) int maxAvailable) {
        this.maxAvailable = maxAvailable;
    }

    @Override
    public int refill(int remaining, long durationNanos, @NotNull IntConsumer addAvailableCounts,
                      @NotNull ScheduledExecutorService executorService) {
        return remaining;
    }

    @Override
    public int atFirstRequest(int approximateWaitingThreadCount) {
        return Math.max(0, maxAvailable / 2 - approximateWaitingThreadCount);
    }

    @Override
    public void stopAll() {
    }
}
