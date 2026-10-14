package com.tacz.guns.legacy.resource;

/**
 * 1.20.1 {@code net.minecraft.util.profiling.ProfilerFiller}; reload listeners only pass it around.
 */
public interface ProfilerFiller {
    ProfilerFiller INACTIVE = new ProfilerFiller() {
    };

    default void startTick() {
    }

    default void endTick() {
    }

    default void push(String name) {
    }

    default void pop() {
    }

    default void popPush(String name) {
    }
}
