package com.jeka8833.toprotocol.serializer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Function;

@FunctionalInterface
public interface PacketInputSerializer {
    byte readByte();

    default boolean readBoolean() {
        return readByte() != 0;
    }

    default short readShort() {
        return (short) ((readByte() & 0xFF) << 8 | (readByte() & 0xFF));
    }

    default int readInt() {
        return (readShort() & 0xFFFF) << 16 | (readShort() & 0xFFFF);
    }

    default int readVarInt() {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new IndexOutOfBoundsException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return i;
    }

    default long readLong() {
        return ((long) readInt() << 32) | (readInt() & 0xFFFFFFFFL);
    }

    default float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    default double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    default byte[] readBytes(int customLimit) {
        int length = readVarInt();
        if (length > customLimit) throw new IndexOutOfBoundsException("Data length is too long");

        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = readByte();
        }
        return data;
    }

    default byte[] readBytes() {
        int length = readVarInt();

        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = readByte();
        }
        return data;
    }

    default String readString(int customLimit) {
        return new String(readBytes(customLimit), StandardCharsets.UTF_8);
    }

    default String readString() {
        return new String(readBytes(), StandardCharsets.UTF_8);
    }

    default UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    default Double readOptionallyDouble() {
        return readBoolean() ? readDouble() : null;
    }

    default OptionalDouble readOptionalDouble() {
        return readBoolean() ? OptionalDouble.of(readDouble()) : OptionalDouble.empty();
    }

    default <T> Optional<T> readOptional(Function<PacketInputSerializer, T> function) {
        return readBoolean() ? Optional.of(function.apply(this)) : Optional.empty();
    }

    default <T> T readOptionally(Function<PacketInputSerializer, T> function) {
        return readBoolean() ? function.apply(this) : null;
    }

    default void skip(int bytes) {
        for (int i = 0; i < bytes; i++) {
            readByte();
        }
    }
}
