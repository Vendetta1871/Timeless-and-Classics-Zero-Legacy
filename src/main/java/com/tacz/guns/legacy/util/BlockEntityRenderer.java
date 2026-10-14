package com.tacz.guns.legacy.util;

import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.OverlayTexture;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderBuffers;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code BlockEntityRenderer} on top of 1.12.2's {@link TileEntitySpecialRenderer}: subclasses override the
 * 1.20.1-style {@link #render(TileEntity, float, PoseStack, MultiBufferSource, int, int)}. The pose stack starts at the
 * block entity's position relative to the camera (the origin of 1.20.1's block entity pose stack), the light is the
 * block entity's combined light.
 */
@SideOnly(Side.CLIENT)
public abstract class BlockEntityRenderer<T extends TileEntity> extends TileEntitySpecialRenderer<T> {
    @Override
    public final void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.getWorld() == null) {
            return;
        }
        PoseStack poseStack = new PoseStack();
        poseStack.translate(x, y, z);
        MultiBufferSource.BufferSource bufferSource = RenderBuffers.bufferSource();
        render(te, partialTicks, poseStack, bufferSource, te.getWorld().getCombinedLight(te.getPos(), 0), OverlayTexture.NO_OVERLAY);
        bufferSource.endBatch();
    }

    public abstract void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn);

    public boolean shouldRenderOffScreen(T blockEntity) {
        return false;
    }

    @Override
    public boolean isGlobalRenderer(T te) {
        return shouldRenderOffScreen(te);
    }
}
