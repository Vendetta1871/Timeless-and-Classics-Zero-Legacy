package com.tacz.guns.legacy.util.gui;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code Renderable}: a widget that a screen draws every frame.
 */
@SideOnly(Side.CLIENT)
public interface Renderable {
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
}
