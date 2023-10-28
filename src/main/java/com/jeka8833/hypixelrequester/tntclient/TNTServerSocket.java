package com.jeka8833.hypixelrequester.tntclient;

import com.jeka8833.hypixelrequester.Main;
import com.jeka8833.hypixelrequester.tntclient.packet.Packet;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketInputStream;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketOutputStream;
import com.jeka8833.hypixelrequester.tntclient.packet.packets.AuthPacket;
import com.jeka8833.hypixelrequester.tntclient.packet.packets.ReceiveHypixelPlayerPacket;
import com.jeka8833.hypixelrequester.tntclient.packet.packets.RequestHypixelPlayerPacket;
import com.jeka8833.hypixelrequester.tntclient.packet.packets.UpdateFreeRequestsPacket;
import com.jeka8833.hypixelrequester.util.BiMap;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TNTServerSocket extends WebSocketListener {
    private static final Logger logger = LogManager.getLogger(TNTServerSocket.class);

    public static volatile boolean isConnected = false;

    private final @NotNull UUID user;
    private final @NotNull UUID password;

    public TNTServerSocket(@NotNull UUID user, @NotNull UUID password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        send(webSocket, new AuthPacket(user, password));
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        try (PacketInputStream stream = new PacketInputStream(bytes.toByteArray())) {
            stream.packet.read(stream);
            stream.packet.clientProcess(webSocket);
        } catch (Exception e) {
            logger.error("onMessage error: ", e);
        }
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        webSocket.close(1000, null);

        isConnected = false;

        logger.warn("Server close stream: " + code + " -> " + reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
        webSocket.close(1000, null);

        isConnected = false;

        logger.error("Force close: ", t);
    }

    @Blocking
    public static void send(@NotNull WebSocket webSocket, @NotNull Packet packet) {
        try (PacketOutputStream stream = new PacketOutputStream(packet.getClass())) {
            packet.write(stream);
            webSocket.send(stream.getByteString());
        } catch (Exception e) {
            logger.error("Fail send: ", e);
        }
    }

    public static final BiMap<Byte, Class<? extends Packet>> registeredPackets = new BiMap<>();

    static {
        registeredPackets.put((byte) 255, AuthPacket.class);
        registeredPackets.put((byte) 13, ReceiveHypixelPlayerPacket.class);
        registeredPackets.put((byte) 14, RequestHypixelPlayerPacket.class);
        registeredPackets.put((byte) 15, UpdateFreeRequestsPacket.class);
    }

    public static boolean isConnect() {
        return isConnected;
    }

    public static WebSocket connect(String serverIP, @NotNull UUID user, @NotNull UUID password) {
        logger.info("Attempting to connect to: " + serverIP);
        Request request = new Request.Builder().url(serverIP).build();
        return Main.Companion.getClient()
                .newWebSocket(request, new TNTServerSocket(user, password));
    }
}
