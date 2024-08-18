package com.jeka8833.hypixelrequester.ratelimiter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncHypixelRateLimiter {

    public static final int ONE_GROUP = Integer.MAX_VALUE;
    public static final int INFINITY_GROUPS = 1;

    final @NotNull ResetManager resetManager;
    final Set<Thread> threadList = ConcurrentHashMap.newKeySet();
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private final Condition requestCondition = requestLock.newCondition();
    volatile @Range(from = 1, to = Integer.MAX_VALUE) int maxRequests = 1;
    private @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize;
    private @Range(from = 0, to = Long.MAX_VALUE) long timeSpaceRequests;
    private @Range(from = 0, to = Long.MAX_VALUE) long sleepAfterFail;
    private @Range(from = 0, to = Long.MAX_VALUE) long noDelayZone;
    private volatile @Range(from = 0, to = Integer.MAX_VALUE) int freeRequests = 1;
    private volatile long firstTryAt = System.nanoTime();
    private volatile @Nullable Thread errorSolverThread = null;
    private long lastRequest = 0;

    public AsyncHypixelRateLimiter(@NotNull ResetManager resetManager,
                                   @Range(from = 1, to = Integer.MAX_VALUE) int groupSize,
                                   @NotNull Duration delayBetweenRequests,
                                   @NotNull Duration freeEndZone,
                                   @NotNull Duration sleepAfterFail) {
        this.resetManager = resetManager;
        setGroupSize(groupSize);
        setSleepAfterFail(sleepAfterFail);
        setTimeSpaceRequests(delayBetweenRequests);
        setNoDelayZone(freeEndZone);

        resetManager.addResetCallback((manager) -> setFreeRequests(maxRequests));
        resetManager.addUpdateCallback((manager) -> {
            requestLock.lockInterruptibly();
            try {
                requestCondition.signalAll();
            } finally {
                requestLock.unlock();
            }
        });
    }

    void block() throws InterruptedException {
        requestLock.lockInterruptibly();
        try {
            while (true) {
                if (firstTryAt != 0) {  // if fatal crash or first request
                    if (errorSolverThread == null) errorSolverThread = Thread.currentThread();

                    if (Thread.currentThread().equals(errorSolverThread)) {
                        long delay = firstTryAt - System.nanoTime();
                        if (delay > 0 && requestCondition.await(delay, TimeUnit.NANOSECONDS)) continue;
                    } else {
                        requestCondition.await();
                        continue;
                    }
                } else {
                    int queue = freeRequests - requestLock.getWaitQueueLength(requestCondition);
                    if (queue <= 0) {   // If too many threads
                        requestCondition.await();
                        continue;
                    } else {
                        long delay = getDelay(queue);
                        if (delay > 0 && requestCondition.await(delay, TimeUnit.NANOSECONDS)) continue;
                    }
                }
                // Try to make a request
                if (throttleBlock()) continue;
                break;
            }

            //noinspection NonAtomicOperationOnVolatileField
            freeRequests--;

            threadList.add(Thread.currentThread());
        } finally {
            requestLock.unlock();
        }
    }

    private boolean throttleBlock() throws InterruptedException {
        if (firstTryAt != 0 && freeRequests <= 0) freeRequests = 1;

        long delay;
        while ((delay = lastRequest - System.nanoTime()) > 0) {
            if (Thread.interrupted()) throw new InterruptedException("Throttle interrupt");

            if ((firstTryAt != 0 && !Thread.currentThread().equals(errorSolverThread)) || freeRequests <= 0)
                return true;

            LockSupport.parkNanos(delay);
        }

        if (Thread.interrupted()) throw new InterruptedException("Throttle interrupt");

        if ((firstTryAt != 0 && !Thread.currentThread().equals(errorSolverThread)) || freeRequests <= 0)
            return true;

        lastRequest = System.nanoTime() + timeSpaceRequests;
        return false;
    }

    void callGood(int remaining) {
        if (Thread.currentThread().equals(errorSolverThread)) {
            setFreeRequests(remaining);

            firstTryAt = 0;
            errorSolverThread = null;
        }
    }

    void fatalError() {
        firstTryAt = Math.max(System.nanoTime() + sleepAfterFail, resetManager.getResetAtNanos());

        requestLock.lock();
        try {
            freeRequests = 0;

            requestCondition.signalAll();

            Iterator<Thread> threadIterator = threadList.iterator();
            while (threadIterator.hasNext()) {
                threadIterator.next().interrupt();
                threadIterator.remove();
            }
        } finally {
            requestLock.unlock();
        }
    }

    private long getDelay(int requestNumber) {
        int groupRemaining = maxRequests - groupRound(maxRequests - requestNumber, false);

        return resetManager.getResetAfterOrDefault(TimeUnit.NANOSECONDS) -
                Math.multiplyExact(resetManager.getDuration(TimeUnit.NANOSECONDS), groupRemaining)
                        / maxRequests;
    }

    private int groupRound(int number, boolean maxValue) {
        int noDelayCount = Math.toIntExact(maxRequests - Math.multiplyExact(noDelayZone, maxRequests) /
                resetManager.getDuration(TimeUnit.NANOSECONDS));
        if (number > noDelayCount) {
            if (maxValue) return maxRequests;

            number = noDelayCount;
        }

        if (maxValue) {
            return (int) Math.min(maxRequests, ((long) number + groupSize - 1) / groupSize * groupSize);
        }

        return number / groupSize * groupSize;
    }

    private void setFreeRequests(int requests) {
        requestLock.lock();
        try {
            freeRequests = requests;

            requestCondition.signalAll();
        } finally {
            requestLock.unlock();
        }
    }

    public void setGroupSize(@Range(from = 1, to = Integer.MAX_VALUE) int groupSize) {
        //noinspection ConstantValue
        if (groupSize < INFINITY_GROUPS) throw new IllegalArgumentException("Group size must be > 0");

        this.groupSize = groupSize;
    }

    public void setSleepAfterFail(@NotNull Duration sleepAfterFail) {
        long time = sleepAfterFail.toNanos();
        if (time <= 0) throw new IllegalArgumentException("Sleep duration is <= 0");

        this.sleepAfterFail = sleepAfterFail.toNanos();
    }

    public void setTimeSpaceRequests(@NotNull Duration timeSpaceRequests) {
        long time = timeSpaceRequests.toNanos();
        if (time <= 0) throw new IllegalArgumentException("Space duration is <= 0");

        this.timeSpaceRequests = timeSpaceRequests.toNanos();
    }

    public void setNoDelayZone(@NotNull Duration noDelayZone) {
        long time = noDelayZone.toNanos();
        if (time <= 0) throw new IllegalArgumentException("No delay zone is <= 0");

        this.noDelayZone = noDelayZone.toNanos();
    }

    @Range(from = 1, to = Integer.MAX_VALUE)
    public int getLimit() {
        return maxRequests;
    }

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getUsed() {
        return maxRequests - freeRequests;
    }

    @Range(from = 0, to = Long.MAX_VALUE)
    public long getResetAt(@NotNull TimeUnit unit) {
        return Math.max(0, resetManager.getResetAfterOrDefault(unit));
    }

    @Range(from = 0, to = Integer.MAX_VALUE)
    public int getFreeAtMoment() {
        if (resetManager.isNoInformation()) return Math.min(groupSize, maxRequests);

        int mustCount = Math.toIntExact(
                Math.multiplyExact(resetManager.getResetAfterOrDefault(TimeUnit.NANOSECONDS), maxRequests) /
                        resetManager.getDuration(TimeUnit.NANOSECONDS));
        return Math.max(0, freeRequests - maxRequests + groupRound(maxRequests - mustCount, true));
    }

    public boolean isFail() {
        return firstTryAt != 0;
    }
}
