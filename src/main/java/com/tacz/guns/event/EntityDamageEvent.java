package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class EntityDamageEvent {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event){
        // TODO(port): needs ModDamageTypes (BULLETS_TAG)
        // if (event.getSource().is(ModDamageTypes.BULLETS_TAG)) {
        if (false) { // placeholder until ModDamageTypes is ported
            EntityLivingBase living = event.getEntityLiving();

            IAttributeInstance resistance = living.getEntityAttribute(ModAttributes.BULLET_RESISTANCE.get());
            if (resistance != null) {
                float modifiedDamage = event.getAmount() * (float) (1 - resistance.getAttributeValue());
                event.setAmount(modifiedDamage);
            }
        }
    }
}
