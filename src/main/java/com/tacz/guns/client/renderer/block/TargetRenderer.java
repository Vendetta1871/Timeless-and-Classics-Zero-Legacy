package com.tacz.guns.client.renderer.block;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.block.TargetBlock;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.RenderType;
import com.tacz.guns.legacy.util.BlockEntityRenderer;
import com.tacz.guns.legacy.render.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.EnumFacing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.util.LegacyMath;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.block.state.IBlockState;

import java.util.Optional;

public class TargetRenderer extends BlockEntityRenderer<TargetBlockEntity> {
    private static final String UPPER_NAME = "target_upper";
    private static final String HEAD_NAME = "head";

    public TargetRenderer() {
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MODEL_LOCATION);
    }

    @Override
    public void render(TargetBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        getModel().ifPresent(model -> {
            IBlockState blockState = blockEntity.getBlockState();
            EnumFacing facing = blockState.getValue(TargetBlock.FACING);
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart upperModel = model.getNode(UPPER_NAME);
            float deg = -LegacyMath.lerp(partialTick, blockEntity.oRot, blockEntity.rot);
            upperModel.xRot = (float) Math.toRadians(deg);
            headModel.visible = false;

            poseStack.pushPose();
            poseStack.translate(0.5, 0.225, 0.5);
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.getHorizontalIndex() * 90));
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.translate(0, -1.275, 0.0125);
            RenderType renderType = RenderType.entityTranslucent(InternalAssetLoader.TARGET_TEXTURE_LOCATION);
            model.render(poseStack, ItemCameraTransforms.TransformType.NONE, renderType, combinedLightIn, combinedOverlayIn);
            if (blockEntity.getOwner() != null) {
                poseStack.translate(0, 1.25, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(deg));
                Minecraft minecraft = Minecraft.getMinecraft();
                var map = minecraft.getSkinManager().loadSkinFromCache(blockEntity.getOwner());
                ResourceLocation skin;
                if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    skin = minecraft.getSkinManager().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                } else {
                    skin = DefaultPlayerSkin.getDefaultSkin(EntityPlayer.getUUID(blockEntity.getOwner()));
                }
                headModel.visible = true;
                RenderType skullRenderType = RenderType.entityCutout(skin);
                headModel.render(poseStack, ItemCameraTransforms.TransformType.NONE, bufferIn.getBuffer(skullRenderType), combinedLightIn, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        });
    }

    // 1.12.2: the view distance is TargetBlockEntity#getMaxRenderDistanceSquared
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(TargetBlockEntity blockEntity) {
        return true;
    }
}