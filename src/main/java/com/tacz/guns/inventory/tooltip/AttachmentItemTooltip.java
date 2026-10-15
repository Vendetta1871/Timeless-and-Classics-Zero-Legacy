package com.tacz.guns.inventory.tooltip;

import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

// 1.12.2 has no TooltipComponent: this is a plain data holder, the tooltip image is drawn by legacy.util.TooltipComponentHandler
public class AttachmentItemTooltip {
    private final ItemStack attachmentItem;
    private final ResourceLocation attachmentId;
    private final AttachmentType type;

    public AttachmentItemTooltip(ResourceLocation attachmentId, AttachmentType type, ItemStack attachmentItem) {
        this.attachmentId = attachmentId;
        this.type = type;
        this.attachmentItem = attachmentItem;
    }

    public ResourceLocation getAttachmentId() {
        return attachmentId;
    }

    public AttachmentType getType() {
        return type;
    }

    public ItemStack getAttachmentItem() {
        return attachmentItem;
    }
}
