package com.tacz.guns.legacy.util;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

/**
 * 1.20.1 {@code net.minecraft.client.Camera} ({@code Minecraft#gameRenderer.getMainCamera()}): the render camera of
 * the current frame. 1.12.2 has no camera object; this one is filled from the last world pass
 * {@link EntityViewRenderEvent.CameraSetup} (lowest priority, so camera animations are included, as 1.20 Forge feeds
 * {@code ComputeCameraAngles} back into the camera) and {@link ActiveRenderInfo}.
 * <p>
 * Angles follow 1.20: {@link #getYRot()} is the entity yaw (the vanilla CameraSetup yaw is that plus 180) and the
 * mirrored third-person view turns by 180 degrees and negates the pitch.
 * <p>
 * Note that 1.12.2 world rendering is relative to the render view entity's interpolated feet position
 * ({@code RenderManager#viewerPosX}), not to {@link #getPosition()}.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Side.CLIENT)
public final class Camera {
    private static final Camera MAIN = new Camera();

    private float xRot;
    private float yRot;
    private float roll;
    private float partialTicks;
    private boolean detached;
    private final Quaternionf rotation = new Quaternionf();

    private Camera() {
    }

    public static Camera getMainCamera() {
        return MAIN;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        int view = Minecraft.getMinecraft().gameSettings.thirdPersonView;
        boolean mirrored = view == 2;
        MAIN.yRot = event.getYaw() - 180.0F + (mirrored ? 180.0F : 0.0F);
        MAIN.xRot = mirrored ? -event.getPitch() : event.getPitch();
        MAIN.roll = mirrored ? -event.getRoll() : event.getRoll();
        MAIN.partialTicks = (float) event.getRenderPartialTicks();
        MAIN.detached = view != 0;
        MAIN.rotation.rotationYXZ(-MAIN.yRot * ((float) Math.PI / 180F), MAIN.xRot * ((float) Math.PI / 180F), 0.0F);
    }

    /**
     * World position of the eye (the render view entity's interpolated position plus the third-person offset).
     */
    public Vec3d getPosition() {
        Entity entity = getEntity();
        if (entity == null) {
            return Vec3d.ZERO;
        }
        return ActiveRenderInfo.projectViewFromEntity(entity, partialTicks);
    }

    @Nullable
    public Entity getEntity() {
        return Minecraft.getMinecraft().getRenderViewEntity();
    }

    public float getXRot() {
        return xRot;
    }

    public float getYRot() {
        return yRot;
    }

    /**
     * Roll of the last {@code CameraSetup} event in degrees (1.20 keeps it out of the camera, see SBM
     * {@code CameraStateCache}).
     */
    public float getRoll() {
        return roll;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public boolean isDetached() {
        return detached;
    }

    /**
     * 1.20 {@code Camera#rotation()}: {@code rotationYXZ(-yRot, xRot, 0)} in radians. Do not modify.
     */
    public Quaternionf rotation() {
        return rotation;
    }
}
