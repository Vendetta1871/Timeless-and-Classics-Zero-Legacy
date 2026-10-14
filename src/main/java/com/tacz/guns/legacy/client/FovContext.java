package com.tacz.guns.legacy.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code ViewportEvent.ComputeFov#usedConfiguredFov()}: 1.12.2's {@code FOVModifier} event does not say whether
 * the FOV is for the level ({@code true}) or the hand ({@code false}). {@code GameRendererMixin} records the
 * {@code useFOVSetting} argument of {@code EntityRenderer#getFOVModifier} here, as upstream does to tell the two
 * apart for the bob events.
 */
@SideOnly(Side.CLIENT)
public final class FovContext {
    private static boolean useFovSetting = true;

    private FovContext() {
    }

    public static boolean usedConfiguredFov() {
        return useFovSetting;
    }

    public static void setUseFovSetting(boolean value) {
        useFovSetting = value;
    }
}
