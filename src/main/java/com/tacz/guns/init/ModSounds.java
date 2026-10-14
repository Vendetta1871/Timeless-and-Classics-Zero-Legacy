package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import com.tacz.guns.legacy.registry.DeferredRegister;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import com.tacz.guns.legacy.registry.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GunMod.MOD_ID);

    public static final RegistryObject<SoundEvent> GUN = SOUNDS.register("gun", () -> new SoundEvent(new ResourceLocation(GunMod.MOD_ID, "gun")));
    public static final RegistryObject<SoundEvent> TARGET_HIT = SOUNDS.register("target_block_hit", () -> new SoundEvent(new ResourceLocation(GunMod.MOD_ID, "target_block_hit")));
}
