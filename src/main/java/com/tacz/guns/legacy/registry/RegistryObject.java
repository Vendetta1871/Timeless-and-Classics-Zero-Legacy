package com.tacz.guns.legacy.registry;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 1.20.1 {@code RegistryObject}: a handle to an object that a {@link DeferredRegister} creates and registers in the
 * matching 1.12.2 {@code RegistryEvent.Register}. {@link #get()} throws before that.
 */
public final class RegistryObject<T> implements Supplier<T> {
    private final ResourceLocation id;
    private T value;

    RegistryObject(ResourceLocation id) {
        this.id = id;
    }

    void set(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return Objects.requireNonNull(value, () -> "Registry object not present: " + id);
    }

    public boolean isPresent() {
        return value != null;
    }

    public ResourceLocation getId() {
        return id;
    }
}
