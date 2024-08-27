package com.jeka8833.hypixelrequester.hypixel.ratelimiter;

import com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy.RefillStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class HypixelRateLimiter {
    protected final RateLimiterLock rateLimiterLock;
    protected final RateLimiterSchedule rateLimiterSchedule;
    protected final @NotNull RefillStrategy refillStrategy;

    public HypixelRateLimiter(long retryAfterFailNanos, long delayBetweenCallsNanos,
                              @NotNull RefillStrategy refillStrategy) {
        this(retryAfterFailNanos, delayBetweenCallsNanos, refillStrategy, Executors.newSingleThreadScheduledExecutor());
    }

    public HypixelRateLimiter(long retryAfterFailNanos, long delayBetweenCallsNanos,
                              @NotNull RefillStrategy refillStrategy,
                              @NotNull ScheduledExecutorService executorService) {
        this.refillStrategy = refillStrategy;

        rateLimiterLock = new RateLimiterLock(delayBetweenCallsNanos);
        rateLimiterSchedule =
                new RateLimiterSchedule(retryAfterFailNanos, rateLimiterLock, refillStrategy, executorService);
    }

    @NotNull
    @Contract(" -> new")
    public Status newRequest() throws InterruptedException {
        rateLimiterLock.join();

        return new Status(this);
    }

    @NotNull
    public RateLimiterLock getRateLimiterLock() {
        return rateLimiterLock;
    }

    @NotNull
    public RateLimiterSchedule getRateLimiterSchedule() {
        return rateLimiterSchedule;
    }

    public int getRemaining() {
        if (!rateLimiterSchedule.isKnownRemaining()) {
            return refillStrategy.atFirstRequest(rateLimiterLock.getApproximateWaitingThreadsCount());
        }

        return rateLimiterLock.getRemaining();
    }

    public static class Status implements AutoCloseable {

        protected final HypixelRateLimiter rateLimiter;

        protected Status(HypixelRateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        public void connectionInfo(int statusCode, @Nullable String remaining, @Nullable String reset) {
            rateLimiter.rateLimiterSchedule.setStatusCodeAndAvailable(statusCode,
                    parseInteger(remaining), parseInteger(reset));

            rateLimiter.rateLimiterLock.releaseThread();
        }

        @Override
        public void close() {
            rateLimiter.rateLimiterLock.releaseThread();
            rateLimiter.rateLimiterSchedule.endCall();
        }

        @NotNull
        protected static OptionalInt parseInteger(@Nullable String value) {
            if (value == null || value.isEmpty()) return OptionalInt.empty();

            try {
                return OptionalInt.of(Integer.parseInt(value));
            } catch (Exception e) {
                return OptionalInt.empty();
            }
        }
    }
}
