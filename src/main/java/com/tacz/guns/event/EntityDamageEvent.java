package com.tacz.guns.event;

import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class EntityDamageEvent {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event){
        if (event.getSource().is(ModDamageTypes.BULLETS_TAG)) {
            EntityLivingBase living = event.getEntity();

            AttributeInstance resistance = living.getAttribute(ModAttributes.BULLET_RESISTANCE.get());
            if (resistance != null) {
                float modifiedDamage = event.getAmount() * (float) (1 - resistance.getValue());
                event.setAmount(modifiedDamage);
            }
        }
    }
}
