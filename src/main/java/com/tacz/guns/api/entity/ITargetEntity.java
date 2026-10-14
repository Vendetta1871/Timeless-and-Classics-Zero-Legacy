package com.tacz.guns.api.entity;

import net.minecraft.util.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;

/**
 * 用于进行一些并非 {@link EntityLivingBase} 但是可被子弹击中的特殊实体的处理
 */
public interface ITargetEntity {
    /**
     * @param projectile 弹射物实体
     * @param result     击中实体的位置
     * @param source     伤害源类型
     * @param damage     伤害值
     */
    void onProjectileHit(Entity projectile, RayTraceResult result, DamageSource source, float damage);
}
