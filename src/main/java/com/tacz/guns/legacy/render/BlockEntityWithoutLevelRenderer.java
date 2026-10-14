package com.tacz.guns.legacy.render;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code BlockEntityWithoutLevelRenderer} on top of 1.12.2 {@link TileEntityItemStackRenderer}: subclasses
 * override the 1.20.1-style {@link #renderByItem(ItemStack, ItemCameraTransforms.TransformType, PoseStack,
 * MultiBufferSource, int, int)}. The display context comes from {@link PerspectiveCaptureModel} (the item's model
 * must be wrapped), the light from the lightmap the caller set.
 */
@SideOnly(Side.CLIENT)
public abstract class BlockEntityWithoutLevelRenderer extends TileEntityItemStackRenderer {
    @Override
    public final void renderByItem(ItemStack stack, float partialTicks) {
        MultiBufferSource.BufferSource bufferSource = RenderBuffers.bufferSource();
        renderByItem(stack, PerspectiveCaptureModel.current(), new PoseStack(), bufferSource, LightTexture.current(), OverlayTexture.NO_OVERLAY);
        bufferSource.endBatch();
    }

    public void renderByItem(ItemStack stack, ItemCameraTransforms.TransformType transformType, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
    }
}
