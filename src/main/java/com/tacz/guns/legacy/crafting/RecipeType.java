package com.tacz.guns.legacy.crafting;

import net.minecraft.util.ResourceLocation;

/**
 * 1.20.1 {@code RecipeType}: a marker that selects recipes of one kind from a {@link RecipeManager}.
 */
public final class RecipeType<T> {
    private final ResourceLocation id;

    public RecipeType(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
