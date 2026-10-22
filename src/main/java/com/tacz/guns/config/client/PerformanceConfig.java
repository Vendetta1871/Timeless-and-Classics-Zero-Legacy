package com.tacz.guns.config.client;

import com.tacz.guns.legacy.config.ForgeConfigSpec;

/**
 * 1.12.2 only: how aiming with a scope magnifies, and what it costs. The picture-in-picture lens view lives in
 * {@code com.tacz.guns.legacy.client.ScopeView}; the full-screen zoom is upstream's behaviour (it needs no extra
 * world pass).
 */
public class PerformanceConfig {
    public static ForgeConfigSpec.EnumValue<ScopeZoomMode> SCOPE_ZOOM_MODE;
    public static ForgeConfigSpec.BooleanValue SCOPE_MIRROR;
    public static ForgeConfigSpec.DoubleValue SCOPE_VIEW_RESOLUTION;
    public static ForgeConfigSpec.DoubleValue SCOPE_FISHEYE;
    public static ForgeConfigSpec.DoubleValue SCOPE_MIRROR_FOV;

    public static void init(ForgeConfigSpec.Builder builder) {
        builder.push("performance");

        // 1.12.2 only
        builder.comment("How aiming through a scope magnifies the target. PICTURE_IN_PICTURE: only the view inside the lens is magnified (rendered by a second world pass), the rest of the screen stays as is. FULL_SCREEN_ZOOM: the whole screen zooms in while aiming (the original TACZ behaviour, no extra world pass - the cheapest option). Off under OptiFine shaders.");
        SCOPE_ZOOM_MODE = builder.defineEnum("ScopeZoomMode", ScopeZoomMode.PICTURE_IN_PICTURE);

        builder.comment("Make a scope's lens mirror what is behind the player when not aiming through it, instead of leaving it see-through from the hip (picture-in-picture mode only). Off restores the see-through lens and skips a whole extra world render pass.");
        SCOPE_MIRROR = builder.define("ScopeMirror", true);

        builder.comment("Resolution of the view through a scope, relative to the screen. Lower values are faster.");
        SCOPE_VIEW_RESOLUTION = builder.defineInRange("ScopeViewResolution", 1.0, 0.25, 1.0);

        builder.comment("Fisheye (barrel) distortion of the view through a scope: how much farther out the lens edge looks. 0 turns it off.");
        SCOPE_FISHEYE = builder.defineInRange("ScopeFisheye", 0.25, 0.0, 1.0);

        builder.comment("Field of view of the reflection in a scope's lens, in degrees: how much of what is behind the player the lens mirrors.");
        SCOPE_MIRROR_FOV = builder.defineInRange("ScopeMirrorFov", 80.0, 60.0, 178.0);

        builder.pop();
    }

    public enum ScopeZoomMode {
        PICTURE_IN_PICTURE,
        FULL_SCREEN_ZOOM
    }
}
