package com.jeka8833.hypixelrequester.ratelimiter;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class HypixelResponse implements Closeable {

    private final @NotNull AsyncHypixelRateLimiter rateLimiter;
    private volatile boolean isInternalError = true;

    @Blocking
    public HypixelResponse(@NotNull AsyncHypixelRateLimiter rateLimiter) throws InterruptedException {
        this.rateLimiter = rateLimiter;

        rateLimiter.block();
    }

    public void setHeaders(int statusCode, @Nullable String reset, @Nullable String limit, @Nullable String remaining)
            throws InterruptedException {
        OptionalInt resetInt = parseInt(reset);
        OptionalInt limitInt = parseInt(limit);
        OptionalInt remainingInt = parseInt(remaining);

        if (resetInt.isPresent()) rateLimiter.resetManager.addResetAfter(resetInt.getAsInt());
        limitInt.ifPresent(value -> rateLimiter.maxRequests = value);

        if (statusCode != 200 || !resetInt.isPresent() || !limitInt.isPresent() || !remainingInt.isPresent()) {
            rateLimiter.fatalError();
        } else {
            rateLimiter.callGood(remainingInt.getAsInt());
        }

        isInternalError = false;
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
    public void setHeaders(int statusCode, @Nullable OptionalLong reset,
                           @Nullable OptionalLong limit, @Nullable OptionalLong remaining) throws InterruptedException {
        if (reset != null && reset.isPresent()) rateLimiter.resetManager.addResetAfter((int) reset.getAsLong());
        if (limit != null && limit.isPresent()) rateLimiter.maxRequests = (int) limit.getAsLong();

        if (statusCode != 200 || reset == null || limit == null || remaining == null ||
                !reset.isPresent() || !limit.isPresent() || !remaining.isPresent()) {
            rateLimiter.fatalError();
        } else {
            rateLimiter.callGood((int) remaining.getAsLong());
        }

        isInternalError = false;
    }

    @Override
    public void close() {
        rateLimiter.threadList.remove(Thread.currentThread());

        if (isInternalError) rateLimiter.fatalError();
    }

    @NotNull
    @Contract(pure = true, value = "_ -> new")
    private static OptionalInt parseInt(@Nullable String value) {
        if (value == null) return OptionalInt.empty();

        try {
            return OptionalInt.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}

