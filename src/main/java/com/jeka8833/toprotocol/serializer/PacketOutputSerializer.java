package com.jeka8833.toprotocol.serializer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.BiConsumer;

@FunctionalInterface
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public interface PacketOutputSerializer {
    PacketOutputSerializer writeByte(byte b);

    default PacketOutputSerializer writeBoolean(boolean b) {
        return writeByte(b ? (byte) 1 : (byte) 0);
    }

    default PacketOutputSerializer writeShort(short s) {
        writeByte((byte) (s >> 8));
        return writeByte((byte) s);
    }

    default PacketOutputSerializer writeInt(int i) {
        writeShort((short) (i >> 16));
        return writeShort((short) i);
    }

    default PacketOutputSerializer writeVarInt(int i) {
        while ((i & 0xFFFFFF80) != 0) {
            writeByte((byte) (i & 0x7F | 0x80));
            i >>>= 7;
        }

        return writeByte((byte) i);
    }

    default PacketOutputSerializer writeLong(long l) {
        writeInt((int) (l >> 32));
        return writeInt((int) l);
    }

    default PacketOutputSerializer writeFloat(float f) {
        return writeInt(Float.floatToIntBits(f));
    }

    default PacketOutputSerializer writeDouble(double d) {
        return writeLong(Double.doubleToLongBits(d));
    }

    default PacketOutputSerializer writeBytes(byte[] bytes, int customLimit) {
        if (bytes.length > customLimit) {
            throw new IndexOutOfBoundsException("Byte array length exceeds the limit of " + customLimit + " bytes.");
        }

        return writeBytes(bytes);
    }

    default PacketOutputSerializer writeBytes(byte[] bytes) {
        writeVarInt(bytes.length);

        for (byte aByte : bytes) {
            writeByte(aByte);
        }

        return this;
    }

    default PacketOutputSerializer writeString(String s, int customLimit) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        return writeBytes(bytes, customLimit);
    }

    default PacketOutputSerializer writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        return writeBytes(bytes);
    }

    default PacketOutputSerializer writeUUID(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        return writeLong(uuid.getLeastSignificantBits());
    }

    default PacketOutputSerializer writeOptionalDouble(OptionalDouble optionalDouble) {
        if (optionalDouble.isPresent()) {
            return writeBoolean(true)
                    .writeDouble(optionalDouble.getAsDouble());
        }

        return writeBoolean(false);
    }

    default PacketOutputSerializer writeOptionallyDouble(Double optionalDouble) {
        if (optionalDouble == null) return writeBoolean(false);

        return writeBoolean(true)
                .writeDouble(optionalDouble);
    }

    default <T> PacketOutputSerializer writeOptional(Optional<T> optional,
                                                     BiConsumer<PacketOutputSerializer, T> consumer) {
        if (optional.isPresent()) {
            writeBoolean(true);
            consumer.accept(this, optional.get());
        } else {
            writeBoolean(false);
        }

        return this;
    }

    default <T> PacketOutputSerializer writeOptionally(T optional, BiConsumer<PacketOutputSerializer, T> consumer) {
        if (optional == null) return writeBoolean(false);

        writeBoolean(true);
        consumer.accept(this, optional);
        return this;
    }
}
