package com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.IntConsumer;

public interface RefillStrategy {
    int refill(@Range(from = 0, to = Integer.MAX_VALUE) int remaining, long durationNanos,
               @NotNull IntConsumer addAvailableCounts, @NotNull ScheduledExecutorService executorService);

    @Range(from = 0, to = Integer.MAX_VALUE)
    int atFirstRequest(@Range(from = 0, to = Integer.MAX_VALUE) int approximateWaitingThreadCount);

    void stopAll();
}
