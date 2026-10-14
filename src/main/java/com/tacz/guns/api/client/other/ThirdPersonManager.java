package com.tacz.guns.api.client.other;

import com.google.common.collect.Maps;
import net.minecraft.client.model.ModelRenderer;
import com.tacz.guns.legacy.util.LegacyMath;
import net.minecraft.entity.EntityLivingBase;

import java.util.Map;

/**
 * 简单的第三人称持枪动画 Manager
 */
public final class ThirdPersonManager {
    private static final Map<String, IThirdPersonAnimation> CACHE = Maps.newHashMap();
    private static final String RESERVED_DEFAULT_NAME = "default";
    private static final IThirdPersonAnimation DEFAULT = new IThirdPersonAnimation() {
        @Override
        public void animateGunHold(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head) {
            rightArm.rotateAngleY = -0.3F + head.rotateAngleY;
            leftArm.rotateAngleY = 0.8F + head.rotateAngleY;
            rightArm.rotateAngleX = -1.4F + head.rotateAngleX;
            leftArm.rotateAngleX = -1.4F + head.rotateAngleX;
        }

        @Override
        public void animateGunAim(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float aimProgress) {
            float lerp1 = LegacyMath.lerp(aimProgress, 0.3f, 0.35f);
            float lerp2 = LegacyMath.lerp(aimProgress, 1.4f, 1.6f);
            rightArm.rotateAngleY = -lerp1 + head.rotateAngleY;
            leftArm.rotateAngleY = 0.8F + head.rotateAngleY;
            rightArm.rotateAngleX = -lerp2 + head.rotateAngleX;
            leftArm.rotateAngleX = -lerp2 + head.rotateAngleX;
        }
    };

    private static final String MINI_GUN_NAME = "minigun";
    private static final IThirdPersonAnimation MINI_GUN = new IThirdPersonAnimation() {
        @Override
        public void animateGunHold(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head) {
            body.rotateAngleY = head.rotateAngleY+0.8f;

            double cosTheta = Math.cos(-body.rotateAngleY);
            double sinTheta = Math.sin(-body.rotateAngleY);

            float x = rightArm.rotationPointX;
            rightArm.rotationPointX = (float) (x * cosTheta);
            rightArm.rotationPointZ = (float) (x * sinTheta);

            rightArm.rotateAngleY = -1.0F + body.rotateAngleY;
            rightArm.rotateAngleX = -0.1F + body.rotateAngleX;

            float x2 = leftArm.rotationPointX;
            leftArm.rotationPointX = (float) (x2 * cosTheta);
            leftArm.rotationPointZ = (float) (x2 * sinTheta);

            leftArm.rotateAngleY = -0.1F + body.rotateAngleY;
            leftArm.rotateAngleX = -1F + body.rotateAngleX;
        }

        @Override
        public void animateGunAim(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float aimProgress) {
            this.animateGunHold(entity, rightArm, leftArm, body, head);
        }
    };

    public static void registerDefault() {
        CACHE.put(RESERVED_DEFAULT_NAME, DEFAULT);
        CACHE.put(MINI_GUN_NAME, MINI_GUN);
    }

    public static void register(String name, IThirdPersonAnimation animation) {
        if (name.equals(RESERVED_DEFAULT_NAME)) {
            return;
        }
        CACHE.put(name, animation);
    }

    public static IThirdPersonAnimation getAnimation(String name) {
        return CACHE.getOrDefault(name, DEFAULT);
    }
}
