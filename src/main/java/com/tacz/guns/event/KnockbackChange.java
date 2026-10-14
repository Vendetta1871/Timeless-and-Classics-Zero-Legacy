package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.KnockBackModifier;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class KnockbackChange {
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(event.getEntityLiving());
        double strength = modifier.getKnockBackStrength();
        if (strength >= 0) {
            event.setStrength((float) strength);
        }
    }
}
