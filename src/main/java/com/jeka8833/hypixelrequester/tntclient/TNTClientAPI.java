package com.jeka8833.hypixelrequester.tntclient;

import com.jeka8833.hypixelrequester.tntclient.packet.ClienboundPacket;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketRegistry;
import com.jeka8833.hypixelrequester.tntclient.packet.ServerboundPacket;
import com.jeka8833.hypixelrequester.tntclient.packet.clientbound.ClientboundAuth;
import com.jeka8833.hypixelrequester.tntclient.packet.clientbound.ClientboundHypixelStatsRequest;
import com.jeka8833.hypixelrequester.tntclient.packet.clientbound.ClientboundRequestAvailableCount;
import com.jeka8833.hypixelrequester.tntclient.packet.serverbound.ServerboundAuth;
import com.jeka8833.hypixelrequester.tntclient.packet.serverbound.ServerboundHypixelStatsResponse;
import com.jeka8833.hypixelrequester.tntclient.packet.serverbound.ServerboundResponseAvailableCount;
import com.jeka8833.toprotocol.serializer.ArrayInputSerializer;
import com.jeka8833.toprotocol.serializer.ArrayOutputSerializer;
import com.jeka8833.toprotocol.serializer.PacketInputSerializer;
import okhttp3.*;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class TNTClientAPI {
    private static final int MAX_PACKET_SIZE = 2 * 1024;

    private static final Logger LOGGER = LogManager.getLogger(TNTClientAPI.class);
    private static final PacketRegistry PACKET_REGISTRY = new PacketRegistry();

    static {
        PACKET_REGISTRY.define(14)
                .clientbound(ClientboundHypixelStatsRequest::new)
                .register();

        PACKET_REGISTRY.define(15)
                .clientbound(ClientboundRequestAvailableCount::new)
                .serverbound(ServerboundResponseAvailableCount.class)
                .register();

        PACKET_REGISTRY.define(17)
                .serverbound(ServerboundHypixelStatsResponse.class)
                .register();

        PACKET_REGISTRY.define(18)
                .clientbound(ClientboundHypixelStatsRequest::new)
                .register();

        PACKET_REGISTRY.define(255)
                .clientbound(ClientboundAuth::new)
                .serverbound(ServerboundAuth.class)
                .register();
    }

    private final Map<Class<? extends ClienboundPacket>, Collection<Consumer<ClienboundPacket>>> listenersMap =
            new ConcurrentHashMap<>();

    private final Request request;
    private final OkHttpClient client;
    private final UUID user;
    private final UUID password;
    private final long reconnectDelayNanos;
    private final ScheduledExecutorService executorService;

    private long lastConnectTime = 0;
    private @Nullable Future<?> authorisingFuture;
    private @Nullable Future<?> reconnectingFuture;
    private @Nullable WebSocket webSocket;
    private State state = State.CLOSED;

    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);

            state = State.AUTHORISING;

            if (authorisingFuture != null) authorisingFuture.cancel(true);

            authorisingFuture = executorService.schedule(() -> {
                if (state == State.AUTHORISING) doReconnect();
            }, reconnectDelayNanos, TimeUnit.NANOSECONDS);

            send(new ServerboundAuth(user, password));
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
            super.onMessage(webSocket, bytes);

            PacketInputSerializer serializer = new ArrayInputSerializer(bytes.toByteArray());
            byte packetId = serializer.readByte();

            ClienboundPacket packet = PACKET_REGISTRY.createPacket(packetId, serializer);
            if (packet == null) {
                LOGGER.warn("Unknown packet id: {}", packetId & 0xFF);
                return;
            }

            Collection<Consumer<ClienboundPacket>> listeners = listenersMap.get(packet.getClass());
            if (listeners != null) {
                for (Consumer<ClienboundPacket> listener : listeners) {
                    listener.accept(packet);
                }
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);

            state = State.RECONNECTING;

            LOGGER.error("Exception in WebSocket", t);

            doReconnect();
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);

            state = State.RECONNECTING;

            LOGGER.info("Server close WebSocket with code {}: {}", code, reason);

            doReconnect();
        }
    };

    public TNTClientAPI(Request request, OkHttpClient client, UUID user, UUID password, long reconnectDelayNanos,
                        ScheduledExecutorService executorService) {
        this.request = request;
        this.client = client;
        this.user = user;
        this.password = password;
        this.reconnectDelayNanos = reconnectDelayNanos;
        this.executorService = executorService;

        registerListener(ClientboundAuth.class, clientboundAuth -> {
            if (authorisingFuture != null) authorisingFuture.cancel(true);

            state = State.CONNECTED;

            LOGGER.info("TNTClient API Connected!");
        });
    }

    public synchronized void connect() {
        if (state != State.CLOSED) return;

        state = State.CONNECTING;
        doReconnect();
    }

    public synchronized void disconnect() {
        if (authorisingFuture != null) {
            authorisingFuture.cancel(true);

            authorisingFuture = null;
        }

        if (reconnectingFuture != null) {
            reconnectingFuture.cancel(true);

            reconnectingFuture = null;
        }

        if (webSocket != null) {
            webSocket.close(1000, null);

            webSocket = null;
        }

        state = State.CLOSED;
    }

    public <T extends ClienboundPacket> void registerListener(@NotNull Class<T> packetClass,
                                                              @NotNull Consumer<T> listener) {
        Collection<Consumer<ClienboundPacket>> list =
                listenersMap.computeIfAbsent(packetClass, k -> new CopyOnWriteArrayList<>());

        //noinspection unchecked
        list.add((Consumer<ClienboundPacket>) listener);
    }

    public void send(@NotNull ServerboundPacket packet, long maxTimeSend, TimeUnit unit) {
        if (!send(packet)) {
            long endTime = System.nanoTime() + unit.toNanos(maxTimeSend);

            executorService.scheduleWithFixedDelay(() -> {
                if (System.nanoTime() - endTime > 0) {
                    throw new CancellationException("Packet is not sent");
                }

                if (send(packet)) {
                    throw new CancellationException("Packet is sent");
                }
            }, 500, 500, TimeUnit.MILLISECONDS);
        }
    }

    public boolean send(@NotNull ServerboundPacket packet) throws RuntimeException {
        Byte identifier = PACKET_REGISTRY.getIdentifier(packet.getClass());
        if (identifier == null) {
            throw new UnsupportedOperationException("Packet not registered: " + packet.getClass().getName());
        }

        ArrayOutputSerializer stream = new ArrayOutputSerializer(MAX_PACKET_SIZE);
        stream.writeByte(identifier);
        packet.write(stream);

        WebSocket webSocket = this.webSocket;
        if (webSocket != null &&
                (state == State.CONNECTED || state == State.AUTHORISING && packet instanceof ServerboundAuth)) {
            return webSocket.send(ByteString.of(stream.array()));
        }
        return false;
    }

    private synchronized void doReconnect() {
        if (state == State.RECONNECTING || state == State.AUTHORISING || state == State.CONNECTING) {
            if (reconnectingFuture != null) reconnectingFuture.cancel(true);
            if (authorisingFuture != null) authorisingFuture.cancel(true);

            if (webSocket != null) {
                webSocket.close(1000, null);

                webSocket = null;
            }

            reconnectingFuture = executorService.schedule(() -> {
                lastConnectTime = System.nanoTime();

                LOGGER.info("TNTClient API Connecting...");

                webSocket = client.newWebSocket(request, webSocketListener);
            }, reconnectDelayNanos - (System.nanoTime() - lastConnectTime), TimeUnit.NANOSECONDS);
        }
    }

    private enum State {
        RECONNECTING, CLOSED, CONNECTING, AUTHORISING, CONNECTED
    }
}
