package com.tacz.guns.legacy.render;

/**
 * 1.20.1 {@code OverlayTexture} constants. 1.12.2 has no overlay texture; the value is accepted and ignored.
 */
public final class OverlayTexture {
    public static final int NO_WHITE_U = 0;
    public static final int RED_OVERLAY_V = 3;
    public static final int WHITE_OVERLAY_V = 10;
    public static final int NO_OVERLAY = pack(NO_WHITE_U, WHITE_OVERLAY_V);

    private OverlayTexture() {
    }

    public static int pack(int u, int v) {
        return u | v << 16;
    }
}
