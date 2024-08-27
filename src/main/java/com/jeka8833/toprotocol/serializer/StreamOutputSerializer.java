package com.jeka8833.toprotocol.serializer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StreamOutputSerializer implements PacketOutputSerializer {
    protected final DataOutputStream stream;
    protected final int limit;

    public StreamOutputSerializer(OutputStream stream, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit for StreamOutputSerializer cannot be <= 0");

        this.stream = new DataOutputStream(stream);
        this.limit = limit;
    }

    @Override
    public StreamOutputSerializer writeByte(byte b) {
        if (stream.size() + 1 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeByte(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeBoolean(boolean b) {
        if (stream.size() + 1 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeBoolean(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeShort(short s) {
        if (stream.size() + 2 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeShort(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeInt(int i) {
        if (stream.size() + 4 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeInt(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeLong(long l) {
        if (stream.size() + 8 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeLong(l);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeFloat(float f) {
        if (stream.size() + 4 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeFloat(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeDouble(double d) {
        if (stream.size() + 8 > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.writeDouble(d);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeBytes(byte[] bytes) {
        writeVarInt(bytes.length);

        if (stream.size() + bytes.length > limit) {
            throw new IndexOutOfBoundsException("StreamOutputSerializer exceeded limit of " + limit);
        }

        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public StreamOutputSerializer writeString(String s, int customLimit) {
        // Prevent byte array allocation if string is much longer than limit, +1 byte for varint
        if (s.length() > customLimit || s.length() >= limit - stream.size()) {
            throw new IndexOutOfBoundsException("String length exceeds the remaining buffer limit of " +
                    (limit - stream.size()) + " bytes.");
        }

        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeBytes(bytes, customLimit);

        return this;
    }

    @Override
    public StreamOutputSerializer writeString(String s) {
        // Prevent byte array allocation if string is much longer than limit, +1 byte for varint
        if (s.length() >= limit - stream.size()) {
            throw new IndexOutOfBoundsException("String length exceeds the remaining buffer limit of " +
                    (limit - stream.size()) + " bytes.");
        }

        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        return writeBytes(bytes);
    }
}
