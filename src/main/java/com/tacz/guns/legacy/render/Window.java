package com.tacz.guns.legacy.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code com.mojang.blaze3d.platform.Window} sizes, from 1.12.2's {@link ScaledResolution}.
 * {@code Minecraft.getInstance().getWindow()} becomes {@link #get()}, overlay events use {@link #of}.
 */
@SideOnly(Side.CLIENT)
public final class Window {
    private final ScaledResolution resolution;

    private Window(ScaledResolution resolution) {
        this.resolution = resolution;
    }

    public static Window get() {
        return new Window(new ScaledResolution(Minecraft.getMinecraft()));
    }

    public static Window of(ScaledResolution resolution) {
        return new Window(resolution);
    }

    public int getGuiScaledWidth() {
        return resolution.getScaledWidth();
    }

    public int getGuiScaledHeight() {
        return resolution.getScaledHeight();
    }

    public double getGuiScale() {
        return resolution.getScaleFactor();
    }

    public int getWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public int getHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }

    public ScaledResolution getResolution() {
        return resolution;
    }
}
