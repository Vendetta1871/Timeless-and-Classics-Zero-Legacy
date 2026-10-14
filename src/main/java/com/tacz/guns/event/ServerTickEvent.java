package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.util.CycleTaskHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ServerTickEvent {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 更新 CycleTaskHelper 中的任务
        CycleTaskHelper.tick();
    }
}
