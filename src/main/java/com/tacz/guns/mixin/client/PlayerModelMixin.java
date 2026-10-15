package com.tacz.guns.mixin.client;

import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
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
 * First-person arm rendering also calls it with {@code ageInTicks == 0}, as upstream relies on.
 */
@Mixin(ModelPlayer.class)
public abstract class PlayerModelMixin extends ModelBiped {
    @Shadow
    public ModelRenderer bipedLeftArmwear;
    @Shadow
    public ModelRenderer bipedRightArmwear;

    @Inject(method = "setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", at = @At(value = "TAIL"))
    private void setRotationAnglesTail(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn, CallbackInfo ci) {
        if (!(entityIn instanceof EntityPlayer player)) {
            return;
        }

        // 用于清除默认的手臂旋转
        // 当第一人称渲染是，ageInTicks 正好是 0
        ItemStack currentItem = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (ageInTicks == 0F && IGun.getIGunOrNull(currentItem) != null) {
            tacz$resetAll(this.bipedRightArm);
            tacz$resetAll(this.bipedLeftArm);
            copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
            copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        }
    }

    /**
     * 将给定模型的旋转角度和旋转点重置为零
     */
    @Unique
    private void tacz$resetAll(ModelRenderer part) {
        part.rotateAngleX = 0.0F;
        part.rotateAngleY = 0.0F;
        part.rotateAngleZ = 0.0F;
    }
}
