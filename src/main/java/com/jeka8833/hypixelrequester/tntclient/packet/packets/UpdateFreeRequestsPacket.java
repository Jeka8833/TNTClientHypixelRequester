package com.jeka8833.hypixelrequester.tntclient.packet.packets;

import com.jeka8833.hypixelrequester.TNTServer;
import com.jeka8833.hypixelrequester.tntclient.packet.Packet;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketInputStream;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketOutputStream;
import okhttp3.WebSocket;

import java.io.IOException;

public class UpdateFreeRequestsPacket implements Packet {

    private final int free;

    @SuppressWarnings("unused")
    public UpdateFreeRequestsPacket() {
        this(0);
    }

    public UpdateFreeRequestsPacket(int free) {
        this.free = free;
    }

    @Override
    public void write(PacketOutputStream stream) throws IOException {
        stream.writeInt(free);
    }

    @Override
    public void read(PacketInputStream stream) {
    }

    @Override
    public void clientProcess(WebSocket socket) {
        TNTServer.Companion.forceSendFreePacket();
    }
}
