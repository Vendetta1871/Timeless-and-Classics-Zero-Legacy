package com.tacz.guns.client.resource.pojo.display.gun;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.HitboxHelper;
import com.tacz.guns.util.block.BlockRayTrace;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.util.ClipContext;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@SideOnly(Side.CLIENT)
public final class RangefinderManager {
    private static final String MISS_TEXT = "---";
    private static final double ENTITY_XZ_EXPAND = 0.25;
    private static final double ENTITY_Y_EXPAND = 8.0;
    private static final long MILLIS_PER_TICK = 50L;
    private static final Predicate<Entity> RANGEFINDER_TARGETS = entity -> entity != null && entity.canBeCollidedWith() && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator()) && entity.isEntityAlive();

    private static Rangefinder currentConfig = null;
    private static long lastUpdateTimestamp = 0L;
    private static int lastRefreshTick = 2;
    private static String text = MISS_TEXT;
    private static boolean hitEntity = false;
    private static double hitDistance = 0.0;
    private static float impactProjectionY = 0.0f;
    private static int impactPredictionColor = 0xFFFFFFFF;
    private static float impactPredictionScale = 1.0f;
    private static boolean showImpactPrediction = false;

    private RangefinderManager() {
    }

    public static void resetTimestamps() {
        currentConfig = null;
        lastUpdateTimestamp = 0L;
        lastRefreshTick = 2;
        clearResult();
    }

    public static void setCurrentConfig(@Nullable Rangefinder config) {
        currentConfig = config;
    }

    public static void clearCurrentConfig() {
        currentConfig = null;
    }

    public static String getText(ItemStack stack) {
        if (currentConfig == null) {
            return "";
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || stack == null || stack.isEmpty()) {
            lastUpdateTimestamp = 0L;
            clearResult();
            return MISS_TEXT;
        }
        int refreshTick = currentConfig.getRefreshTick();
        long now = GunTime.nowMillis();
        if (lastUpdateTimestamp != 0L && GunTime.isWithin(lastUpdateTimestamp, refreshTick * MILLIS_PER_TICK) && refreshTick == lastRefreshTick) {
            return text;
        }
        lastUpdateTimestamp = now;
        lastRefreshTick = refreshTick;
        updateResult(player, stack, currentConfig);
        return text;
    }

    public static boolean shouldRenderImpactPrediction() {
        if (!showImpactPrediction) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || !GunTime.isWithin(lastUpdateTimestamp, (lastRefreshTick + 1L) * MILLIS_PER_TICK)) {
            showImpactPrediction = false;
            return false;
        }
        if (!(player instanceof IClientPlayerGunOperator operator) || operator.getClientAimingProgress(mc.getRenderPartialTicks()) < 0.9f) {
            showImpactPrediction = false;
            return false;
        }
        return true;
    }

    public static float getImpactProjectionY() {
        return impactProjectionY;
    }

    public static int getImpactPredictionColor() {
        return impactPredictionColor;
    }

    public static float getImpactPredictionScale() {
        return impactPredictionScale;
    }

    private static void updateResult(EntityPlayerSP player, ItemStack stack, Rangefinder config) {
        impactPredictionColor = config.getImpactPredictionColor();
        impactPredictionScale = config.getImpactPredictionScale();
        if (!(stack.getItem() instanceof IGun iGun) || !(player instanceof IClientPlayerGunOperator operator)) {
            clearResult();
            return;
        }
        if (operator.getClientAimingProgress(Minecraft.getMinecraft().getRenderPartialTicks()) < 0.9f) {
            clearResult();
            return;
        }

        float maxDistance = config.getMaxDistance();
        Vec3d start = player.getPositionEyes(Minecraft.getMinecraft().getRenderPartialTicks());
        Vec3d look = player.getLook(Minecraft.getMinecraft().getRenderPartialTicks());
        Vec3d end = start.add(look.scale(maxDistance));

        RayTraceResult blockResult = BlockRayTrace.rayTraceBlocks(player.world, new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDistance = blockResult == null || blockResult.typeOfHit == RayTraceResult.Type.MISS ? Double.MAX_VALUE : start.distanceTo(blockResult.hitVec);
        EntityHit entityHit = findEntityHit(player, start, end, blockDistance).orElse(null);

        if (entityHit != null) {
            double distance = start.distanceTo(entityHit.hitPos());
            setDistanceText(distance, true, maxDistance);
            updateImpactPrediction(player, stack, iGun, config, distance);
        } else if (blockDistance != Double.MAX_VALUE) {
            setDistanceText(blockDistance, false, maxDistance);
            showImpactPrediction = false;
        } else {
            clearResult();
        }
    }

    private static Optional<EntityHit> findEntityHit(EntityPlayerSP player, Vec3d start, Vec3d end, double maxDistance) {
        AxisAlignedBB scanBox = new AxisAlignedBB(start, end).grow(ENTITY_XZ_EXPAND, ENTITY_Y_EXPAND, ENTITY_XZ_EXPAND);
        return player.world.getEntitiesInAABBexcluding(player, scanBox, RANGEFINDER_TARGETS::test)
                .stream()
                .filter(entity -> !entity.equals(player))
                .filter(entity -> !entity.isRidingSameEntity(player))
                .map(entity -> HitboxHelper.getFixedBoundingBox(entity, player).grow(ENTITY_XZ_EXPAND, ENTITY_Y_EXPAND, ENTITY_XZ_EXPAND).calculateIntercept(start, end))
                .filter(Objects::nonNull)
                .map(result -> new EntityHit(result.hitVec))
                .filter(hit -> hit != null && start.distanceTo(hit.hitPos()) <= maxDistance)
                .min(Comparator.comparingDouble(hit -> start.distanceTo(hit.hitPos())));
    }

    private static void setDistanceText(double distance, boolean entityHit, float maxDistance) {
        hitDistance = distance;
        hitEntity = entityHit;
        int roundedDistance = MathHelper.floor(distance + 0.5);
        // 与距离使用相同的取整规则，按测距上限的位数补零。
        int digits = Integer.toString(MathHelper.floor(maxDistance + 0.5)).length();
        String distanceText = Integer.toString(roundedDistance);
        text = StringUtils.repeat("0", Math.max(0, digits - distanceText.length())) + distanceText;
    }

    private static void clearResult() {
        text = MISS_TEXT;
        hitEntity = false;
        hitDistance = 0.0;
        impactProjectionY = 0.0f;
        showImpactPrediction = false;
    }

    private static void updateImpactPrediction(EntityPlayerSP player, ItemStack stack, IGun iGun, Rangefinder config, double distance) {
        showImpactPrediction = false;
        if (!hitEntity || !config.isShowImpactPrediction() || MISS_TEXT.equals(text) || distance <= 0.0) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(stack);
        ClientGunIndex gunIndex = TimelessAPI.getClientGunIndex(gunId).orElse(null);
        if (gunIndex == null) {
            return;
        }
        GunData gunData = gunIndex.getGunData();
        if (gunData == null || gunData.getBulletData() == null) {
            return;
        }
        BulletData bulletData = gunData.getBulletData();
        float speed = getAmmoSpeed(player, stack, bulletData);
        if (speed <= 0.0f) {
            return;
        }
        Vec3d look = player.getLook(Minecraft.getMinecraft().getRenderPartialTicks());
        double drop = calculateDrop(distance, look.y, speed / 20.0f, bulletData.getGravity(), bulletData.getFriction());
        if (!Double.isFinite(drop)) {
            return;
        }

        float fov = getCurrentFov(player, stack, iGun);
        double angle = Math.atan2(-drop, Math.max(distance, 0.001));
        impactProjectionY = (float) (Math.tan(angle) / Math.tan(Math.toRadians(fov) / 2.0));
        showImpactPrediction = Float.isFinite(impactProjectionY);
    }

    private static float getAmmoSpeed(EntityPlayerSP player, ItemStack stack, BulletData bulletData) {
        float speed = bulletData.getSpeed();
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty != null) {
            try {
                speed = cacheProperty.getCache(GunProperties.AMMO_SPEED);
            } catch (RuntimeException ignored) {
                speed = bulletData.getSpeed();
            }
        }
        return Math.max(0.0f, speed * AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get().floatValue());
    }

    private static float getCurrentFov(EntityPlayerSP player, ItemStack stack, IGun iGun) {
        float configuredFov = Minecraft.getMinecraft().gameSettings.fovSetting;
        float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getMinecraft().getRenderPartialTicks());
        AdsFov.Channel curve = TimelessAPI.getGunDisplay(stack)
                .map(GunDisplayInstance::getAdsFov).map(AdsFov::getWorld).orElse(null);
        float fovProgress = curve == null ? (float) Easing.easeInExpo(aimingProgress)
                : curve.evaluate(aimingProgress);
        float zoom = iGun.getAimingZoom(stack);
        float targetFov = (float) MathUtil.magnificationToFov(1 + (zoom - 1) * fovProgress, configuredFov);
        if (curve != null && curve.isSmooth()) {
            float actualFov = CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            if (Float.isFinite(actualFov) && actualFov > 0) {
                return actualFov;
            }
        }
        return targetFov;
    }

    private static double calculateDrop(double distance, double lookY, float speedPerTick, float gravity, float friction) {
        double traveled = 0.0;
        double vertical = 0.0;
        double speed = speedPerTick;
        double verticalVelocity = lookY * speedPerTick;
        int maxTicks = 20 * 30;
        for (int i = 0; i < maxTicks && traveled < distance && speed > 1.0E-4; i++) {
            double step = Math.min(speed, distance - traveled);
            double ratio = step / speed;
            vertical += verticalVelocity * ratio;
            traveled += step;
            speed *= 1.0f - friction;
            verticalVelocity = verticalVelocity * (1.0f - friction) - gravity;
        }
        return vertical - lookY * distance;
    }

    private static final class EntityHit {
        private final Vec3d hitPos;

        public EntityHit(Vec3d hitPos) {
            this.hitPos = hitPos;
        }

        public Vec3d hitPos() {
            return hitPos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntityHit)) return false;
            EntityHit that = (EntityHit) o;
            return java.util.Objects.equals(hitPos, that.hitPos);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(hitPos);
        }

        @Override
        public String toString() {
            return "EntityHit[" + "hitPos=" + hitPos + "]";
        }

    }
}
