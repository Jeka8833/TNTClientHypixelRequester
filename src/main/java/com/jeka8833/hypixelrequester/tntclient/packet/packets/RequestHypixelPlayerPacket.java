package com.jeka8833.hypixelrequester.tntclient.packet.packets;

import com.jeka8833.hypixelrequester.TNTServer;
import com.jeka8833.hypixelrequester.tntclient.packet.Packet;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketInputStream;
import com.jeka8833.hypixelrequester.tntclient.packet.PacketOutputStream;
import okhttp3.WebSocket;

import java.io.IOException;

public class RequestHypixelPlayerPacket implements Packet {

    public RequestHypixelPlayerPacket() {
    }

    @Override
    public void write(PacketOutputStream stream) {
    }

    @Override
    public void read(PacketInputStream stream) throws IOException {
        int size = stream.readUnsignedByte();
        for (int i = 0; i < size; i++) {
            TNTServer.Companion.request(stream.readUUID());
        }
    }

    @Override
    public void clientProcess(WebSocket socket) {
    }
}
