package com.tacz.guns.compat.ar;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.VertexConsumer;

/**
 * Accelerated Rendering does not exist on 1.12.2. The class is kept as a no-op so upstream call sites stay
 * unchanged; {@link #shouldAccelerate()} is always false.
 */
public final class ARCompat {
    public static final String MOD_ID = "acceleratedrendering";
    public static boolean LOADED = false;

    private ARCompat() {
    }

    public static void init() {
    }

    public static boolean shouldAccelerate() {
        return false;
    }

    public static boolean isAccelerated(VertexConsumer vertexConsumer) {
        return false;
    }

    public static void setRenderingLevel() {
    }

    public static void resetRenderingLevel() {
    }

    public static void setRenderLayer(int layer) {
    }

    public static void setRenderBeforeFunction(Runnable runnable) {
    }

    public static void setRenderAfterFunction(Runnable runnable) {
    }

    public static void resetRenderLayer() {
    }

    public static void resetRenderBeforeFunction() {
    }

    public static void resetRenderAfterFunction() {
    }

    public static void disableAcceleration() {
    }

    public static void resetAcceleration() {
    }

    public static void renderLaser(VertexConsumer extension, float z, float width, boolean fadeOut, PoseStack poseStack, int color) {
    }
}
