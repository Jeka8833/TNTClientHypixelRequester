package com.jeka8833.hypixelrequester.tntclient.packet.packets;

import com.jeka8833.hypixelrequester.tntclient.packet.Packet;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketInputStream;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketOutputStream;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

public class AuthPacket implements Packet {

    private final @NotNull UUID user;
    private final @NotNull UUID password;

    public AuthPacket(@NotNull UUID user, @NotNull UUID password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeUUID(user);
        stream.writeUUID(password);
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void clientProcess(WebSocket socket) {
    }
}
