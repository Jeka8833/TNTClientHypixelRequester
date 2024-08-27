package com.jeka8833.hypixelrequester.tntclient.packet;

import com.jeka8833.toprotocol.serializer.PacketInputSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class PacketRegistry {
    private final Map<Byte, Function<PacketInputSerializer, ? extends ClienboundPacket>> identifierToConstructor = new HashMap<>();
    private final Map<Class<? extends ServerboundPacket>, Byte> classToIdentifier = new HashMap<>();

    private void register(byte identifier,
                          @Nullable Function<PacketInputSerializer, ? extends ClienboundPacket> clientPacketFactory,
                          @Nullable Class<? extends ServerboundPacket> serverboundClazz) {
        if (clientPacketFactory != null) {
            identifierToConstructor.put(identifier, clientPacketFactory);
        }
        if (serverboundClazz != null) {
            classToIdentifier.put(serverboundClazz, identifier);
        }
    }

    public RegistrationBuilder define(int identifier) {
        return new RegistrationBuilder(this, (byte) identifier);
    }

    @Nullable
    public ClienboundPacket createPacket(byte identifier, PacketInputSerializer serializer) {
        Function<PacketInputSerializer, ? extends ClienboundPacket> registration = identifierToConstructor.get(identifier);
        if (registration == null) return null;

        return registration.apply(serializer);
    }

    @Nullable
    public Byte getIdentifier(Class<? extends ServerboundPacket> clazz) {
        return classToIdentifier.get(clazz);
    }

    public static final class RegistrationBuilder {
        private final PacketRegistry registry;
        private final byte identifier;

        private @Nullable Class<? extends ServerboundPacket> serverboundClazz;
        private @Nullable Function<PacketInputSerializer, ? extends ClienboundPacket> clientPacketFactory;

        private RegistrationBuilder(PacketRegistry registry, byte identifier) {
            this.registry = registry;
            this.identifier = identifier;
        }

        public RegistrationBuilder clientbound(Function<PacketInputSerializer, ? extends ClienboundPacket> serverPacketFactory) {
            this.clientPacketFactory = serverPacketFactory;

            return this;
        }

        public RegistrationBuilder serverbound(Class<? extends ServerboundPacket> serverboundClazz) {
            this.serverboundClazz = serverboundClazz;

            return this;
        }

        public void register() {
            registry.register(identifier, clientPacketFactory, serverboundClazz);
        }
    }
}
