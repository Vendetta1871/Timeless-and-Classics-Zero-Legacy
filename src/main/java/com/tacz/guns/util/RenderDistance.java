package com.tacz.guns.util;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.client.renderer.item.ScreenItemVboContext;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

@SideOnly(Side.CLIENT)
public final class RenderDistance {
    public static boolean inRenderHighPolyModelDistance(PoseStack poseStack) {
        // Only the active screen draw bypasses LOD; an open screen still renders the world first.
        if (ScreenItemVboContext.isActive()) {
            return true;
        }
        int distance = RenderConfig.GUN_LOD_RENDER_DISTANCE.get();
        if (distance <= 0) {
            return false;
        }
        // 1.12.2: the camera-relative part of the transform is on the GL matrix stack, not in the pose
        Matrix4f matrix4f = RenderSystem.getModelViewMatrix().mul(poseStack.last().pose());
        float viewDistance = matrix4f.m30() * matrix4f.m30() + matrix4f.m31() * matrix4f.m31() + matrix4f.m32() * matrix4f.m32();
        return viewDistance < distance * distance;
    }
}
