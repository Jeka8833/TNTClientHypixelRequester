package com.jeka8833.hypixelrequester;

import com.jeka8833.hypixelrequester.hypixel.HypixelWebAPI;
import com.jeka8833.hypixelrequester.hypixel.ratelimiter.HypixelRateLimiter;
import com.jeka8833.hypixelrequester.hypixel.ratelimiter.strategy.RequesterStrategyRefill;
import com.jeka8833.hypixelrequester.tntclient.TNTClientAPI;
import com.jeka8833.hypixelrequester.tntclient.packet.clientbound.ClientboundAuth;
import com.jeka8833.hypixelrequester.tntclient.packet.clientbound.ClientboundHypixelStatsRequest;
import com.jeka8833.hypixelrequester.tntclient.packet.clientbound.ClientboundRequestAvailableCount;
import com.jeka8833.hypixelrequester.tntclient.packet.serverbound.ServerboundHypixelStatsResponse;
import com.jeka8833.hypixelrequester.tntclient.packet.serverbound.ServerboundResponseAvailableCount;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RequesterManager {
    private static final Logger LOGGER = LogManager.getLogger(RequesterManager.class);

    private final Main config;
    private final TNTClientAPI tntClientAPI;
    private final HypixelWebAPI hypixelWebAPI;
    private final HypixelRateLimiter rateLimiter;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public RequesterManager(Main config) {
        this.config = config;

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(5, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(config.tntServerIP).build();

        this.tntClientAPI = new TNTClientAPI(request, client, config.botName, config.botPassword,
                config.reconnectDelay.toNanos(), scheduledExecutorService);

        this.rateLimiter = new HypixelRateLimiter(config.failSleep.toNanos(), config.delayBetweenCalls.toNanos(),
                new RequesterStrategyRefill(config.maxRequests, config.threads), scheduledExecutorService);

        ExecutorService executorService = Executors.newFixedThreadPool(config.threads);
        this.hypixelWebAPI = new HypixelWebAPI(config.hypixelKey, rateLimiter, client, executorService);
    }

    public void start() throws IOException, InterruptedException {
        setupAvailableUpdater();

        tntClientAPI.registerListener(ClientboundHypixelStatsRequest.class, clientboundHypixelStatsRequest -> {
            for (UUID player : clientboundHypixelStatsRequest.getPlayers()) {
                hypixelWebAPI.request(player, hypixelJSONStructure -> {
                    tntClientAPI.send(new ServerboundHypixelStatsResponse(
                            Collections.singletonMap(player, hypixelJSONStructure)), 10, TimeUnit.SECONDS);

                    LOGGER.info("Player {} stats sent.", player);
                }, config.requestRetry);
            }
        });


        tntClientAPI.connect();

        Optional<Boolean> hypixelKeyStatus = hypixelWebAPI.checkAuthentication();
        if (hypixelKeyStatus.isEmpty()) {
            LOGGER.warn("Failed to check Hypixel API key status.");
        } else {
            if (hypixelKeyStatus.get()) {
                LOGGER.info("Hypixel API key is valid.");
            } else {
                LOGGER.warn("Hypixel API key is invalid.");
            }
        }

        LOGGER.info("HypixelRequester started.");
    }

    private void setupAvailableUpdater() {
        long sendIntervalNanos = config.sendAvailableCountInterval.toNanos();

        tntClientAPI.registerListener(ClientboundRequestAvailableCount.class,
                clientboundRequestAvailableCount -> sendAvailableCount());
        tntClientAPI.registerListener(ClientboundAuth.class,
                clientboundAuth -> sendAvailableCount());

        scheduledExecutorService.scheduleWithFixedDelay(this::sendAvailableCount,
                sendIntervalNanos, sendIntervalNanos, TimeUnit.NANOSECONDS);
    }

    private void sendAvailableCount() {
        int availableCount = hypixelWebAPI.isAuthenticated() ? rateLimiter.getRemaining() : 0;

        tntClientAPI.send(new ServerboundResponseAvailableCount(availableCount));
    }
}
