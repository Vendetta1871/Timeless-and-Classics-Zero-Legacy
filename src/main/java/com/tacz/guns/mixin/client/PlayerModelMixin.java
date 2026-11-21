package com.tacz.guns.mixin.client;

import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.third.AnimationRenderContext;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2: {@code PlayerModel#setupAnim} is {@link ModelPlayer#setRotationAngles}; the sleeves are the arm wear parts.
 * The hand draw is recognized through {@code AnimationRenderContext}, as upstream does.
 */
@Mixin(ModelPlayer.class)
public abstract class PlayerModelMixin extends ModelBiped {
    @Shadow
    public ModelRenderer bipedLeftArmwear;
    @Shadow
    public ModelRenderer bipedRightArmwear;

    @Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", at = @At(value = "TAIL"))
    private void setRotationAnglesTail(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if (!(entityIn instanceof EntityPlayer) || !AnimationRenderContext.isFirstPersonHand(entityIn)) {
            return;
        }

        // Only neutralize the vanilla arm rotation during an actual hand draw.
        ItemStack currentItem = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (IGun.getIGunOrNull(currentItem) != null) {
            tacz$resetAll(this.bipedRightArm);
            tacz$resetAll(this.bipedLeftArm);
            copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
            copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        }
    }

    /**
     * 将给定模型的旋转角度重置为零
     */
    @Unique
    private void tacz$resetAll(ModelRenderer part) {
        part.rotateAngleX = 0.0F;
        part.rotateAngleY = 0.0F;
        part.rotateAngleZ = 0.0F;
    }
}
