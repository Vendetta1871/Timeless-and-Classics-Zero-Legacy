package com.tacz.guns.client.model.functional;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.VertexConsumer;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.papi.PapiManager;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.legacy.render.LightTexture;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import com.tacz.guns.legacy.render.DisplayContext;

public class TextShowRender implements IFunctionalRenderer {
    private final BedrockModel bedrockModel;
    private final TextShow textShow;
    private final ItemStack gunStack;

    public TextShowRender(BedrockModel bedrockModel, TextShow textShow, ItemStack gunStack) {
        this.bedrockModel = bedrockModel;
        this.textShow = textShow;
        this.gunStack = gunStack;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemCameraTransforms.TransformType transformType, int light, int overlay) {
        if (!DisplayContext.firstPerson(transformType)) {
            return;
        }
        String text = PapiManager.getTextShow(textShow.getTextKey(), gunStack);
        if (StringUtils.isBlank(text)) {
            return;
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        Matrix4f pose = new Matrix4f(poseStack.last().pose());

        // 和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
        bedrockModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            boolean shadow = textShow.isShadow();
            int color = textShow.getColorInt();
            float scale = textShow.getScale();
            int packLight = LightTexture.pack(textShow.getTextLight(), textShow.getTextLight());
            int width = font.getStringWidth(text);
            int xOffset;
            switch (textShow.getAlign()) {
                case CENTER -> xOffset = width / 2;
                case RIGHT -> xOffset = width;
                default -> xOffset = 0;
            }

            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().mul(pose);
            poseStack2.scale(2 / 300f * scale, -2 / 300f * scale, -2 / 300f);

            // 1.12.2 fonts draw immediately through the GL matrix stack
            GlStateManager.pushMatrix();
            RenderSystem.multMatrix(poseStack2.last().pose());
            float lastBrightnessX = OpenGlHelper.lastBrightnessX;
            float lastBrightnessY = OpenGlHelper.lastBrightnessY;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, packLight & 0xFFFF, packLight >> 16 & 0xFFFF);
            font.drawString(text, -xOffset, -font.FONT_HEIGHT / 2f, color, shadow);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            GlStateManager.popMatrix();
        });
    }
}
