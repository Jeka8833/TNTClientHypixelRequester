package com.jeka8833.toprotocol.serializer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class ArrayOutputSerializer implements PacketOutputSerializer {
    protected final int limit;

    protected byte[] buf;
    protected int index;

    public ArrayOutputSerializer(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit for PacketOutputSerializer cannot be <= 0");

        buf = new byte[Math.min(16, limit)];
        this.limit = limit;
    }

    @Override
    public ArrayOutputSerializer writeByte(byte b) {
        ensureCapacity(index + 1);
        buf[index++] = b;

        return this;
    }

    @Override
    public ArrayOutputSerializer writeBoolean(boolean b) {
        ensureCapacity(index + 1);
        if (b) buf[index] = 1;

        index++;
        return this;
    }

    @Override
    public ArrayOutputSerializer writeShort(short s) {
        ensureCapacity(index + 2);
        buf[index++] = (byte) (s >> 8);
        buf[index++] = (byte) s;

        return this;
    }

    @Override
    public ArrayOutputSerializer writeInt(int i) {
        ensureCapacity(index + 4);
        buf[index++] = (byte) (i >> 24);
        buf[index++] = (byte) (i >> 16);
        buf[index++] = (byte) (i >> 8);
        buf[index++] = (byte) i;

        return this;
    }

    @Override
    public ArrayOutputSerializer writeLong(long l) {
        ensureCapacity(index + 8);
        buf[index++] = (byte) (l >> 56);
        buf[index++] = (byte) (l >> 48);
        buf[index++] = (byte) (l >> 40);
        buf[index++] = (byte) (l >> 32);
        buf[index++] = (byte) (l >> 24);
        buf[index++] = (byte) (l >> 16);
        buf[index++] = (byte) (l >> 8);
        buf[index++] = (byte) l;

        return this;
    }

    @Override
    public ArrayOutputSerializer writeBytes(byte[] bytes) {
        writeVarInt(bytes.length);

        ensureCapacity(index + bytes.length);
        System.arraycopy(bytes, 0, buf, index, bytes.length);
        index += bytes.length;

        return this;
    }

    @Override
    public ArrayOutputSerializer writeString(String s, int customLimit) {
        // Prevent byte array allocation if string is much longer than limit, +1 byte for varint
        if (s.length() > customLimit || s.length() >= limit - index) {
            throw new IndexOutOfBoundsException("String length exceeds the remaining buffer limit of " +
                    (limit - index) + " bytes.");
        }

        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeBytes(bytes, customLimit);

        return this;
    }

    @Override
    public ArrayOutputSerializer writeString(String s) {
        // Prevent byte array allocation if string is much longer than limit, +1 byte for varint
        if (s.length() >= limit - index) {
            throw new IndexOutOfBoundsException("String length exceeds the remaining buffer limit of " +
                    (limit - index) + " bytes.");
        }

        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        return writeBytes(bytes);
    }

    /**
     * Increases the capacity if necessary to ensure that it can hold
     * at least the number of elements specified by the minimum
     * capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    protected void ensureCapacity(int minCapacity) {
        if (minCapacity <= 0 || minCapacity > limit) {
            throw new IndexOutOfBoundsException("PacketOutputSerializer exceeded limit of " + limit);
        }

        if (minCapacity > buf.length) {
            grow(minCapacity);
        }
    }

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    protected void grow(int minCapacity) {
        int newCapacity = buf.length << 1;
        if (newCapacity <= 0 || newCapacity > limit) {
            newCapacity = limit;
        }

        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }

        buf = Arrays.copyOf(buf, newCapacity);
    }

    public byte[] array() {
        return Arrays.copyOf(buf, index);
    }
}
