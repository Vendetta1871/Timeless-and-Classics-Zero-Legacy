package com.tacz.guns.mixin.client;

import com.tacz.guns.client.animation.third.InnerThirdPersonManager;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2: {@code HumanoidModel#setupAnim} is {@link ModelBiped#setRotationAngles}.
 */
@Mixin(ModelBiped.class)
public class HumanoidModelMixin {
    @Shadow
    public ModelRenderer bipedHead;
    @Shadow
    public ModelRenderer bipedBody;
    @Shadow
    public ModelRenderer bipedLeftArm;
    @Shadow
    public ModelRenderer bipedRightArm;

    @Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", at = @At(value = "TAIL"))
    private void setRotationAnglesHead(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if (ageInTicks == 0 || !(entityIn instanceof EntityLivingBase livingEntity)) {
            return;
        }
        InnerThirdPersonManager.setRotationAnglesHead(livingEntity, bipedRightArm, bipedLeftArm, bipedBody, bipedHead, limbSwingAmount);
    }
}
