package com.tacz.guns.legacy.util;

import java.util.function.Consumer;

/**
 * 1.20.1 {@code net.minecraft.Util} (only the methods TACZ uses).
 */
public final class Util {
    private Util() {
    }

    public static <T> T make(T object, Consumer<T> initializer) {
        initializer.accept(object);
        return object;
    }
}
