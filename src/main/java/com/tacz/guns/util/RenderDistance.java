package com.tacz.guns.util;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

@SideOnly(Side.CLIENT)
public final class RenderDistance {
    private static long GUI_RENDER_TIMESTAMP = -1L;

    public static boolean inRenderHighPolyModelDistance(PoseStack poseStack) {
        if (isGuiRender()) {
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

    public static void markGuiRenderTimestamp() {
        GUI_RENDER_TIMESTAMP = System.currentTimeMillis();
    }

    private static boolean isGuiRender() {
        return System.currentTimeMillis() - GUI_RENDER_TIMESTAMP < 100;
    }
}
