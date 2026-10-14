package com.tacz.guns.client.model.functional;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.VertexConsumer;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHandSide;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import com.tacz.guns.legacy.render.DisplayContext;
import com.tacz.guns.legacy.render.RenderBuffers;

public class RightHandRender implements IFunctionalRenderer {
    private final BedrockAnimatedModel bedrockGunModel;

    public RightHandRender(BedrockAnimatedModel bedrockGunModel) {
        this.bedrockGunModel = bedrockGunModel;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemCameraTransforms.TransformType transformType, int light, int overlay) {
        if (DisplayContext.firstPerson(transformType)) {
            if (!bedrockGunModel.getRenderHand()) {
                return;
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            Matrix3f normal = new Matrix3f(poseStack.last().normal());
            Matrix4f pose = new Matrix4f(poseStack.last().pose());
            //和枪械模型共用顶点缓冲的都需要代理到渲染结束后渲染
            bedrockGunModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
                PoseStack poseStack2 = new PoseStack();
                poseStack2.last().normal().mul(normal);
                poseStack2.last().pose().mul(pose);
                RenderHelper.renderFirstPersonArm(Minecraft.getMinecraft().player, EnumHandSide.RIGHT, poseStack2, light1);
                RenderBuffers.bufferSource().endBatch();
            });
        }
    }
}
