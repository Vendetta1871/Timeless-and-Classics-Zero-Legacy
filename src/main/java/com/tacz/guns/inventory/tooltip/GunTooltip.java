package com.tacz.guns.inventory.tooltip;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

// 1.12.2 has no TooltipComponent: this is a plain data holder, the tooltip image is drawn by legacy.util.TooltipComponentHandler
public class GunTooltip {
    private final ItemStack gun;
    private final IGun iGun;
    private final ResourceLocation ammoId;
    private final CommonGunIndex gunIndex;

    public GunTooltip(ItemStack gun, IGun iGun, ResourceLocation ammoId, CommonGunIndex gunIndex) {
        this.gun = gun;
        this.iGun = iGun;
        this.ammoId = ammoId;
        this.gunIndex = gunIndex;
    }

    public ItemStack getGun() {
        return gun;
    }

    public IGun getIGun() {
        return iGun;
    }

    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    public CommonGunIndex getGunIndex() {
        return gunIndex;
    }
}
