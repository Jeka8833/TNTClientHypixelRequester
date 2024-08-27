package com.jeka8833.hypixelrequester.tntclient.packet.serverbound;

import com.jeka8833.hypixelrequester.hypixel.HypixelJSONStructure;
import com.jeka8833.hypixelrequester.tntclient.packet.ServerboundPacket;
import com.jeka8833.toprotocol.serializer.PacketOutputSerializer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ServerboundHypixelStatsResponse implements ServerboundPacket {
    private static final byte RECEIVE_FAIL = 0;
    private static final byte RECEIVE_GOOD = 1;
    private static final byte RECEIVE_GOOD_NOTHING = 2;

    private final Map<UUID, HypixelJSONStructure> usersData;

    public ServerboundHypixelStatsResponse(Map<UUID, HypixelJSONStructure> usersData) {
        this.usersData = usersData;
    }

    @Override
    public void write(PacketOutputSerializer serializer) {
        serializer.writeByte((byte) usersData.size());

        for (Map.Entry<UUID, HypixelJSONStructure> player : usersData.entrySet()) {
            UUID playerUUID = player.getKey();
            HypixelJSONStructure playerData = player.getValue();

            final byte statusCode;
            if (playerData == null) {
                statusCode = RECEIVE_FAIL;
            } else if (playerData.isEmpty()) {
                statusCode = RECEIVE_GOOD_NOTHING;
            } else {
                statusCode = RECEIVE_GOOD;
            }

            serializer.writeByte(statusCode)
                    .writeUUID(playerUUID);
            if (statusCode == RECEIVE_GOOD) {
                write(playerData, serializer);
            }
        }
    }

    private static void write(HypixelJSONStructure data, PacketOutputSerializer serializer) {
        Optional<HypixelJSONStructure.Player> playerOptional = data.player;
        Optional<HypixelJSONStructure.TNTGames> tntGamesOptional = playerOptional.flatMap(player ->
                player.stats.flatMap(stats -> stats.tntGames));
        Optional<HypixelJSONStructure.Duels> duelsOptional = playerOptional.flatMap(player ->
                player.stats.flatMap(stats -> stats.duels));

        serializer.writeLong(playerOptional.flatMap(player -> player.networkExp).orElse(-1L))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.coins).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.winStreak).orElse(-1))

                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.tntRunWins).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.tntRunLosses).orElse(-1))

                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.pvpRunWins).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.pvpRunLosses).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.pvpRunKills).orElse(-1))

                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.bowSpleefWins).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.bowSpleefLosses).orElse(-1))

                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.tntTagWins).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.tntTagLosses).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.tntTagKills).orElse(-1))

                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.wizardsWins).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.wizardsLosses).orElse(-1))
                .writeInt(tntGamesOptional.flatMap(tntGames -> tntGames.wizardsKills).orElse(-1))

                .writeInt(duelsOptional.flatMap(duels -> duels.bowSpleefWins).orElse(-1))
                .writeInt(duelsOptional.flatMap(duels -> duels.bowSpleefLosses).orElse(-1))
                .writeInt(duelsOptional.flatMap(duels -> duels.bowSpleefWinStreak).orElse(-1));
    }
}