package com.tacz.guns.compat.oculus;

import com.tacz.guns.legacy.render.MultiBufferSource;

/**
 * Oculus does not exist on 1.12.2. The class is kept so upstream call sites stay unchanged; shader-pack checks
 * will be answered by OptiFine here once shader support lands (README, stage 2).
 */
public final class OculusCompat {
    private OculusCompat() {
    }

    public static void initCompat() {
    }

    public static boolean isRenderShadow() {
        return false;
    }

    public static boolean isUsingRenderPack() {
        return false;
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        return false;
    }
}
