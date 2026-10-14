package com.tacz.guns.legacy.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.function.Supplier;

/**
 * 1.20.1 {@code PacketDistributor}: who a server-to-client {@link SimpleChannel} message goes to.
 * {@link #DIMENSION} takes the 1.12.2 dimension id instead of a level key.
 */
public final class PacketDistributor<T> {
    public static final PacketDistributor<EntityPlayerMP> PLAYER = new PacketDistributor<>(Kind.PLAYER);
    public static final PacketDistributor<Entity> TRACKING_ENTITY = new PacketDistributor<>(Kind.TRACKING_ENTITY);
    public static final PacketDistributor<Entity> TRACKING_ENTITY_AND_SELF = new PacketDistributor<>(Kind.TRACKING_ENTITY_AND_SELF);
    public static final PacketDistributor<Integer> DIMENSION = new PacketDistributor<>(Kind.DIMENSION);
    public static final PacketDistributor<Void> ALL = new PacketDistributor<>(Kind.ALL);

    private final Kind kind;

    private PacketDistributor(Kind kind) {
        this.kind = kind;
    }

    public PacketTarget with(Supplier<T> argument) {
        return new PacketTarget(kind, argument.get());
    }

    public PacketTarget noArg() {
        return new PacketTarget(kind, null);
    }

    enum Kind {
        PLAYER, TRACKING_ENTITY, TRACKING_ENTITY_AND_SELF, DIMENSION, ALL
    }

    public static final class PacketTarget {
        final Kind kind;
        final Object argument;

        private PacketTarget(Kind kind, Object argument) {
            this.kind = kind;
            this.argument = argument;
        }
    }
}
