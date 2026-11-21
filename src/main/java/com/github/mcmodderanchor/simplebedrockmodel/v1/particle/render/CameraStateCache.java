package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.tacz.guns.legacy.client.FovContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = SimpleBedrockModel.EVENT_MOD_ID, value = Side.CLIENT)
public final class CameraStateCache {

    private static float cameraRoll;
    private static float worldFov = 70.0f;
    private static float handFov = 70.0f;

    private CameraStateCache() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(EntityViewRenderEvent.CameraSetup event) {
        cameraRoll = event.getRoll();
    }

    /**
     * 缓存本帧最终使用的世界/手部投影 FOV。手部模型可使用独立 FOV，第一人称
     * 粒子从手部空间转换到世界空间时必须补偿二者的深度比例。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeFov(EntityViewRenderEvent.FOVModifier event) {
        // 1.12.2: FOVModifier does not say which projection it is for; GameRendererMixin records it in FovContext
        if (FovContext.usedConfiguredFov()) {
            worldFov = (float) event.getFOV();
        } else {
            handFov = (float) event.getFOV();
        }
    }

    /**
     * 获取当前帧的 camera roll（度）。
     */
    public static float getCameraRollDegrees() {
        return cameraRoll;
    }

    /**
     * 获取当前帧的 camera roll（弧度）。
     */
    public static float getCameraRollRadians() {
        return (float) Math.toRadians(cameraRoll);
    }

    /**
     * 返回从手部投影空间映射到世界投影空间所需的视图 Z 轴缩放。
     */
    public static float getFirstPersonToWorldDepthScale() {
        if (handFov <= 0.0f || worldFov <= 0.0f) {
            return 1.0f;
        }
        double handTan = Math.tan(Math.toRadians(handFov) / 2.0);
        double worldTan = Math.tan(Math.toRadians(worldFov) / 2.0);
        if (Math.abs(worldTan) < 1.0e-6) {
            return 1.0f;
        }
        return (float) (handTan / worldTan);
    }

    /** Current world-particle size multiplier for matching the hand projection. */
    public static float getWorldParticleScale() {
        float handToWorld = getFirstPersonToWorldDepthScale();
        return Float.isFinite(handToWorld) && handToWorld > 1.0e-4f
                ? 1.0f / handToWorld : 1.0f;
    }
}
