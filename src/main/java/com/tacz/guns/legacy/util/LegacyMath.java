package com.tacz.guns.legacy.util;

/**
 * 1.20.1 {@code net.minecraft.util.Mth} helpers that 1.12.2's {@code MathHelper} does not have.
 */
public final class LegacyMath {
    private LegacyMath() {
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static float inverseLerp(float value, float start, float end) {
        return (value - start) / (end - start);
    }

    public static double inverseLerp(double value, double start, double end) {
        return (value - start) / (end - start);
    }

    public static int sign(double value) {
        return value == 0.0D ? 0 : (value > 0.0D ? 1 : -1);
    }

    public static float square(float value) {
        return value * value;
    }

    public static double square(double value) {
        return value * value;
    }
}
