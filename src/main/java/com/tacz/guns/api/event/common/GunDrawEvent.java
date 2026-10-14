package com.tacz.guns.api.event.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 生物开始更换枪械弹药时触发的事件。
 */
public class GunDrawEvent extends Event implements KubeJSGunEventPoster<GunDrawEvent>{
    private final EntityLivingBase entity;
    private final ItemStack previousGunItem;
    private final ItemStack currentGunItem;
    private final Side logicalSide;

    public GunDrawEvent(EntityLivingBase entity, ItemStack previousGunItem, ItemStack currentGunItem, Side side) {
        this.entity = entity;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }

    public EntityLivingBase getEntity() {
        return entity;
    }

    public ItemStack getPreviousGunItem() {
        return previousGunItem;
    }

    public ItemStack getCurrentGunItem() {
        return currentGunItem;
    }

    public Side getLogicalSide() {
        return logicalSide;
    }
}
