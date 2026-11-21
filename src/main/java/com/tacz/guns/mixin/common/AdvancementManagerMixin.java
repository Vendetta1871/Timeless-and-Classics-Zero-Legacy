package com.tacz.guns.mixin.common;

import com.tacz.guns.legacy.advancement.GunPackAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

/**
 * 1.12.2: adds the gun packs' advancements to the ones being loaded (on 1.20.1 gun packs are data packs), after the
 * world folder's, the built-in and the mod jars' ones; see {@link GunPackAdvancements}.
 */
@Mixin(AdvancementManager.class)
public class AdvancementManagerMixin {
    @ModifyArg(method = "reload", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/advancements/AdvancementList;loadAdvancements(Ljava/util/Map;)V"))
    private Map<ResourceLocation, Advancement.Builder> tacz$addGunPackAdvancements(Map<ResourceLocation, Advancement.Builder> map) {
        GunPackAdvancements.INSTANCE.addTo(map);
        return map;
    }
}
