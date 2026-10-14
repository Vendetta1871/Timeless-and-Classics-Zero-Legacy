package com.tacz.guns.api.event.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 生物的枪击发的事件。与 {@link GunShootEvent}不同的是，扣动一次扳机可能多次触发这个事件（如枪械处于 Burst 模式），但 {@link GunShootEvent} 只会触发一次
 */
public class GunFireEvent extends Event implements KubeJSGunEventPoster<GunFireEvent>{
    private final EntityLivingBase shooter;
    private final ItemStack gunItemStack;
    private final Side logicalSide;

    public GunFireEvent(EntityLivingBase shooter, ItemStack gunItemStack, Side side) {
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
