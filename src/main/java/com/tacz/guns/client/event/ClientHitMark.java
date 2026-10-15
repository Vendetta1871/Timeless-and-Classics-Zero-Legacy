package com.tacz.guns.client.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.tacz.guns.legacy.util.Components;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class ClientHitMark {
    public static long lastHitTimestamp = 0;
    public static float damageAmount = 0;
    @SubscribeEvent
    public static void onEntityHurt(EntityHurtByGunEvent.Post event) {
        Side logicalSide = event.getLogicalSide();
        if (logicalSide != Side.CLIENT) {
            return;
        }
        EntityLivingBase attacker = event.getAttacker();
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        Entity hurtEntity = event.getHurtEntity();
        if (player != null && player.equals(attacker) && hurtEntity!=null) {
            ResourceLocation gunId = event.getGunId();
            ResourceLocation gunDisplayId = event.getGunDisplayId();
            RenderCrosshairEvent.markHitTimestamp();
            if (event.isHeadShot()) {
                RenderCrosshairEvent.markHeadShotTimestamp();
                TimelessAPI.getGunDisplay(gunDisplayId, gunId).ifPresent(index -> SoundPlayManager.playHeadHitSound(player, index));
            } else {
                TimelessAPI.getGunDisplay(gunDisplayId, gunId).ifPresent(index -> SoundPlayManager.playFleshHitSound(player, index));
            }

           if(hurtEntity instanceof TargetMinecart){
               if(System.currentTimeMillis() - lastHitTimestamp < RenderConfig.DAMAGE_COUNTER_RESET_TIME.get()) {
                   damageAmount += event.getAmount();
               } else {
                   damageAmount = event.getAmount();
               }
               float distance = player.getDistance(event.getHurtEntity());
               player.sendStatusMessage(Components.translatable("message.tacz.target_minecart.hit", String.format("%.1f", damageAmount), String.format("%.2f", distance)), true);

               lastHitTimestamp = System.currentTimeMillis();
           }
        }
    }

    @SubscribeEvent
    public static void onEntityKill(EntityKillByGunEvent event) {
        Side logicalSide = event.getLogicalSide();
        if (logicalSide != Side.CLIENT) {
            return;
        }
        EntityLivingBase attacker = event.getAttacker();
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && player.equals(attacker)) {
            RenderCrosshairEvent.markKillTimestamp();
            KillAmountOverlay.markTimestamp();
            TimelessAPI.getGunDisplay(event.getGunDisplayId(), event.getGunId()).ifPresent(index -> SoundPlayManager.playKillSound(player, index));
            if (event.isHeadShot()) {
                RenderCrosshairEvent.markHeadShotTimestamp();
            }
        }
    }
}
