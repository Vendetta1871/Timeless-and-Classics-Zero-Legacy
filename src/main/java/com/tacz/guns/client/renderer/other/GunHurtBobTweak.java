package com.tacz.guns.client.renderer.other;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.Axis;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.MathHelper;

public class GunHurtBobTweak {
    private static long hurtByGunTimeStamp = -1L;
    private static float lastTweakMultiplier = 0.05f;

    public static boolean onHurtBobTweak(EntityPlayerSP player, PoseStack matrixStack, float partialTicks) {
        // 原版受伤的时长是 500 ms，所以如果大于 500 ms，那么说明不是子弹造成的伤害了
        if (System.currentTimeMillis() - hurtByGunTimeStamp > 500) {
            // 返回 false，让程序调用原版受伤晃动
            return false;
        }
        float zRot = (float) player.hurtTime - partialTicks;
        if (zRot < 0) {
            return true;
        }
        zRot /= (float) player.hurtDuration;
        zRot = MathHelper.sin(zRot * zRot * zRot * zRot * (float) Math.PI);
        float yRot = player.getHurtDir();

        yRot = yRot * lastTweakMultiplier;
        zRot = zRot * lastTweakMultiplier;

        matrixStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        matrixStack.mulPose(Axis.XP.rotationDegrees(-zRot * 14.0F));
        matrixStack.mulPose(Axis.YP.rotationDegrees(yRot));
        return true;
    }

    public static void markTimestamp(float tweakMultiplier) {
        hurtByGunTimeStamp = System.currentTimeMillis();
        lastTweakMultiplier = tweakMultiplier;
    }
}
