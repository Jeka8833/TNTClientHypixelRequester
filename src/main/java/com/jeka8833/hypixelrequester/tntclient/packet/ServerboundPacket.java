package com.jeka8833.hypixelrequester.tntclient.packet;

import com.jeka8833.toprotocol.serializer.PacketOutputSerializer;

public interface ServerboundPacket {
    void write(PacketOutputSerializer serializer);
}
