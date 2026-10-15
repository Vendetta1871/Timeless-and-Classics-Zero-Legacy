package com.tacz.guns.util;

import com.tacz.guns.compat.ar.ARCompat;
import com.tacz.guns.compat.optifine.OptifineCompat;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@SideOnly(Side.CLIENT)
public final class RenderHelper {
    public static void blit(PoseStack poseStack, float x, float y, float uOffset, float vOffset, float pWidth, float height, float textureWidth, float textureHeight) {
        blit(poseStack, x, y, pWidth, height, uOffset, vOffset, pWidth, height, textureWidth, textureHeight);
    }

    private static void blit(PoseStack poseStack, float x, float y, float pWidth, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        innerBlit(poseStack, x, x + pWidth, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    private static void innerBlit(PoseStack poseStack, float x1, float x2, float y1, float y2, float blitOffset, float uWidth, float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight) {
        innerBlit(poseStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight);
    }

    /**
     * The texture is the one bound by the caller (upstream: {@code RenderSystem.setShaderTexture}).
     */
    private static void innerBlit(Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV) {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        vertex(bufferbuilder, matrix, x1, y2, blitOffset).tex(minU, maxV).endVertex();
        vertex(bufferbuilder, matrix, x2, y2, blitOffset).tex(maxU, maxV).endVertex();
        vertex(bufferbuilder, matrix, x2, y1, blitOffset).tex(maxU, minV).endVertex();
        vertex(bufferbuilder, matrix, x1, y1, blitOffset).tex(minU, minV).endVertex();
        Tessellator.getInstance().draw();
    }

    private static BufferBuilder vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z) {
        Vector4f vector = matrix.transform(new Vector4f(x, y, z, 1.0F));
        return builder.pos(vector.x(), vector.y(), vector.z());
    }

    public static void enableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread();
        // 1.12.2: without shaders OptiFine keeps the vanilla framebuffer, which Forge can give stencil bits. Forge's
        // enableStencil() re-creates the framebuffer on every call (it would wipe the frame), so only once.
        // TODO(port): OptiFine shaders (stage 2); upstream's depth-texture branch for OptiFine/Oculus, kept below
//        if (OptifineCompat.isOptifineInstalled()) {
//            // 以下代码用于应对 使用 optifine 的场景
//            int depthTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
//            int stencilTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
//            if (depthTextureId != GL11.GL_NONE && stencilTextureId == GL11.GL_NONE) {
//                GlStateManager.bindTexture(depthTextureId);
//                int dataType = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_DEPTH_TYPE);
//                if (dataType == GL30.GL_UNSIGNED_NORMALIZED) {
//                    int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
//                    int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
//                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, (java.nio.ByteBuffer) null);
//                    GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTextureId, 0);
//                }
//            }
//        }
        Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
        if (!framebuffer.isStencilEnabled()) {
            framebuffer.enableStencil();
        }
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    public static void disableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    /**
     * 1.12.2 arm models render through the GL matrix stack, so the pose is applied to GL around the call.
     */
    public static void renderFirstPersonArm(EntityPlayerSP player, EnumHandSide hand, PoseStack matrixStack, int combinedLight) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderPlayer renderer = (RenderPlayer) mc.getRenderManager().<net.minecraft.client.entity.AbstractClientPlayer>getEntityRenderObject(player);
        if (renderer == null) {
            return;
        }

		if (ARCompat.shouldAccelerate()) {
			ARCompat.setRenderingLevel();
		}

        mc.getTextureManager().bindTexture(player.getLocationSkin());
        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combinedLight & 0xFFFF, combinedLight >> 16 & 0xFFFF);
        GlStateManager.pushMatrix();
        RenderSystem.multMatrix(matrixStack.last().pose());
        GlStateManager.enableCull();
        if (hand == EnumHandSide.RIGHT) {
            renderer.renderRightArm(player);
        } else {
            renderer.renderLeftArm(player);
        }
        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);

		if (ARCompat.shouldAccelerate()) {
			ARCompat.resetRenderingLevel();
		}
    }
}
