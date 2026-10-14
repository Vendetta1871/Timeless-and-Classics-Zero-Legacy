package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.function.Supplier;

/**
 * 1.12.2: attributes have no registry; an attribute exists once it is registered on an entity's attribute map. Like
 * upstream's {@code EntityAttributeModificationEvent} (adds the attribute to every living entity type), it is added
 * to every {@link EntityLivingBase} when the entity is constructed.
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ModAttributes {
    private static final IAttribute BULLET_RESISTANCE_ATTRIBUTE =
            new RangedAttribute(null, "attribute.name.tacz.bullet_resistance", 0.0D, 0.0D, 1.0D).setShouldWatch(true);
//
//    private static final IAttribute WEIGHT_CAPACITY_ATTRIBUTE =
//            new RangedAttribute(null, "attribute.name.tacz.weight_capacity", 0.0D, -1024D, 1024.0D).setShouldWatch(true);

    public static final Supplier<IAttribute> BULLET_RESISTANCE = () -> BULLET_RESISTANCE_ATTRIBUTE;

    @SubscribeEvent
    public static void registerAttributes(EntityEvent.EntityConstructing event) {
        if (event.getEntity() instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) event.getEntity();
            if (living.getAttributeMap().getAttributeInstance(BULLET_RESISTANCE_ATTRIBUTE) == null) {
                living.getAttributeMap().registerAttribute(BULLET_RESISTANCE_ATTRIBUTE);
            }
        }
    }
}
