package com.tacz.guns.util;

import net.minecraft.entity.MultiPartEntityPart;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.scoreboard.Team;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Predicate;

public class EntityUtil {
    
    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.canBeCollidedWith() && EntitySelectors.NOT_SPECTATING.apply(input);
    @Nullable
    public static EntityKineticBullet.EntityResult findEntityOnPath(EntityKineticBullet bulletEntity, Vec3d startVec, Vec3d endVec) {
    
        Vec3d hitVec = null;
        Entity hitEntity = null;
        boolean headshot = false;
        // 获取子弹 tick 路径上所有的实体
        List<Entity> entities = bulletEntity.world.getEntitiesInAABBexcluding(bulletEntity, bulletEntity.getEntityBoundingBox().expand(bulletEntity.motionX, bulletEntity.motionY, bulletEntity.motionZ).grow(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            // 禁止对自己造成伤害（如有需要可以增加 Config 开启对自己的伤害）
            if (!entity.equals(owner)) {
                // 射击无视自己的载具和该载具上的其他乘客
                if (owner != null && entity.isRidingSameEntity(owner)) {
                    continue;
                }
                EntityKineticBullet.EntityResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                Vec3d hitPos = result.getHitPos();
                double distanceToHit = startVec.distanceTo(hitPos);
                if (entity.isEntityAlive()) {
                    if (distanceToHit < closestDistance) {
                        hitVec = hitPos;
                        hitEntity = entity;
                        closestDistance = distanceToHit;
                        headshot = result.isHeadshot();
                    }
                }
            }
        }
        return hitEntity != null ? new EntityKineticBullet.EntityResult(hitEntity, hitVec, headshot) : null;
    }
    
    @NotNull
    public static List<EntityKineticBullet.EntityResult> findEntitiesOnPath(EntityKineticBullet bulletEntity, Vec3d startVec, Vec3d endVec) {
        List<EntityKineticBullet.EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = bulletEntity.world.getEntitiesInAABBexcluding(bulletEntity, bulletEntity.getEntityBoundingBox().expand(bulletEntity.motionX, bulletEntity.motionY, bulletEntity.motionZ).grow(1.0), PROJECTILE_TARGETS);
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            if (!entity.equals(owner)) {
                if (owner != null && entity.equals(owner.getRidingEntity())) {
                    continue;
                }
                EntityKineticBullet.EntityResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                if (entity.isEntityAlive()) {
                    hitEntities.add(result);
                }
            }
        }
        return hitEntities;
    }
    
    @Nullable
    protected static EntityKineticBullet.EntityResult getHitResult(EntityKineticBullet bulletEntity, Entity entity, Vec3d startVec, Vec3d endVec) {
        if (isBulletDamageIgnored(entity)) {
            return null;
        }
        // Filter before picking the nearest hit, so an ignored ally cannot hide an enemy behind it.
        Entity owner = bulletEntity.getOwner();
        if (AmmoConfig.IGNORE_TEAM_FRIENDLY_FIRE.get() && owner != null) {
            Team team = owner.getTeam();
            if (team != null && team.isSameTeam(entity.getTeam()) && !team.getAllowFriendlyFire()) {
                return null;
            }
        }
        AxisAlignedBB boundingBox = HitboxHelper.getFixedBoundingBox(entity, bulletEntity.getOwner());
        // 计算射线与实体 boundingBox 的交点
        RayTraceResult intercept = boundingBox.calculateIntercept(startVec, endVec);
        Vec3d hitPos = intercept == null ? null : intercept.hitVec;
        // 爆头判定
        if (hitPos == null) {
            return null;
        }
        Vec3d hitBoxPos = hitPos.subtract(entity.getPositionVector());
        net.minecraftforge.fml.common.registry.EntityEntry entityEntry = EntityRegistry.getEntry(entity.getClass());
        ResourceLocation entityId = entityEntry == null ? null : entityEntry.getRegistryName();
        // 有配置的调用配置
        if (entityId != null) {
            AxisAlignedBB aabb = HeadShotAABBConfigRead.getAABB(entityId);
            if (aabb != null) {
                return new EntityKineticBullet.EntityResult(entity, hitPos, aabb.contains(hitBoxPos));
            }
        }
        // 没有配置的默认给一个
        boolean headshot = false;
        float eyeHeight = entity.getEyeHeight();
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25)) {
            headshot = true;
        }
        return new EntityKineticBullet.EntityResult(entity, hitPos, headshot);
    }

    public static boolean isBulletDamageIgnored(@Nullable Entity entity) {
        if (entity == null || !AmmoConfig.ENABLE_BULLET_DAMAGE_IGNORE_LIST.get()) {
            return false;
        }
        // 1.12.2: multipart entities are MultiPartEntityPart, entity ids come from EntityList
        Entity core = entity instanceof MultiPartEntityPart part && part.parent instanceof Entity parent ? parent : entity;
        ResourceLocation entityId = EntityList.getKey(core);
        return entityId != null && AmmoConfig.BULLET_DAMAGE_IGNORE_LIST.get().contains(entityId.toString());
    }
}
