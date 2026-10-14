package com.tacz.guns.client.renderer.entity;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.RenderBuffers;
import com.tacz.guns.legacy.util.LegacyMath;
import com.tacz.guns.legacy.util.Optionals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.entity.EntityPlayerSP;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.RenderType;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import com.tacz.guns.legacy.render.OverlayTexture;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

/**
 * 1.12.2: a {@link Render}; {@link #doRender} moves the GL matrix to the bullet and calls upstream's
 * {@link #render} with a fresh pose stack and the bullet's light (block light forced to 15, see
 * {@link #getBlockLightLevel}).
 */
public class EntityBulletRenderer extends Render<EntityKineticBullet> {
    public EntityBulletRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityKineticBullet bullet, double x, double y, double z, float entityYaw, float partialTicks) {
        int sky = LightTexture.sky(bullet.getBrightnessForRender());
        int packedLight = LightTexture.pack(getBlockLightLevel(bullet, new BlockPos(bullet)), sky);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        MultiBufferSource.BufferSource buffer = RenderBuffers.bufferSource();
        render(bullet, entityYaw, partialTicks, new PoseStack(), buffer, packedLight);
        buffer.endBatch();
        GlStateManager.popMatrix();
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.DEFAULT_BULLET_MODEL);
    }

    public void render(EntityKineticBullet bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ResourceLocation gunId = bullet.getGunId();
        ResourceLocation gunDisplayId = bullet.getGunDisplayId();
        Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(gunDisplayId, gunId);
        if (!display.isPresent()) {
            return;
        }
        float @Nullable [] tracerColor = bullet.getTracerColorOverride().orElse(display.get().getTracerColor());
        ResourceLocation ammoId = bullet.getAmmoId();
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(ammoIndex -> {
            BedrockAmmoModel ammoEntityModel = ammoIndex.getAmmoEntityModel();
            ResourceLocation textureLocation = ammoIndex.getAmmoEntityTextureLocation();
            if (ammoEntityModel != null && textureLocation != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(LegacyMath.lerp(partialTicks, bullet.prevRotationYaw, bullet.rotationYaw) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(LegacyMath.lerp(partialTicks, bullet.prevRotationPitch, bullet.rotationPitch)));
                poseStack.pushPose();
                poseStack.translate(0, 1.5, 0);
                poseStack.scale(-1, -1, 1);
                ammoEntityModel.render(poseStack, ItemCameraTransforms.TransformType.GROUND, RenderType.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }

            // 曳光弹发光
            if (bullet.isTracerAmmo()) {
                float[] actualTracerColor = tracerColor != null ? tracerColor : Objects.requireNonNull(ammoIndex.getTracerColor());
                renderTracerAmmo(bullet, actualTracerColor, partialTicks, poseStack, packedLight);
            }
        });
    }

    public void renderTracerAmmo(EntityKineticBullet bullet, float[] tracerColor, float partialTicks, PoseStack poseStack, int packedLight) {
        getModel().ifPresent(model -> {
            Entity shooter = bullet.getOwner();
            if (shooter == null) {
                return;
            }
            boolean isFirstPerson = this.renderManager.options.thirdPersonView == 0 && shooter instanceof EntityPlayerSP;
            if (isFirstPerson && !RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE.get()) {
                return;
            }
            poseStack.pushPose();
            {
                float width = 0.005f;
                Vec3d bulletPosition = new Vec3d(LegacyMath.lerp(partialTicks, bullet.lastTickPosX, bullet.posX),
                        LegacyMath.lerp(partialTicks, bullet.lastTickPosY, bullet.posY), LegacyMath.lerp(partialTicks, bullet.lastTickPosZ, bullet.posZ));
                double trailLength = 0.85 * Math.sqrt(bullet.motionX * bullet.motionX + bullet.motionY * bullet.motionY + bullet.motionZ * bullet.motionZ);
                double disToEye = bulletPosition.distanceTo(shooter.getPositionEyes(partialTicks));
                trailLength = Math.min(trailLength, disToEye * 0.8);

                if (isFirstPerson) {
                    // 第一人称渲染自己的曳光弹的时候需要应用偏移
                    // 1.12.2: in first person the camera looks along the view entity's interpolated rotation
                    Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
                    Vector3f offset = bullet.getFirstPersonRenderOffset();
                    if (offset == null && camera != null) {
                        offset = new Vector3f(GunItemRendererWrapper.muzzleRenderOffset);
                        bullet.setCameraXRot(LegacyMath.lerp(partialTicks, camera.prevRotationPitch, camera.rotationPitch));
                        bullet.setCameraYRot(LegacyMath.lerp(partialTicks, camera.prevRotationYaw, camera.rotationYaw));
                        bullet.setFirstPersonRenderOffset(offset);
                    }
                    if (offset == null) {
                        offset = new Vector3f();
                    }
                    // 按照生存时间减少曳光弹的偏移，避免渲染位置距离落点太远
                    double offsetReducer = Math.max(0, (50 - disToEye)) / 50;
                    // 摄像机旋转
                    poseStack.mulPose(Axis.YN.rotationDegrees(bullet.getCameraYRot() + 180f));
                    poseStack.mulPose(Axis.XN.rotationDegrees(bullet.getCameraXRot()));
                    // 应用偏移
                    poseStack.translate(offset.x * offsetReducer, offset.y * offsetReducer, offset.z * offsetReducer);
                    // 逆转摄像机旋转
                    poseStack.mulPose(Axis.XP.rotationDegrees(bullet.getCameraXRot()));
                    poseStack.mulPose(Axis.YP.rotationDegrees(bullet.getCameraYRot() + 180f));
                }
                // 说是 override 其实默认值是 1
                // 所以这里直接乘也没关系
                width *= bullet.getTracerSizeOverride();
                width *= (float) Math.max(1.0, disToEye / 3.5);
                poseStack.mulPose(Axis.YP.rotationDegrees(LegacyMath.lerp(partialTicks, bullet.prevRotationYaw, bullet.rotationYaw) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(LegacyMath.lerp(partialTicks, bullet.prevRotationPitch, bullet.rotationPitch)));
                poseStack.translate(0, isFirstPerson ? 0 : -0.2, trailLength / 2.0);
                poseStack.scale(width, width, (float) trailLength);
                // 距离两格外才渲染，只在前 5 tick 判定
                double bulletDistance = bulletPosition.distanceTo(shooter.getPositionEyes(1.0F));
                if (bullet.ticksExisted >= 5 || bulletDistance > 2) {
                    RenderType type = RenderType.energySwirl(InternalAssetLoader.DEFAULT_BULLET_TEXTURE, 15, 15);
                    model.render(poseStack, ItemCameraTransforms.TransformType.NONE, type, packedLight, OverlayTexture.NO_OVERLAY,
                            tracerColor[0], tracerColor[1], tracerColor[2], 1);
                }
            }
            poseStack.popPose();
        });
    }

    protected int getBlockLightLevel(@NotNull EntityKineticBullet entityBullet, @NotNull BlockPos blockPos) {
        return 15;
    }

    @Override
    public boolean shouldRender(EntityKineticBullet bullet, ICamera camera, double pCamX, double pCamY, double pCamZ) {
        AxisAlignedBB aabb = bullet.getRenderBoundingBox().grow(0.5);
        if (aabb.hasNaN() || aabb.getAverageEdgeLength() == 0) {
            aabb = new AxisAlignedBB(bullet.posX - 2.0, bullet.posY - 2.0, bullet.posZ - 2.0, bullet.posX + 2.0, bullet.posY + 2.0, bullet.posZ + 2.0);
        }
        return camera.isBoundingBoxInFrustum(aabb);
    }

    @Override
    public ResourceLocation getEntityTexture(@NotNull EntityKineticBullet entity) {
        return null;
    }
}
