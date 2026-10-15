package com.tacz.guns.legacy.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;

/**
 * Private 1.12.2 {@link Minecraft} members that the 1.20.1 API exposes (there is no access transformer in this project).
 */
@SideOnly(Side.CLIENT)
public final class MinecraftAccess {
    private static final Method RIGHT_CLICK_MOUSE = ObfuscationReflectionHelper.findMethod(Minecraft.class, "func_147121_ag", void.class);

    private MinecraftAccess() {
    }

    /**
     * {@code Minecraft#startUseItem()} of 1.20.1 is {@code rightClickMouse()} here.
     */
    public static void startUseItem(Minecraft minecraft) {
        try {
            RIGHT_CLICK_MOUSE.invoke(minecraft);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
