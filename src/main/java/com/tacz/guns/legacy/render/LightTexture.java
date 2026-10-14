package com.tacz.guns.legacy.render;

/**
 * 1.20.1 {@code LightTexture} packing helpers. The packed format is the same as 1.12.2's combined light
 * ({@code sky << 20 | block << 4}), so values from {@code getBrightnessForRender} can be passed straight through.
 */
public final class LightTexture {
    public static final int FULL_BRIGHT = 0xF000F0;
    public static final int FULL_SKY = 0xF00000;
    public static final int FULL_BLOCK = 0xF0;

    private LightTexture() {
    }

    public static int pack(int blockLight, int skyLight) {
        return blockLight << 4 | skyLight << 20;
    }

    public static int block(int packedLight) {
        return packedLight >> 4 & 0xFFFF;
    }

    public static int sky(int packedLight) {
        return packedLight >> 20 & 0xFFFF;
    }
}
