package com.tacz.guns.mixin.client;

import com.tacz.guns.legacy.compat.PlayerAnimationLegacyCompat;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Saves the TACZ gun pose before Player Animation Legacy animates the player model.
 * See {@link PlayerAnimationLegacyCompat}. Injectors at the same RETURN run in mixin priority order, Player
 * Animation Legacy's at the default 1000.
 */
@Mixin(value = ModelPlayer.class, priority = 500)
public class ModelPlayerPoseCaptureMixin {
    @Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", at = @At("RETURN"))
    private void tacz$capturePose(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        PlayerAnimationLegacyCompat.capture((ModelPlayer) (Object) this, entityIn, ageInTicks);
    }
}
