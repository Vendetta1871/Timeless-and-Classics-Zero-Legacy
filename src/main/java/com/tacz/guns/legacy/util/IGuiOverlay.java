package com.tacz.guns.legacy.util;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code IGuiOverlay}. {@code ForgeGui} is 1.12.2's {@link GuiIngame} ({@code getGuiTicks()} is
 * {@code getUpdateCounter()}); overlays are called from a {@code RenderGameOverlayEvent.Post} handler
 * ({@code client.init.ClientSetupEvent#onRenderGameOverlay}).
 */
@SideOnly(Side.CLIENT)
public interface IGuiOverlay {
    void render(GuiIngame gui, GuiGraphics graphics, float partialTick, int width, int height);
}
