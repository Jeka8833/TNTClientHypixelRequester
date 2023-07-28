package com.jeka8833.hypixelrequester.ratelimiter;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.NoSuchElementException;
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
        if (!(statusCode == 200 || statusCode == 429)) rateLimiter.fatalError();

        try {
            if (reset == null) throw new NumberFormatException("Cannot parse null string");
            rateLimiter.resetManager.addResetAfter(Integer.parseInt(reset));

            if (limit == null) throw new NumberFormatException("Cannot parse null string");
            rateLimiter.maxRequests = Integer.parseInt(limit);

            if (statusCode == 200 || statusCode == 429) {
                if (remaining == null) throw new NumberFormatException("Cannot parse null string");
                rateLimiter.callGood(Integer.parseInt(remaining));
            }
        } catch (NumberFormatException ignore) {
            if (statusCode == 200) rateLimiter.fatalError();
        }

        isInternalError = false;
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "DataFlowIssue", "OptionalGetWithoutIsPresent"})
    public void setHeaders(int statusCode, @Nullable OptionalLong reset,
                           @Nullable OptionalLong limit, @Nullable OptionalLong remaining) throws InterruptedException {
        if (!(statusCode == 200 || statusCode == 429)) rateLimiter.fatalError();

        try {
            rateLimiter.resetManager.addResetAfter((int) reset.getAsLong());
            rateLimiter.maxRequests = (int) limit.getAsLong();

            if (statusCode == 200 || statusCode == 429) rateLimiter.callGood((int) remaining.getAsLong());
        } catch (NoSuchElementException | NullPointerException ignore) {
            if (statusCode == 200) rateLimiter.fatalError();
        }

        isInternalError = false;
    }

    @Override
    public void close() {
        rateLimiter.threadList.remove(Thread.currentThread());

        if (isInternalError) rateLimiter.fatalError();
    }
}
