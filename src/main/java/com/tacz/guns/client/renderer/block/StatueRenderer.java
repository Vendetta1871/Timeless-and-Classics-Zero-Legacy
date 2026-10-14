package com.tacz.guns.client.renderer.block;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.RenderType;
import com.tacz.guns.legacy.util.BlockEntityRenderer;
import com.tacz.guns.legacy.render.OverlayTexture;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class StatueRenderer extends BlockEntityRenderer<StatueBlockEntity> {
    public StatueRenderer() {
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.STATUE_MODEL_LOCATION);
    }

    @Override
    public void render(StatueBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        getModel().ifPresent(model -> {
            World level = blockEntity.getWorld();
            if (level == null) {
                return;
            }

            poseStack.pushPose();
            {
                IBlockState blockState = blockEntity.getBlockState();
                EnumFacing facing = blockState.getValue(TargetBlock.FACING);

                poseStack.translate(0.5, 1.5, 0.5);

                poseStack.mulPose(Axis.YN.rotationDegrees((facing.getHorizontalIndex() + 2) % 4 * 90));
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));

                RenderType renderType = RenderConfig.BLOCK_ENTITY_TRANSLUCENT.get() ?
                        RenderType.entityTranslucent(getTextureLocation()) :
                        RenderType.entityCutout(getTextureLocation());
                model.render(poseStack, ItemCameraTransforms.TransformType.NONE, renderType, combinedLightIn, combinedOverlayIn);

                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.translate(0, -0.875, -1.2);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));

                double offset = Math.sin(System.currentTimeMillis() / 500.0) * 0.1;
                poseStack.translate(0, offset, 0);

                ItemStack stack = blockEntity.getGunItem();

                // 1.12.2: the item is rendered through GL with the pose stack's matrix; draw what is batched first
                if (bufferIn instanceof MultiBufferSource.BufferSource bufferSource) {
                    bufferSource.endBatch();
                }
                GlStateManager.pushMatrix();
                RenderSystem.multMatrix(poseStack.last().pose());
                Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
                GlStateManager.popMatrix();
            }
            poseStack.popPose();
        });
    }

    public static ResourceLocation getTextureLocation() {
        return InternalAssetLoader.STATUE_TEXTURE_LOCATION;
    }

    // 1.12.2: the view distance is StatueBlockEntity#getMaxRenderDistanceSquared
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(StatueBlockEntity blockEntity) {
        return true;
    }
}