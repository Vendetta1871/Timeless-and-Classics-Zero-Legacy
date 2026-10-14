package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.TargetMinecart;
import com.tacz.guns.legacy.registry.DeferredRegister;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ModEntities {
    public static final DeferredRegister<EntityEntry> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, GunMod.MOD_ID);

    public static Supplier<EntityEntry> BULLET = () -> EntityKineticBullet.TYPE;

    /**
     * 1.12.2: an {@code EntityEntry} made by {@code EntityEntryBuilder} already carries its registry name (and the
     * tracker settings), so it cannot go through the {@link DeferredRegister}, which names the entries itself. It is
     * registered by {@link #onRegisterEntities} instead; this accessor keeps upstream's {@code .get()}.
     */
    public static Supplier<EntityEntry> TARGET_MINECART = () -> TargetMinecart.TYPE;

    @SubscribeEvent
    public static void onRegisterEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(BULLET.get());
        event.getRegistry().register(TARGET_MINECART.get());
    }
}
