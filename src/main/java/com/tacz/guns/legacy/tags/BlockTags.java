package com.tacz.guns.legacy.tags;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;

/**
 * 1.20.1 {@code BlockTags} (only what TACZ uses).
 */
public final class BlockTags {
    public static final TagKey<Block> RAILS = create(new ResourceLocation("minecraft", "rails"));

    private BlockTags() {
    }

    public static TagKey<Block> create(ResourceLocation location) {
        return TagKey.create(TagType.BLOCK, location);
    }
}
