package com.tacz.guns.client.model.bedrock;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.VertexConsumer;

public interface BedrockCube {
    void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha);
}
