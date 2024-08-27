package com.jeka8833.toprotocol.serializer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamInputSerializer implements PacketInputSerializer {
    protected final DataInputStream stream;

    public StreamInputSerializer(InputStream stream) {
        this.stream = new DataInputStream(stream);
    }

    @Override
    public byte readByte() {
        try {
            return stream.readByte();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean readBoolean() {
        try {
            return stream.readBoolean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public short readShort() {
        try {
            return stream.readShort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int readInt() {
        try {
            return stream.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long readLong() {
        try {
            return stream.readLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float readFloat() {
        try {
            return stream.readFloat();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double readDouble() {
        try {
            return stream.readDouble();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] readBytes(int customLimit) {
        int length = readVarInt();
        if (length > customLimit) throw new IndexOutOfBoundsException("Data length is too long");

        byte[] data = new byte[length];
        try {
            stream.readFully(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return data;
    }

    @Override
    public byte[] readBytes() {
        int length = readVarInt();

        byte[] data = new byte[length];
        try {
            stream.readFully(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return data;
    }

    @Override
    public void skip(int bytes) {
        try {
            stream.skipBytes(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
