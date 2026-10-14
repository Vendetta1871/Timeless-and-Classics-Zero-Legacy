package com.tacz.guns.event;

import com.tacz.guns.api.item.IGun;
import net.minecraft.util.EnumHand;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventGunClick {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // 只要主手有枪，那么禁止交互
        ItemStack itemInHand = event.getEntity().getHeldItem(EnumHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof IGun) {
            event.setCanceled(true);
        }
    }
}
