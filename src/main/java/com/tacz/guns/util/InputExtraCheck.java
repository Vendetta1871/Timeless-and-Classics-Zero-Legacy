package com.tacz.guns.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class InputExtraCheck {
    public static boolean isInGame() {
        Minecraft mc = Minecraft.getMinecraft();
        // 不能是加载界面
        // 1.12.2: no loading overlay while in game (resource reloads block the game thread)
        // 不能打开任何 GUI
        if (mc.currentScreen != null) {
            return false;
        }
        // 当前窗口捕获鼠标操作
        if (!mc.inGameHasFocus) {
            return false;
        }
        // 选择了当前窗口
        return Display.isActive();
    }
}
