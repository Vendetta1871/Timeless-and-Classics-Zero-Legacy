package com.tacz.guns.api.event.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 用枪近战时触发
 */
public class GunMeleeEvent extends Event implements KubeJSGunEventPoster<GunMeleeEvent>{
    private final EntityLivingBase shooter;
    private final ItemStack gunItemStack;
    private final Side logicalSide;

    public GunMeleeEvent(EntityLivingBase shooter, ItemStack gunItemStack, Side side) {
        this.shooter = shooter;
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    public EntityLivingBase getShooter() {
        return shooter;
    }

    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public Side getLogicalSide() {
        return logicalSide;
    }
}
