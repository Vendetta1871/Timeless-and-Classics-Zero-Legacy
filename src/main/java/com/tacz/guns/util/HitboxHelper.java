package com.tacz.guns.util;

import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.config.common.OtherConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.WeakHashMap;

public final class HitboxHelper {
    // 玩家位置缓存表
    private static final WeakHashMap<EntityPlayer, LinkedList<Vec3d>> PLAYER_POSITION = new WeakHashMap<>();
    // 玩家命中箱缓存表
    private static final WeakHashMap<EntityPlayer, LinkedList<AxisAlignedBB>> PLAYER_HITBOXES = new WeakHashMap<>();
    // 玩家速度缓存表
    private static final WeakHashMap<EntityPlayer, LinkedList<Vec3d>> PLAYER_VELOCITY = new WeakHashMap<>();
    // 命中箱缓存 Tick 上限
    private static final int SAVE_TICK = MathHelper.floor(OtherConfig.SERVER_HITBOX_LATENCY_MAX_SAVE_MS.get() / 1000 * 20 + 0.5);

    public static void onPlayerTick(EntityPlayer player) {
        if (player.isSpectator()) {
            PLAYER_POSITION.remove(player);
            PLAYER_HITBOXES.remove(player);
            PLAYER_VELOCITY.remove(player);
            return;
        }
        LinkedList<Vec3d> positions = PLAYER_POSITION.computeIfAbsent(player, p -> new LinkedList<>());
        LinkedList<AxisAlignedBB> boxes = PLAYER_HITBOXES.computeIfAbsent(player, p -> new LinkedList<>());
        LinkedList<Vec3d> velocities = PLAYER_VELOCITY.computeIfAbsent(player, p -> new LinkedList<>());
        positions.addFirst(player.getPositionVector());
        boxes.addFirst(player.getEntityBoundingBox());
        velocities.addFirst(getPlayerVelocity(player));
        // Position 用于速度计算，所以只需要缓存 2 个位置
        if (positions.size() > 2) {
            positions.removeLast();
        }
        // 命中箱和速度缓存数量限制
        if (boxes.size() > SAVE_TICK) {
            boxes.removeLast();
            velocities.removeLast();
        }
    }

    public static void onPlayerLoggedOut(EntityPlayer player) {
        PLAYER_POSITION.remove(player);
        PLAYER_HITBOXES.remove(player);
        PLAYER_VELOCITY.remove(player);
    }

    public static Vec3d getPlayerVelocity(EntityPlayer entity) {
        LinkedList<Vec3d> positions = PLAYER_POSITION.computeIfAbsent(entity, player -> new LinkedList<>());
        if (positions.size() > 1) {
            Vec3d currPos = positions.getFirst();
            Vec3d prevPos = positions.getLast();
            return new Vec3d(currPos.x - prevPos.x, currPos.y - prevPos.y, currPos.z - prevPos.z);
        }
        return new Vec3d(0, 0, 0);
    }

    public static AxisAlignedBB getBoundingBox(EntityPlayer entity, int ping) {
        if (PLAYER_HITBOXES.containsKey(entity)) {
            LinkedList<AxisAlignedBB> boxes = PLAYER_HITBOXES.get(entity);
            int index = MathHelper.clamp(ping, 0, boxes.size() - 1);
            return boxes.get(index);
        }
        return entity.getEntityBoundingBox();
    }

    public static Vec3d getVelocity(EntityPlayer entity, int ping) {
        if (PLAYER_VELOCITY.containsKey(entity)) {
            LinkedList<Vec3d> velocities = PLAYER_VELOCITY.get(entity);
            int index = MathHelper.clamp(ping, 0, velocities.size() - 1);
            return velocities.get(index);
        }
        return getPlayerVelocity(entity);
    }

    public static AxisAlignedBB getFixedBoundingBox(Entity entity, Entity owner) {
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox();
        Vec3d velocity = new Vec3d(entity.posX - entity.lastTickPosX, entity.posY - entity.lastTickPosY, entity.posZ - entity.lastTickPosZ);
        // hitbox 延迟补偿。只有射击者是玩家（且被击中者也是玩家）才进行此类延迟补偿计算
        if (OtherConfig.SERVER_HITBOX_LATENCY_FIX.get() && entity instanceof EntityPlayerMP player && owner instanceof EntityPlayerMP serverPlayerOwner) {
            int ping = MathHelper.floor((serverPlayerOwner.ping / 1000.0) * 20.0 + 0.5);
            boundingBox = getBoundingBox(player, ping);
            velocity = getVelocity(player, ping);
        }
        // 应用蹲伏导致的 hitbox 变形
        double expandHeight = entity instanceof EntityPlayer && !entity.isSneaking() ? 0.0625 : 0.0;
        boundingBox = boundingBox.expand(0, expandHeight, 0);
        // 根据速度一定程度地扩展 hitbox
        boundingBox = boundingBox.expand(velocity.x, velocity.y, velocity.z);
        // 玩家 hitbox 修正，可以通过 Config 调整
        double playerHitboxOffset = OtherConfig.SERVER_HITBOX_OFFSET.get();
        if (entity instanceof EntityPlayerMP) {
            if (entity.getRidingEntity() != null) {
                boundingBox = boundingBox.offset(velocity.scale(playerHitboxOffset / 2));
            }
            boundingBox = boundingBox.offset(velocity.scale(playerHitboxOffset));
        }
        // 给所有实体统一应用的 Hitbox 偏移，其数值为实验得出的定值。
        if (entity.getRidingEntity() != null || entity instanceof ITargetEntity) {
            boundingBox = boundingBox.offset(velocity.scale(-2.5));
        }
        boundingBox = boundingBox.offset(velocity.scale(-5));
        return boundingBox;
    }
}
