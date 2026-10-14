package com.tacz.guns.legacy.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 1.20.1 {@code ItemStack#getOrCreateTag()} for 1.12.2.
 */
public final class StackNbt {
    private StackNbt() {
    }

    public static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }
}
