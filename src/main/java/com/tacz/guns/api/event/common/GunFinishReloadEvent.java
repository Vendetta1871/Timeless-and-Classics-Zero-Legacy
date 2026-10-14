package com.tacz.guns.api.event.common;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 生物结束更换枪械弹药时触发的事件。
 */
public class GunFinishReloadEvent extends Event implements KubeJSGunEventPoster<GunFinishReloadEvent>{
    private final ItemStack gunItemStack;
    private final Side logicalSide;

    public GunFinishReloadEvent(ItemStack gunItemStack, Side side) {
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public Side getLogicalSide() {
        return logicalSide;
    }
}
