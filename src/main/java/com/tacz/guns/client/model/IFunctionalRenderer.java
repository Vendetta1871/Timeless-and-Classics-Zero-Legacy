package com.tacz.guns.client.model;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.VertexConsumer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;

public interface IFunctionalRenderer {
    void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemCameraTransforms.TransformType transformType, int light, int overlay);
}
