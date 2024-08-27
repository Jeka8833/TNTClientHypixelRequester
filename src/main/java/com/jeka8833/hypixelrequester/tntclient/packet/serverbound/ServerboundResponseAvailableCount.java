package com.jeka8833.hypixelrequester.tntclient.packet.serverbound;

import com.jeka8833.hypixelrequester.tntclient.packet.ServerboundPacket;
import com.jeka8833.toprotocol.serializer.PacketOutputSerializer;

public final class ServerboundResponseAvailableCount implements ServerboundPacket {
    private final int available;

    public ServerboundResponseAvailableCount(int available) {
        this.available = available;
    }

    @Override
    public void write(PacketOutputSerializer serializer) {
        serializer.writeInt(available);
    }
}
