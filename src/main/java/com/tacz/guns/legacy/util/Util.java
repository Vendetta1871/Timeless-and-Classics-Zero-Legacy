package com.tacz.guns.legacy.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /**
     * 1.20 {@code Util#memoize(Function)}: caches the results of a function by argument.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> function) {
        Map<T, R> cache = new ConcurrentHashMap<>();
        return key -> cache.computeIfAbsent(key, function);
    }
}
