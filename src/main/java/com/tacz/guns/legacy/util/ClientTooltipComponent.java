package com.tacz.guns.legacy.util;

import com.tacz.guns.legacy.render.GuiGraphics;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code ClientTooltipComponent}: an image/text block that is part of an item tooltip. 1.12.2 tooltips are
 * plain string lists, so {@link TooltipComponentHandler} reserves the space with blank
 * lines and draws the component in {@code RenderTooltipEvent.PostText}.
 * <p>
 * Deviation: {@code renderText} draws straight through {@link GuiGraphics} instead of {@code Font#drawInBatch} with a
 * matrix and a buffer source.
 */
@SideOnly(Side.CLIENT)
public interface ClientTooltipComponent {
    int getHeight();

    int getWidth(FontRenderer font);

    default void renderText(FontRenderer font, int x, int y, GuiGraphics graphics) {
    }

    default void renderImage(FontRenderer font, int x, int y, GuiGraphics graphics) {
    }
}
