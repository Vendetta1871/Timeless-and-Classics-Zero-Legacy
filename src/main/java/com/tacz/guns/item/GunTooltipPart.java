package com.tacz.guns.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.tacz.guns.legacy.util.StackNbt;

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
        if (tag != null && tag.hasKey("HideFlags", Constants.NBT.TAG_ANY_NUMERIC)) {
            return tag.getInteger("HideFlags");
        }
        // 1.12.2: Item#getDefaultTooltipHideFlags does not exist, items have no default hide flags
        return 0;
    }

    public static void setHideFlags(ItemStack stack, int mask) {
        StackNbt.getOrCreateTag(stack).setInteger("HideFlags", mask);
    }
}
