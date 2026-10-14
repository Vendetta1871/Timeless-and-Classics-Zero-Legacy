package com.tacz.guns.legacy.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * The subset of 1.20.1 {@code com.mojang.blaze3d.systems.RenderSystem} TACZ uses, mapped to 1.12.2
 * {@link GlStateManager} (raw GL where 1.12.2 has no state-managed call, e.g. stencil).
 */
@SideOnly(Side.CLIENT)
public final class RenderSystem {
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    private RenderSystem() {
    }

    /**
     * Multiplies the current GL matrix by {@code matrix}: lets code that only has a {@link PoseStack} drive 1.12.2
     * renderers that read the GL matrix stack (fonts, vanilla models).
     */
    public static void multMatrix(Matrix4f matrix) {
        MATRIX_BUFFER.clear();
        matrix.get(MATRIX_BUFFER);
        GlStateManager.multMatrix(MATRIX_BUFFER);
    }

    public static void assertOnRenderThread() {
    }

    public static void stencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    public static void stencilOp(int sfail, int dpfail, int dppass) {
        GL11.glStencilOp(sfail, dpfail, dppass);
    }

    public static void stencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    public static void clearStencil(int index) {
        GL11.glClearStencil(index);
    }

    public static void clear(int mask, boolean checkError) {
        GlStateManager.clear(mask);
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlStateManager.colorMask(red, green, blue, alpha);
    }

    public static void depthMask(boolean flag) {
        GlStateManager.depthMask(flag);
    }

    public static void enableDepthTest() {
        GlStateManager.enableDepth();
    }

    public static void disableDepthTest() {
        GlStateManager.disableDepth();
    }

    public static void depthFunc(int func) {
        GlStateManager.depthFunc(func);
    }

    public static void enableBlend() {
        GlStateManager.enableBlend();
    }

    public static void disableBlend() {
        GlStateManager.disableBlend();
    }

    public static void blendFunc(GlStateManager.SourceFactor source, GlStateManager.DestFactor dest) {
        GlStateManager.blendFunc(source, dest);
    }

    public static void blendFuncSeparate(GlStateManager.SourceFactor srcColor, GlStateManager.DestFactor dstColor,
                                         GlStateManager.SourceFactor srcAlpha, GlStateManager.DestFactor dstAlpha) {
        GlStateManager.tryBlendFuncSeparate(srcColor, dstColor, srcAlpha, dstAlpha);
    }

    public static void defaultBlendFunc() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    public static void enableCull() {
        GlStateManager.enableCull();
    }

    public static void disableCull() {
        GlStateManager.disableCull();
    }

    public static void setShaderColor(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void setShaderTexture(int unit, ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }
}
