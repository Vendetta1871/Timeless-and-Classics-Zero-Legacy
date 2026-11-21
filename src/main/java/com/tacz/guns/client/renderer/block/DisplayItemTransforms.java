package com.tacz.guns.client.renderer.block;

import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.legacy.render.PoseStack;
import net.minecraft.util.EnumFacing;

/** 展示容器的槽位变换；输出是调用 ItemRenderer.renderStatic 之前的坐标空间。 */
public class DisplayItemTransforms {
    private DisplayItemTransforms() {}

    public static void apply(PoseStack pose, EnumFacing facing, double x, double y, double offset, int rotation) {
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.getHorizontalIndex() * 90.0F));
        pose.translate(x, y, offset);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation * 45.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
    }
}
