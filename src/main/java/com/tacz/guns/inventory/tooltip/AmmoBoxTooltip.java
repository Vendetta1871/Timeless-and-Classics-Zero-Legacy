package com.tacz.guns.inventory.tooltip;

import net.minecraft.item.ItemStack;

// 1.12.2 has no TooltipComponent: this is a plain data holder, the tooltip image is drawn by legacy.util.TooltipComponentHandler
public class AmmoBoxTooltip {
    private final ItemStack ammoBox;
    private final ItemStack ammo;
    private final int count;

    public AmmoBoxTooltip(ItemStack ammoBox, ItemStack ammo, int count) {
        this.ammoBox = ammoBox;
        this.ammo = ammo;
        this.count = count;
    }

    public ItemStack getAmmoBox() {
        return ammoBox;
    }

    public ItemStack getAmmo() {
        return ammo;
    }

    public int getCount() {
        return count;
    }
}
