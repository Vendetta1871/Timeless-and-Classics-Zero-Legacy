package com.tacz.guns.api.event.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 生物射击时触发的事件。与 {@link GunFireEvent}不同的是，扣动一次扳机只会触发一次这个事件，但可能多次触发 {@link GunFireEvent}（如枪械处于 Burst 模式）
 */
public class GunShootEvent extends Event implements KubeJSGunEventPoster<GunShootEvent>{
    private final EntityLivingBase shooter;
    private final ItemStack gunItemStack;
    private final Side logicalSide;

    public GunShootEvent(EntityLivingBase shooter, ItemStack gunItemStack, Side side) {
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
