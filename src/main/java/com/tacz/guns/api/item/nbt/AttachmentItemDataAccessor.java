package com.tacz.guns.api.item.nbt;

import com.tacz.guns.legacy.util.StackNbt;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAttachment;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.util.ResourceLocations;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AttachmentItemDataAccessor extends IAttachment {
    String ATTACHMENT_ID_TAG = "AttachmentId";
    String SKIN_ID_TAG = "Skin";
    String ZOOM_NUMBER_TAG = "ZoomNumber";
    String LASER_COLOR_TAG = "LaserColor";

    // 仅检查给定的 NBTTagCompound 是否具有配件 ID ，不校验其是否存在
    static boolean isAttachmentLike(NBTTagCompound tag) {
        return tag.hasKey(ATTACHMENT_ID_TAG, Constants.NBT.TAG_STRING);
    }

    @Nonnull
    static ResourceLocation getAttachmentIdFromTag(@Nullable NBTTagCompound nbt) {
        if (nbt == null) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        if (isAttachmentLike(nbt)) {
            ResourceLocation attachmentId = ResourceLocations.tryParse(nbt.getString(ATTACHMENT_ID_TAG));
            return attachmentId != null ? attachmentId : DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        return DefaultAssets.EMPTY_ATTACHMENT_ID;
    }

    static int getZoomNumberFromTag(@Nullable NBTTagCompound nbt) {
        if (nbt == null) {
            return 0;
        }
        if (nbt.hasKey(ZOOM_NUMBER_TAG, Constants.NBT.TAG_INT)) {
            return nbt.getInteger(ZOOM_NUMBER_TAG);
        }
        return 0;
    }

    static void setZoomNumberToTag(NBTTagCompound nbt, int zoomNumber) {
        nbt.setInteger(ZOOM_NUMBER_TAG, zoomNumber);
    }

    @Override
    @Nonnull
    default ResourceLocation getAttachmentId(ItemStack attachmentStack) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        return getAttachmentIdFromTag(nbt);
    }

    @Override
    default void setAttachmentId(ItemStack attachmentStack, @Nullable ResourceLocation attachmentId) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        if (attachmentId != null) {
            nbt.setString(ATTACHMENT_ID_TAG, attachmentId.toString());
        }
    }

    @Override
    @Nullable
    default ResourceLocation getSkinId(ItemStack attachmentStack) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        if (nbt.hasKey(SKIN_ID_TAG, Constants.NBT.TAG_STRING)) {
            return ResourceLocations.tryParse(nbt.getString(SKIN_ID_TAG));
        }
        return null;
    }

    @Override
    default void setSkinId(ItemStack attachmentStack, @Nullable ResourceLocation skinId) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        if (skinId != null) {
            nbt.setString(SKIN_ID_TAG, skinId.toString());
        } else {
            nbt.removeTag(SKIN_ID_TAG);
        }
    }

    @Override
    default int getZoomNumber(ItemStack attachmentStack) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        return getZoomNumberFromTag(nbt);
    }

    @Override
    default void setZoomNumber(ItemStack attachmentStack, int zoomNumber) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        setZoomNumberToTag(nbt, zoomNumber);
    }

    @Override
    default boolean hasCustomLaserColor(ItemStack attachmentStack) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        return nbt.hasKey(LASER_COLOR_TAG, Constants.NBT.TAG_INT);
    }

    @Override
    default int getLaserColor(ItemStack attachmentStack) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        if (!hasCustomLaserColor(attachmentStack)) {
            return 0xFF0000;
        }
        return nbt.getInteger(LASER_COLOR_TAG);
    }

    @Override
    default void setLaserColor(ItemStack attachmentStack, int color) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(attachmentStack);
        nbt.setInteger(LASER_COLOR_TAG, color);
    }
}
