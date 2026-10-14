package com.tacz.guns.client.renderer.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.entity.TargetMinecart;
import com.tacz.guns.legacy.render.RenderBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.entity.player.EntityPlayer;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.RenderType;
import com.tacz.guns.legacy.render.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Optional;

@SideOnly(Side.CLIENT)
/**
 * 1.12.2: a {@link RenderMinecart}; {@code renderMinecartContents} is {@code renderCartContents}, drawn on the GL
 * matrix at the same place, so the contents get a fresh pose stack and the cart's light.
 */
public class TargetMinecartRenderer extends RenderMinecart<TargetMinecart> {
    private static final String HEAD_NAME = "head";
    private static final String HEAD_2_NAME = "head2";

    public TargetMinecartRenderer(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.25F;
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);
    }

    @Override
    protected ResourceLocation getEntityTexture(TargetMinecart minecart) {
        return InternalAssetLoader.ENTITY_EMPTY_TEXTURE;
    }

    @Override
    protected void renderCartContents(TargetMinecart targetMinecart, float pPartialTicks, IBlockState pState) {
        MultiBufferSource.BufferSource buffer = RenderBuffers.bufferSource();
        renderMinecartContents(targetMinecart, pPartialTicks, pState, new PoseStack(), buffer, targetMinecart.getBrightnessForRender());
        buffer.endBatch();
    }

    protected void renderMinecartContents(TargetMinecart targetMinecart, float pPartialTicks, IBlockState pState, PoseStack stack, MultiBufferSource buffer, int pPackedLight) {
        getModel().ifPresent(model -> {
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart head2Model = model.getNode(HEAD_2_NAME);
            headModel.visible = false;
            head2Model.visible = false;

            stack.pushPose();
            stack.translate(0.5, 1.875, 0.5);
            stack.scale(1.5f, 1.5f, 1.5f);
            stack.mulPose(Axis.ZN.rotationDegrees(180));
            stack.mulPose(Axis.YN.rotationDegrees(90));
            RenderType renderType = RenderType.entityTranslucent(InternalAssetLoader.TARGET_MINECART_TEXTURE_LOCATION);
            model.render(stack, ItemCameraTransforms.TransformType.NONE, renderType, pPackedLight, OverlayTexture.NO_OVERLAY);
            if (targetMinecart.getGameProfile() != null) {
                stack.translate(0, 1, -4.5 / 16d);
                Minecraft minecraft = Minecraft.getMinecraft();
                GameProfile gameProfile = targetMinecart.getGameProfile();
                var map = minecraft.getSkinManager().loadSkinFromCache(gameProfile);
                ResourceLocation skin;
                if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    skin = minecraft.getSkinManager().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
                } else {
                    skin = DefaultPlayerSkin.getDefaultSkin(EntityPlayer.getUUID(gameProfile));
                }
                headModel.visible = true;
                RenderType skullRenderType = RenderType.entityTranslucentCull(skin);
                headModel.render(stack, ItemCameraTransforms.TransformType.NONE, buffer.getBuffer(skullRenderType), pPackedLight, OverlayTexture.NO_OVERLAY);

                head2Model.visible = true;
                stack.translate(0, 0, 0.01);
                head2Model.render(stack, ItemCameraTransforms.TransformType.NONE, buffer.getBuffer(skullRenderType), pPackedLight, OverlayTexture.NO_OVERLAY);
            }
            stack.popPose();
        });
    }
}