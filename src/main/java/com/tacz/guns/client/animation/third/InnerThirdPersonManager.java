package com.tacz.guns.client.animation.third;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class InnerThirdPersonManager {
    public static void setRotationAnglesHead(EntityLivingBase entityIn, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float limbSwingAmount) {
        // 游戏暂停时不进行动画计算，否则会 StackOverflow
        if (Minecraft.getMinecraft().isGamePaused()) {
            return;
        }
        if (entityIn instanceof IGunOperator operator) {
            ItemStack mainHandItem = entityIn.getHeldItemMainhand();
            IGun iGun = IGun.getIGunOrNull(mainHandItem);
            if (iGun == null) {
                // TODO(port): Player Animator compat is not ported (no 1.12.2 version)
//                PlayerAnimatorCompat.stopAllAnimation(entityIn);
                return;
            }
            // 睡觉、爬梯、游泳、鞘翅飞行不播放第三人称动画
            // 1.12.2: no poses; sleeping, ladders and elytra are checked directly, there is no swimming state
            if (entityIn instanceof EntityPlayer player && player.isPlayerSleeping() || entityIn.isOnLadder() || entityIn.isElytraFlying()) {
                // TODO(port): Player Animator compat is not ported (no 1.12.2 version)
//                PlayerAnimatorCompat.stopAllAnimation(entityIn);
                return;
            }

            TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                // TODO(port): Player Animator compat is not ported (no 1.12.2 version)
//                if (PlayerAnimatorCompat.hasPlayerAnimator3rd(entityIn, display)) {
//                    PlayerAnimatorCompat.playAnimation(entityIn, display, limbSwingAmount);
//                } else {
                    playVanillaAnimation(entityIn, rightArm, leftArm, body, head, operator, display);
//                }
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
