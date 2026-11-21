package com.tacz.guns.util;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.TimelessAPI;
import net.minecraft.nbt.NBTTagCompound;
import com.tacz.guns.legacy.util.StackNbt;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

public final class VirtualOemAttachment {
    private static final String VIRTUAL_OEM_TAG = "VirtualOemAttachment";

    private VirtualOemAttachment() {
    }

    public static boolean isAllowedVirtualOem(ResourceLocation gunId, ResourceLocation attachmentId, AttachmentType type) {
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> {
                    var builtInAttachment = index.getGunData().getBuiltInAttachments().get(type);
                    return builtInAttachment != null && builtInAttachment.getAttachments().contains(attachmentId);
                })
                .orElse(false);
    }

    public static boolean shouldShowIcon(ResourceLocation gunId, ResourceLocation attachmentId, AttachmentType type) {
        return TimelessAPI.getCommonGunIndex(gunId)
                .map(index -> {
                    var builtInAttachment = index.getGunData().getBuiltInAttachments().get(type);
                    // 回退显示的配件已在枪上，不重复列入改装选项。
                    return builtInAttachment != null && builtInAttachment.getAttachments().contains(attachmentId)
                            && builtInAttachment.isShowIcon() && !attachmentId.equals(builtInAttachment.getDisplayAttachment());
                })
                .orElse(false);
    }

    public static boolean isBuiltinAttachment(ResourceLocation attachmentId) {
        return TimelessAPI.getCommonAttachmentIndex(attachmentId)
                .map(index -> index.getPojo().isBuiltinAttachment())
                .orElse(false);
    }

    public static void mark(ItemStack attachmentStack) {
        StackNbt.getOrCreateTag(attachmentStack).setBoolean(VIRTUAL_OEM_TAG, true);
    }

    public static boolean isMarked(ItemStack attachmentStack) {
        NBTTagCompound tag = attachmentStack.getTagCompound();
        return tag != null && tag.hasKey(VIRTUAL_OEM_TAG, Constants.NBT.TAG_BYTE) && tag.getBoolean(VIRTUAL_OEM_TAG);
    }
}
