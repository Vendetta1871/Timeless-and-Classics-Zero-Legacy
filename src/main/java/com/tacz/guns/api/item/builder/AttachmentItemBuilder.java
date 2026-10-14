package com.tacz.guns.api.item.builder;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAttachment;
// TODO(port): needs ModItems
// import com.tacz.guns.init.ModItems;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

public class AttachmentItemBuilder {
    private int count = 1;
    private ResourceLocation attachmentId = DefaultAssets.DEFAULT_ATTACHMENT_ID;

    private AttachmentItemBuilder() {
    }

    public static AttachmentItemBuilder create() {
        return new AttachmentItemBuilder();
    }

    public AttachmentItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public AttachmentItemBuilder setId(ResourceLocation id) {
        this.attachmentId = id;
        return this;
    }

    @Deprecated
    public AttachmentItemBuilder setSkinId(ResourceLocation skinId) {
        return this;
    }

    public ItemStack build() {
        // TODO(port): needs ModItems
        // ItemStack attachment = new ItemStack(ModItems.ATTACHMENT.get(), this.count);
        ItemStack attachment = ItemStack.EMPTY; // placeholder until ModItems is ported
        if (attachment.getItem() instanceof IAttachment iAttachment) {
            iAttachment.setAttachmentId(attachment, this.attachmentId);
        }
        return attachment;
    }
}
