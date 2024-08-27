package com.jeka8833.toprotocol.serializer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The format should be similar to the Netty and Hypixel encoding format.
 * */
public class ArrayInputSerializer implements PacketInputSerializer {
    protected final byte[] data;

    protected int index = 0;

    public ArrayInputSerializer(byte[] data) {
        this.data = data;
    }

    @Override
    public byte readByte() {
        return data[index++];
    }

    @Override
    public boolean readBoolean() {
        return data[index++] != 0;
    }

    @Override
    public short readShort() {
        return (short) ((data[index++] & 0xFF) << 8 | (data[index++] & 0xFF));
    }

    @Override
    public int readInt() {
        return (data[index++] & 0xFF) << 24 |
                (data[index++] & 0xFF) << 16 |
                (data[index++] & 0xFF) << 8 |
                (data[index++] & 0xFF);
    }

    @Override
    public int readVarInt() {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = data[index++];
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new IndexOutOfBoundsException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return i;
    }

    @Override
    public long readLong() {
        return (long) (data[index++] & 0xFF) << 56 |
                (long) (data[index++] & 0xFF) << 48 |
                (long) (data[index++] & 0xFF) << 40 |
                (long) (data[index++] & 0xFF) << 32 |
                (long) (data[index++] & 0xFF) << 24 |
                (data[index++] & 0xFF) << 16 |
                (data[index++] & 0xFF) << 8 |
                (data[index++] & 0xFF);
    }

    @Override
    public byte[] readBytes(int customLimit) {
        int length = readVarInt();
        if (length > customLimit) throw new IndexOutOfBoundsException("Data length is too long");

        int end = index + length;
        if (end > data.length) throw new IndexOutOfBoundsException("Not enough data to read");

        return Arrays.copyOfRange(data, index, index = end);
    }

    @Override
    public byte[] readBytes() {
        int length = readVarInt();

        int end = index + length;
        if (end > data.length) throw new IndexOutOfBoundsException("Not enough data to read");

        return Arrays.copyOfRange(data, index, index = end);
    }

    @Override
    public String readString(int customLimit) {
        int length = readVarInt();
        if (length > customLimit) throw new IndexOutOfBoundsException("Data length is too long");

        String str = new String(data, index, length, StandardCharsets.UTF_8);
        index += length;
        return str;
    }

    @Override
    public String readString() {
        int length = readVarInt();
        String str = new String(data, index, length, StandardCharsets.UTF_8);
        index += length;
        return str;
    }

    @Override
    public void skip(int bytes) {
        if (index + bytes > data.length) throw new IndexOutOfBoundsException("Not enough data to skip");

        index += bytes;
    }
}
