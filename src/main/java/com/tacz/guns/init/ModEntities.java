package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.TargetMinecart;
import com.tacz.guns.legacy.registry.DeferredRegister;
import com.tacz.guns.legacy.registry.RegistryObject;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityEntry> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, GunMod.MOD_ID);

    // TODO(port): needs EntityKineticBullet
    // public static RegistryObject<EntityEntry> BULLET = ENTITY_TYPES.register("bullet", () -> EntityKineticBullet.TYPE);
    public static RegistryObject<EntityEntry> TARGET_MINECART = ENTITY_TYPES.register("target_minecart", () -> TargetMinecart.TYPE);
}
