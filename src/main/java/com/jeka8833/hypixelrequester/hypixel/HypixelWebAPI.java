package com.jeka8833.hypixelrequester.hypixel;

import com.alibaba.fastjson2.JSON;
import com.jeka8833.hypixelrequester.hypixel.ratelimiter.HypixelRateLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class HypixelWebAPI {
    private static final String PLAYER_ENDPOINT = "https://api.hypixel.net/v2/player?uuid=";

    private static final Logger LOGGER = LogManager.getLogger(HypixelWebAPI.class);

    private final String key;
    private final HypixelRateLimiter rateLimiter;
    private final OkHttpClient client;
    private final ExecutorService executorService;

    private boolean isAuthenticated = false;

    public HypixelWebAPI(UUID key, HypixelRateLimiter rateLimiter, OkHttpClient client,
                         ExecutorService executorService) {
        this.key = key.toString();
        this.rateLimiter = rateLimiter;
        this.client = client;
        this.executorService = executorService;
    }

    @Blocking
    public Optional<Boolean> checkAuthentication() throws InterruptedException, IOException {
        try (HypixelRateLimiter.Status status = rateLimiter.newRequest()) {
            Request request = new Request.Builder()
                    .url(PLAYER_ENDPOINT + "6bd6e833-a80a-430e-9029-4786368811f9")
                    .header("API-Key", key)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                status.connectionInfo(response.code(),
                        response.header("RateLimit-Remaining"),
                        response.header("RateLimit-Reset"));

                LOGGER.info("Hypixel API response: {}", response.code());
                LOGGER.info("Hypixel API response body: {}", response.headers());

                if (response.isSuccessful()) {
                    return Optional.of(isAuthenticated = true);
                }
                if (response.code() == 403) {
                    return Optional.of(isAuthenticated = false);
                }
            }
        }

        isAuthenticated = true;

        return Optional.empty();
    }

    public void request(UUID player, Consumer<@Nullable HypixelJSONStructure> listener) {
        if (!isAuthenticated) {
            listener.accept(null);

            return;
        }

        executorService.execute(() -> {
            HypixelJSONStructure structure = null;
            try (HypixelRateLimiter.Status status = rateLimiter.newRequest()) {
                Request request = new Request.Builder()
                        .url(PLAYER_ENDPOINT + player)
                        .header("API-Key", key)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    status.connectionInfo(response.code(),
                            response.header("RateLimit-Remaining"),
                            response.header("RateLimit-Reset"));

                    if (response.code() == 403) {
                        isAuthenticated = false;
                    }

                    if (!response.isSuccessful())
                        throw new IOException("Hypixel API request returned: " + response.code());

                    try (ResponseBody body = response.body(); InputStream reader = body.byteStream()) {
                        structure = JSON.parseObject(reader, HypixelJSONStructure.class);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Hypixel API request failed for player, {}", player, e);
            } finally {
                listener.accept(structure);
            }
        });
    }

    public void request(UUID player, Consumer<@Nullable HypixelJSONStructure> listener, int retryCount) {
        if (!isAuthenticated) {
            listener.accept(null);

            return;
        }

        executorService.execute(() -> {
            HypixelJSONStructure structure = null;
            try {
                for (int i = 0; i < retryCount; i++) {
                    try (HypixelRateLimiter.Status status = rateLimiter.newRequest()) {
                        Request request = new Request.Builder()
                                .url(PLAYER_ENDPOINT + player)
                                .header("API-Key", key)
                                .build();

                        try (Response response = client.newCall(request).execute()) {
                            status.connectionInfo(response.code(),
                                    response.header("RateLimit-Remaining"),
                                    response.header("RateLimit-Reset"));

                            if (response.code() == 403) {
                                isAuthenticated = false;
                                return;
                            }

                            if (response.isSuccessful()) {
                                try (ResponseBody body = response.body(); InputStream reader = body.byteStream()) {
                                    structure = JSON.parseObject(reader, HypixelJSONStructure.class);
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Hypixel API request failed for player, {}", player, e);
                    }
                }

                LOGGER.warn("Hypixel API request failed for player, {}", player);
            } finally {
                listener.accept(structure);
            }
        });
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }
}
