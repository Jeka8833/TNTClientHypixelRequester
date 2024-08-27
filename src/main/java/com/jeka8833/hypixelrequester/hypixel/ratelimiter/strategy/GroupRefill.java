package com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

public final class GroupRefill implements RefillStrategy {
    private final int maxAvailable;
    private final int groupCount;

    private @Nullable Future<?> task;

    public GroupRefill(int maxAvailable, int groupCount) {
        this.maxAvailable = maxAvailable;
        this.groupCount = groupCount;
    }

    @Override
    public int refill(int remaining, long durationNanos, @NotNull IntConsumer addAvailableCounts,
                      @NotNull ScheduledExecutorService executorService) {
        if (remaining < groupCount) return remaining;

        if (task != null) task.cancel(true);

        Queue<Integer> parts = splitIntoParts(remaining, groupCount);

        task = executorService.scheduleAtFixedRate(() -> {
            Integer groutCapacity = parts.poll();
            if (groutCapacity == null) throw new RuntimeException("Stop");

            addAvailableCounts.accept(groutCapacity);
        }, durationNanos / parts.size(), durationNanos / parts.size(), TimeUnit.NANOSECONDS);

        Integer first = parts.poll();
        return first == null ? 0 : first;
    }

    @Override
    public int atFirstRequest(int approximateWaitingThreadCount) {
        return Math.max(0, maxAvailable / (groupCount * 2) - approximateWaitingThreadCount);
    }

    @Override
    public void stopAll() {
        if (task != null) {
            task.cancel(true);
        }
    }

    @VisibleForTesting
    static Queue<Integer> splitIntoParts(int whole, int parts) {
        Queue<Integer> queue = new ArrayBlockingQueue<>(parts);
        for (int i = 0; i < parts; i++) {
            int part = (whole + parts - i - 1) / (parts - i);
            if (part == 0) break;

            queue.offer(part);
            whole -= part;
        }
        return queue;
    }
}
