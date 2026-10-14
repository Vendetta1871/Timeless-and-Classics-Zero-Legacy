package com.tacz.guns.client.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.client.renderer.other.GunHurtBobTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class PlayerHurtByGunEvent {
    @SubscribeEvent
    public static void onPlayerHurtByGun(EntityHurtByGunEvent.Post event) {
        Side logicalSide = event.getLogicalSide();
        if (logicalSide != Side.CLIENT) {
            return;
        }
        Entity hurtEntity = event.getHurtEntity();
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        // 当受伤的是自己的时候，触发受伤晃动的调整参数
        if (player != null && player.equals(hurtEntity)) {
            ResourceLocation gunId = event.getGunId();
            TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
                float tweakMultiplier = index.getGunData().getHurtBobTweakMultiplier();
                GunHurtBobTweak.markTimestamp(tweakMultiplier);
            });
        }
    }
}
