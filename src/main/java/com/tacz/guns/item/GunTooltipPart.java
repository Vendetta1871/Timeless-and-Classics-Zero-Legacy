package com.tacz.guns.item;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraft.item.ItemStack;

public enum GunTooltipPart {
    DESCRIPTION,
    AMMO_INFO,
    BASE_INFO,
    EXTRA_DAMAGE_INFO,
    UPGRADES_TIP,
    PACK_INFO;

    private final int mask = 1 << this.ordinal();

    public int getMask() {
        return this.mask;
    }

    public static int getHideFlags(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.contains("HideFlags", NBTBase.TAG_ANY_NUMERIC)) {
            return tag.getInt("HideFlags");
        }
        return stack.getItem().getDefaultTooltipHideFlags(stack);
    }

    public static void setHideFlags(ItemStack stack, int mask) {
        stack.getOrCreateTag().setInteger("HideFlags", mask);
    }
}
