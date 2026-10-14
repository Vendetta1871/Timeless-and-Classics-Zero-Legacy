package com.tacz.guns.inventory.tooltip;

import net.minecraft.util.ResourceLocation;

// TODO(port): 1.12.2 has no TooltipComponent: this is a plain data holder until a tooltip image hook exists
public class BlockItemTooltip {
    private final ResourceLocation blockId;

    public BlockItemTooltip(ResourceLocation blockId) {
        this.blockId = blockId;
    }

    public ResourceLocation getBlockId() {
        return blockId;
    }
}
