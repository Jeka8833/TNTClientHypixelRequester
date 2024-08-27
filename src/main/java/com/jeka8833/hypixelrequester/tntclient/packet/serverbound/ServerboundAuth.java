package com.jeka8833.hypixelrequester.tntclient.packet.serverbound;

import com.jeka8833.hypixelrequester.tntclient.packet.ServerboundPacket;
import com.jeka8833.toprotocol.serializer.PacketOutputSerializer;

import java.util.UUID;

public final class ServerboundAuth implements ServerboundPacket {
    private final UUID user;
    private final UUID password;

    public ServerboundAuth(UUID user, UUID password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public void write(PacketOutputSerializer serializer) {
        serializer.writeUUID(user)
                .writeUUID(password);
    }
}
