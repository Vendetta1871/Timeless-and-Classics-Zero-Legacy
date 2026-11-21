package com.tacz.guns.util;

import net.minecraft.nbt.NBTTagCompound;
import com.tacz.guns.legacy.util.ResourceLocations;
import com.tacz.guns.legacy.util.StackNbt;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class VariantHelper {
    private static final String VARIANT_TAG = "Variant";

    private VariantHelper() {
    }

    public static void setVariant(ItemStack attachment, ResourceLocation variantId) {
        if (attachment.isEmpty() || variantId == null) {
            return;
        }
        StackNbt.getOrCreateTag(attachment).setString(VARIANT_TAG, variantId.toString());
    }

    @Nullable
    public static ResourceLocation getVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return null;
        }
        NBTTagCompound tag = attachment.getTagCompound();
        if (tag == null || !tag.hasKey(VARIANT_TAG, Constants.NBT.TAG_STRING)) {
            return null;
        }
        return ResourceLocations.tryParse(tag.getString(VARIANT_TAG));
    }

    public static boolean hasVariant(ItemStack attachment) {
        return getVariant(attachment) != null;
    }

    public static void removeVariant(ItemStack attachment) {
        if (attachment.isEmpty()) {
            return;
        }
        NBTTagCompound tag = attachment.getTagCompound();
        if (tag != null) {
            tag.removeTag(VARIANT_TAG);
        }
    }
}
