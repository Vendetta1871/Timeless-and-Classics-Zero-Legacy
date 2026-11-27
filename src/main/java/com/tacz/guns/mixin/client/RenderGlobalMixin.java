package com.tacz.guns.mixin.client;

import com.tacz.guns.legacy.client.CameraMatrices;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2 only: keeps the camera modelview of every pass that renders entities, for eye space to world conversions
 * ({@link CameraMatrices}).
 * <p>
 * Recorded where the entities of a pass start, so it is the very modelview the held guns of that pass are drawn
 * under, whoever calls {@code renderEntities}: Forge's and OptiFine's {@code renderWorldPass} (OptiFine's never ran
 * the {@code updateRenderInfo} hook this replaces, which left the third-person muzzle flash far from the muzzle) and
 * OptiFine's shader shadow pass.
 */
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin {
    @Inject(method = "renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V", at = @At("HEAD"))
    private void tacz$recordCameraMatrix(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        CameraMatrices.record();
    }
}
