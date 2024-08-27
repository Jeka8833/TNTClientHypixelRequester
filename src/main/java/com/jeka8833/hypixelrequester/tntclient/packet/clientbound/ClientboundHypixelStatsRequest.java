package com.jeka8833.hypixelrequester.tntclient.packet.clientbound;

import com.jeka8833.hypixelrequester.tntclient.packet.ClienboundPacket;
import com.jeka8833.toprotocol.serializer.PacketInputSerializer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ClientboundHypixelStatsRequest implements ClienboundPacket {
    private final Set<UUID> players;

    public ClientboundHypixelStatsRequest(PacketInputSerializer serializer) {
        int size = serializer.readByte() & 0xFF;

        players = new HashSet<>(size, 1f);
        for (int i = 0; i < size; i++) {
            players.add(serializer.readUUID());
        }
    }

    public Set<UUID> getPlayers() {
        return players;
    }
}
