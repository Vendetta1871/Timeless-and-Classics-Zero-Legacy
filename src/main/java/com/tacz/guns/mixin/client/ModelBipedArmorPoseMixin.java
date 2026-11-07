package com.tacz.guns.mixin.client;

import com.tacz.guns.legacy.compat.PlayerAnimationLegacyCompat;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives armor models the restored TACZ gun pose after Player Animation Legacy synced them to its own.
 * See {@link PlayerAnimationLegacyCompat}. Injectors at the same RETURN run in mixin priority order, Player
 * Animation Legacy's at the default 1000.
 */
@Mixin(value = ModelBiped.class, priority = 1500)
public class ModelBipedArmorPoseMixin {
    @Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", at = @At("RETURN"))
    private void tacz$restoreArmorPose(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        PlayerAnimationLegacyCompat.restoreArmor((ModelBiped) (Object) this, entityIn);
    }
}
