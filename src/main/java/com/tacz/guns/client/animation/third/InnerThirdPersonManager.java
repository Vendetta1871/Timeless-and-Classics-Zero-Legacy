package com.tacz.guns.client.animation.third;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class InnerThirdPersonManager {
    public static void setRotationAnglesHead(EntityLivingBase entityIn, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float limbSwingAmount) {
        if (AnimationRenderContext.isFirstPersonHand(entityIn) || !PlayerAnimatorCompat.canPlayAnimation(entityIn)) {
            return;
        }
        if (entityIn instanceof IGunOperator operator) {
            ItemStack mainHandItem = entityIn.getHeldItemMainhand();
            TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                if (!PlayerAnimatorCompat.hasPlayerAnimator3rd(entityIn, display)) {
                    playVanillaAnimation(entityIn, rightArm, leftArm, body, head, operator, display);
                }
            });
        }
    }

    private static void playVanillaAnimation(EntityLivingBase entityIn, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, IGunOperator operator, GunDisplayInstance display) {
        String animation = display.getThirdPersonAnimation();
        float aimingProgress = operator.getSynAimingProgress();
        if (aimingProgress <= 0) {
            ThirdPersonManager.getAnimation(animation).animateGunHold(entityIn, rightArm, leftArm, body, head);
        } else {
            ThirdPersonManager.getAnimation(animation).animateGunAim(entityIn, rightArm, leftArm, body, head, aimingProgress);
        }
    }
}
