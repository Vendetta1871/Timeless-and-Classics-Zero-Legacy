package com.tacz.guns.legacy.network;

import net.minecraft.util.ResourceLocation;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 1.20.1 {@code NetworkRegistry.newSimpleChannel}. 1.12.2 has no per-channel version handshake (FML compares mod
 * versions instead), so the version arguments are not checked.
 */
public final class NetworkRegistry {
    private NetworkRegistry() {
    }

    public static SimpleChannel newSimpleChannel(ResourceLocation name, Supplier<String> networkProtocolVersion,
                                                 Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
        return new SimpleChannel(name);
    }
}
