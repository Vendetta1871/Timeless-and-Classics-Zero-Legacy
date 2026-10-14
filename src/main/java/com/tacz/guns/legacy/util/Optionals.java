package com.tacz.guns.legacy.util;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Java 9+ {@link Optional} methods for the Java 8 runtime.
 */
public final class Optionals {
    private Optionals() {
    }

    public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }
}
