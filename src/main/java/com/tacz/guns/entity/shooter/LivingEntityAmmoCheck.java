package com.tacz.guns.entity.shooter;

import com.tacz.guns.config.common.GunConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class LivingEntityAmmoCheck {
    private final EntityLivingBase shooter;

    public LivingEntityAmmoCheck(EntityLivingBase shooter) {
        this.shooter = shooter;
    }

    public boolean needCheckAmmo() {
        if (shooter instanceof EntityPlayer player) {
            return !player.isCreative();
        }
        return true;
    }

    public boolean consumesAmmoOrNot() {
        if (shooter instanceof EntityPlayer player) {
            return !player.isCreative() || GunConfig.CREATIVE_PLAYER_CONSUME_AMMO.get();
        }
        return true;
    }
}
