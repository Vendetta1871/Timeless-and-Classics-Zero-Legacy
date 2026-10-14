package com.tacz.guns.event;

import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.util.HitboxHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class HitboxHelperEvent {
    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!OtherConfig.SERVER_HITBOX_LATENCY_FIX.get()) {
            return;
        }
        if (event.side == Side.SERVER && event.phase == TickEvent.Phase.END) {
            HitboxHelper.onPlayerTick(event.player);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HitboxHelper.onPlayerLoggedOut(event.getEntity());
    }
}
