package com.tacz.guns.api.client.other;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;

public interface IThirdPersonAnimation {
    /**
     * 第三人称动画：主手持有枪械时
     *
     * @param entity   持有枪械的实体
     * @param rightArm 右手模型
     * @param leftArm  左手模型
     * @param head     头部模型
     */
    void animateGunHold(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head);

    /**
     * 第三人称动画：枪械瞄准时
     *
     * @param entity      持有枪械的实体
     * @param rightArm    右手模型
     * @param leftArm     左手模型
     * @param head        头部模型
     * @param aimProgress 瞄准进度 0-1
     */
    void animateGunAim(EntityLivingBase entity, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer body, ModelRenderer head, float aimProgress);
}
