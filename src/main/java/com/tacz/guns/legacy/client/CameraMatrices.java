package com.tacz.guns.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * 1.12.2 only: the camera's part of the transform lives on the GL modelview matrix (set up by
 * {@code EntityRenderer#setupCameraTransform}), not in a pose stack. This keeps the camera modelview of the current
 * world pass, recorded right after the camera is set up, so an eye-space matrix (GL modelview times a pose) can be
 * turned back into world space, as 1.20 code does with {@code Camera}.
 */
@SideOnly(Side.CLIENT)
public final class CameraMatrices {
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f INVERSE_VIEW = new Matrix4f();

    private CameraMatrices() {
    }

    /**
     * Called by {@code GameRendererMixin} once the camera transform of a world pass is on the GL modelview.
     */
    public static void record() {
        BUFFER.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, BUFFER);
        VIEW.set(BUFFER);
        VIEW.invert(INVERSE_VIEW);
    }

    /**
     * The camera modelview of the current world pass (world relative to the viewer position, to eye space).
     */
    public static Matrix4f getView() {
        return new Matrix4f(VIEW);
    }

    /**
     * Eye space (current GL modelview times a pose) to absolute world coordinates.
     */
    public static Matrix4f eyeToWorld(Matrix4f eyePose) {
        Matrix4f world = new Matrix4f(INVERSE_VIEW).mul(eyePose);
        RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
        world.m30(world.m30() + (float) renderManager.viewerPosX);
        world.m31(world.m31() + (float) renderManager.viewerPosY);
        world.m32(world.m32() + (float) renderManager.viewerPosZ);
        return world;
    }
}
