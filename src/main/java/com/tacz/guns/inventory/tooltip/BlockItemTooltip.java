package com.tacz.guns.inventory.tooltip;

import net.minecraft.util.ResourceLocation;

// 1.12.2 has no TooltipComponent: this is a plain data holder, the tooltip image is drawn by legacy.util.TooltipComponentHandler
public class BlockItemTooltip {
    private final ResourceLocation blockId;

    public BlockItemTooltip(ResourceLocation blockId) {
        this.blockId = blockId;
    }

    public ResourceLocation getBlockId() {
        return blockId;
    }
}
