package com.tacz.guns.legacy.tags;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * The registries TACZ has tags for, with the data folder their tag files live in and how a 1.12.2 value is named.
 * Stands in for 1.20.1 {@code Registries.BLOCK} / {@code Registries.ENTITY_TYPE}.
 */
public final class TagType<T> {
    public static final TagType<Block> BLOCK = new TagType<>("blocks", Block.REGISTRY::getNameForObject);
    /**
     * Entity tags match entities by their entity id (1.12.2 has no entity types).
     */
    public static final TagType<Entity> ENTITY_TYPE = new TagType<>("entity_types", EntityList::getKey);

    private final String directory;
    private final Function<T, ResourceLocation> idGetter;

    private TagType(String directory, Function<T, ResourceLocation> idGetter) {
        this.directory = directory;
        this.idGetter = idGetter;
    }

    public String getDirectory() {
        return directory;
    }

    @Nullable
    ResourceLocation idOf(T value) {
        return idGetter.apply(value);
    }
}
