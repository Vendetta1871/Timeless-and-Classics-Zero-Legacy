package com.tacz.guns.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.ZoomConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.compat.AquaAcrobaticsCompat;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * 1.12.2: {@code MouseHandler#turnPlayer} is inlined into {@code EntityRenderer#updateCameraAndRender}, which calls
 * {@code EntityPlayerSP#turn(float, float)} (normal and smooth camera). Cleanroom (0.7.x) patches those calls into
 * the static {@code InputHooks.onMouseTurn(EntityPlayerSP, float, float)}; the wrapped operation has the same shape
 * either way, so both targets share this handler (the injector needs one of them to match).
 */
@Mixin(EntityRenderer.class)
public class MouseHandlerMixin {
    @WrapOperation(method = "updateCameraAndRender(FJ)V", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"),
            @At(value = "INVOKE", target = "Lcom/cleanroommc/client/input/InputHooks;onMouseTurn(Lnet/minecraft/client/entity/EntityPlayerSP;FF)V", remap = false)
    })
    public void reduceSensitivity(EntityPlayerSP player, float yaw, float pitch, Operation<Void> original) {
        ItemStack mainHandItem = player.getHeldItemMainhand();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            original.call(player, yaw, pitch);
            return;
        }
        ResourceLocation scopeId = iGun.getAttachmentId(mainHandItem, AttachmentType.SCOPE);
        if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeId = iGun.getBuiltInAttachmentId(mainHandItem, AttachmentType.SCOPE);
        }
        float zoomLevel = 1;
        if (DefaultAssets.isEmptyAttachmentId(scopeId)) {
            // 缩放倍率
            zoomLevel = TimelessAPI.getGunDisplay(mainHandItem).map(GunDisplayInstance::getIronZoom).orElse(1f);
        } else {
            Optional<ClientAttachmentIndex> optional = TimelessAPI.getClientAttachmentIndex(scopeId);
            if (optional.isPresent()) {
                float[] zoom = optional.get().getZoom();
                if (zoom != null && zoom.length > 0) {
                    NBTTagCompound attachmentTag = iGun.getAttachmentTag(mainHandItem, AttachmentType.SCOPE);
                    zoomLevel = zoom[AttachmentItemDataAccessor.getZoomNumberFromTag(attachmentTag) % zoom.length];
                }
            }
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        float progress = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
        float fovProgress = (float) Easing.easeInExpo(progress);
        // 开镜灵敏度系数
        double sensitivityMultiplier = ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER.get();
        sensitivityMultiplier = 1 + (sensitivityMultiplier - 1) * progress;
        // 两种状态下的 fov 计算
        double originalFov = minecraft.gameSettings.fovSetting;
        double currentFov = MathUtil.magnificationToFov(1 + (zoomLevel - 1) * fovProgress, originalFov);
        // 荧幕距离系数，MC 和 COD 一样使用 MDV 标准，默认为 MDV133（系数为 1.33）
        double coefficient = ZoomConfig.SCREEN_DISTANCE_COEFFICIENT.get();
        double denominator = MathUtil.zoomSensitivityRatio(currentFov, originalFov, coefficient) * sensitivityMultiplier;
        // 最终结果
        double finalYaw = yaw * denominator;
        double finalPitch = getCrawlPitch(player, pitch * denominator);
        original.call(player, (float) finalYaw, (float) finalPitch);
    }

    @Unique
    private static double getCrawlPitch(EntityPlayerSP player, double pitch) {
        // 1.12.2: no swimming pose; crawling comes from Aqua Acrobatics
        boolean crawling = AquaAcrobaticsCompat.isCrawling(player);
        if (!crawling) {
            return pitch;
        }
        double minPitch = -SyncConfig.CRAWL_MAX_UP_ANGLE.get();
        double maxPitch = SyncConfig.CRAWL_MAX_DOWN_ANGLE.get();
        double currentPitch = player.rotationPitch;
        double minInput = Math.min(0, (minPitch - currentPitch) / 0.15F);
        double maxInput = Math.max(0, (maxPitch - currentPitch) / 0.15F);
        return MathHelper.clamp(pitch, minInput, maxInput);
    }
}
