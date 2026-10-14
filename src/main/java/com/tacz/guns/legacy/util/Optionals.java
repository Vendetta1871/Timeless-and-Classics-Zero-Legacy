package com.tacz.guns.legacy.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Java 9+ {@link Optional} methods for the Java 8 runtime.
 */
public final class Optionals {
    private Optionals() {
    }

    /**
     * {@code Objects.requireNonNullElseGet} (Java 9).
     */
    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier) {
        return obj != null ? obj : java.util.Objects.requireNonNull(supplier.get(), "supplier.get()");
    }

    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }
}
