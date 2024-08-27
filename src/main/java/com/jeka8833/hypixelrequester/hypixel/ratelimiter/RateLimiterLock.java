package com.jeka8833.hypixelrequester.hypixel.ratelimiter;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiterLock {
    protected final long delayBetweenCallsNanos;

    protected final Collection<Thread> waitingThreads = ConcurrentHashMap.newKeySet();
    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition condition = lock.newCondition();

    protected int remaining = 1;
    protected long nextAllowTime;

    public RateLimiterLock(long delayBetweenCallsNanos) {
        this.delayBetweenCallsNanos = delayBetweenCallsNanos;

        nextAllowTime = System.nanoTime() - delayBetweenCallsNanos;
    }

    public void join() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            long sleepThrottleTime;
            do {
                while (remaining <= 0 || lock.hasWaiters(condition)) {
                    condition.await();  // Block till signal, more optimized than await(time)
                }

                // Only one thread can pass this point
                sleepThrottleTime = nextAllowTime - System.nanoTime();
            } while ((sleepThrottleTime > 0 &&
                    condition.await(sleepThrottleTime, TimeUnit.NANOSECONDS))
                    || remaining <= 0);   // Don't change order of conditions!!

            remaining--;

            waitingThreads.add(Thread.currentThread());

            condition.signalAll();  // Recalculate because nextAllowTime is changed
            nextAllowTime = System.nanoTime() + delayBetweenCallsNanos;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryJoin(boolean reserve) {
        lock.lock();
        try {
            boolean success = remaining > 0 && nextAllowTime - System.nanoTime() <= 0L &&
                    !lock.hasWaiters(condition);
            if (success && reserve) {
                remaining--;

                waitingThreads.add(Thread.currentThread());

                condition.signalAll();  // Recalculate because nextAllowTime is changed
                nextAllowTime = System.nanoTime() + delayBetweenCallsNanos;
            }

            return success;
        } finally {
            lock.unlock();
        }
    }

    public void setRemaining(int remaining) {
        lock.lock();
        try {
            this.remaining = remaining;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void addRemaining(int remaining) {
        lock.lock();
        try {
            this.remaining += remaining;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void requestFirstCall() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            remaining = 0;
            condition.signalAll();

            while (!waitingThreads.isEmpty()) {
                condition.await();
            }

            remaining = 1;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void interruptAll() {
        for (Thread thread : waitingThreads) {
            thread.interrupt();
        }
    }

    public void releaseThread() {
        waitingThreads.remove(Thread.currentThread());

        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int getApproximateWaitingThreadsCount() {
        lock.lock();
        try {
            return lock.getWaitQueueLength(condition);
        } finally {
            lock.unlock();
        }
    }

    public int getRemaining() {
        return remaining;
    }
}
