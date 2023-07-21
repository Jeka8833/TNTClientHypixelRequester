package com.jeka8833.hypixelrequester.tntclient.packet.packets;

import com.jeka8833.hypixelrequester.HypixelPlayerStorage;
import com.jeka8833.hypixelrequester.tntclient.packet.Packet;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketInputStream;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketOutputStream;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class ReceiveHypixelPlayerPacket implements Packet {

    public static final int RECEIVE_FAIL = 0;
    public static final int RECEIVE_GOOD = 1;
    public static final int RECEIVE_GOOD_NOTHING = 2;

    private @Nullable Collection<ReceivePlayer> userList;


    public ReceiveHypixelPlayerPacket() {
    }

    public ReceiveHypixelPlayerPacket(@NotNull Collection<ReceivePlayer> userList) {
        this.userList = userList;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        if (userList == null || userList.isEmpty()) throw new NullPointerException("User list is empty");

        stream.writeByte(userList.size());
        for (ReceivePlayer player : userList) { // 105 byte one player
            stream.writeByte(player.status);
            stream.writeUUID(player.player);
            if ((player.status & RECEIVE_GOOD) == RECEIVE_GOOD) {
                player.storage.writeStream(stream);
            }
        }
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void clientProcess(WebSocket socket) {
    }

    public static class ReceivePlayer {
        public final UUID player;
        public final int status;
        public final HypixelPlayerStorage storage;

        public ReceivePlayer(UUID player, int status, HypixelPlayerStorage storage) {
            this.player = player;
            this.status = status;
            this.storage = storage;
        }
    }
}
