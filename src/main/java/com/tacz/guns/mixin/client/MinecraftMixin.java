package com.tacz.guns.mixin.client;

import com.tacz.guns.legacy.resource.ClientGunPacks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2 only: rescans the gun packs before the resource pack list is collected, which is when upstream's pack
 * finder runs.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "refreshResources()V", at = @At("HEAD"))
    private void tacz$rescanGunPacks(CallbackInfo ci) {
        ClientGunPacks.rescan();
    }
}
