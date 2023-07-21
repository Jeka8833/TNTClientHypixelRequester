package com.jeka8833.hypixelrequester.util;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.time.Duration;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class HypixelRateLimiter {

    public static final int ONE_GROUP = Integer.MAX_VALUE;
    public static final int INFINITY_GROUPS = 1;

    private @Range(from = 1, to = Integer.MAX_VALUE) int refreshDuration;
    private @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize;
    private @Range(from = 0, to = Long.MAX_VALUE) long sleepAfterFail;
    private long nextRefresh = 0;

    private @Nullable ServerResponse response;

    public HypixelRateLimiter(@NotNull Duration refreshDuration) {
        this(refreshDuration, INFINITY_GROUPS);
    }

    public HypixelRateLimiter(@NotNull Duration refreshDuration,
                              @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize) {
        this(refreshDuration, groupSize, Duration.ofSeconds(10));
    }

    public HypixelRateLimiter(@NotNull Duration refreshDuration,
                              @Range(from = INFINITY_GROUPS, to = ONE_GROUP) int groupSize,
                              @NotNull Duration sleepAfterFail) {
        setRefreshDuration(refreshDuration);
        setGroupSize(groupSize);
        setSleepAfterFail(sleepAfterFail);
    }

    @Blocking
    public void receiveAndLock(@Nullable ServerResponse response) throws InterruptedException {
        if (response == null) {
            if (sleepAfterFail > 0) Thread.sleep(sleepAfterFail);
            return;
        }

        this.response = response;

        long restTime = response.getReset() + 1000L;
        if (restTime < nextRefresh || restTime - nextRefresh > refreshDuration / 4) {
            nextRefresh = restTime;
        }

        // 2000 - 2 seconds makes up for the mathematical rounding.
        if (response.getRemaining() >= (2000 * response.getLimit()) / refreshDuration) {
            int groupRemaining = response.getLimit() -
                    (response.getLimit() - response.getRemaining()) / groupSize * groupSize;   // Math round
            int sleepTime = getToResetUnchecked() - (refreshDuration * groupRemaining) / response.getLimit();

            if (sleepTime > 0) Thread.sleep(sleepTime);
        } else if (response.getRemaining() == 0) {
            int sleepTime = getToResetUnchecked();

            if (sleepTime > 0) Thread.sleep(sleepTime);
        }
    }

    public void setRefreshDuration(@NotNull Duration refreshDuration) {
        long time = refreshDuration.toMillis();
        if (time <= 0) throw new IllegalArgumentException("RefreshDuration is <= 0");

        this.refreshDuration = Math.toIntExact(time);
    }

    public void setSleepAfterFail(@NotNull Duration sleepAfterFail) {
        if (sleepAfterFail.isNegative()) throw new IllegalArgumentException("Sleep duration is negative");

        this.sleepAfterFail = sleepAfterFail.toMillis();
    }

    public void setGroupSize(int groupSize) {
        if (groupSize < INFINITY_GROUPS) throw new IllegalArgumentException("Group size must be > 0");

        this.groupSize = groupSize;
    }

    public OptionalInt getLimit() {
        if (response == null) return OptionalInt.empty();

        return OptionalInt.of(response.getLimit());
    }

    public OptionalInt getUsed() {
        if (response == null) return OptionalInt.empty();

        return OptionalInt.of(response.getLimit() - response.getRemaining());
    }

    public OptionalInt getToReset() {
        if (response == null) return OptionalInt.empty();

        return OptionalInt.of(Math.max(0, getToResetUnchecked()));
    }

    public OptionalInt getFreeAtMoment() {
        if (response == null) return OptionalInt.empty();

        if (getToResetUnchecked() < 0) return OptionalInt.of(Math.min(groupSize, response.getLimit()));

        long mustCount = ((System.currentTimeMillis() -
                (response.getReset() - refreshDuration)) * response.getLimit()) / refreshDuration;
        long roundCount = response.getLimit() - (response.getLimit() - mustCount) / groupSize * groupSize;
        return OptionalInt.of(Math.toIntExact(roundCount - (response.getLimit() - response.getRemaining())));
    }

    private int getToResetUnchecked() {
        return Math.toIntExact(nextRefresh - System.currentTimeMillis());
    }

    public boolean isFail() {
        return response == null;
    }

    public static class ServerResponse {
        private final @Range(from = 0, to = Long.MAX_VALUE) long reset;
        private final @Range(from = 1, to = Integer.MAX_VALUE) int limit;
        private final @Range(from = 0, to = Integer.MAX_VALUE) int remaining;

        public ServerResponse(@Range(from = 0, to = Integer.MAX_VALUE) int reset,
                              @Range(from = 1, to = Integer.MAX_VALUE) int limit,
                              @Range(from = 0, to = Integer.MAX_VALUE) int remaining) {
            this.reset = System.currentTimeMillis() + reset * 1000L;
            this.limit = limit;
            this.remaining = remaining;
        }

        @Nullable
        public static ServerResponse create(@Nullable String reset, @Nullable String limit,
                                            @Nullable String remaining) {
            try {
                //noinspection DataFlowIssue
                return new ServerResponse(Integer.parseInt(reset), Integer.parseInt(limit), Integer.parseInt(remaining));
            } catch (NumberFormatException ignore) {
            }

            return null;
        }

        @Nullable
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public static ServerResponse create(@Nullable OptionalLong reset, @Nullable OptionalLong limit,
                                            @Nullable OptionalLong remaining) {
            //noinspection OptionalAssignedToNull,SimplifyOptionalCallChains
            if (reset == null || limit == null || remaining == null ||
                    !reset.isPresent() || !limit.isPresent() || !remaining.isPresent()) return null;

            return new ServerResponse((int) reset.getAsLong(), (int) limit.getAsLong(), (int) remaining.getAsLong());
        }

        @Range(from = 0, to = Integer.MAX_VALUE)
        public int getLimit() {
            return limit;
        }

        @Range(from = 0, to = Integer.MAX_VALUE)
        public int getRemaining() {
            return remaining;
        }

        public long getReset() {
            return reset;
        }

        @Override
        public String toString() {
            return "ServerResponse{" +
                    "limit=" + limit +
                    ", remaining=" + remaining +
                    ", reset=" + reset +
                    '}';
        }
    }
}