package com.tacz.guns.legacy.util;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 1.20.1 {@code Font#width(Component)} / {@code Font#split(Component, int)} on 1.12.2's string based {@link FontRenderer}.
 */
@SideOnly(Side.CLIENT)
public final class FontUtil {
    private FontUtil() {
    }

    public static int width(FontRenderer font, ITextComponent text) {
        return font.getStringWidth(text.getFormattedText());
    }

    public static int width(FontRenderer font, String text) {
        return font.getStringWidth(text);
    }

    /**
     * {@code Font#split}: wraps the text to the given pixel width (formatting codes carry over to the next line).
     */
    public static List<String> split(FontRenderer font, ITextComponent text, int maxWidth) {
        return font.listFormattedStringToWidth(text.getFormattedText(), maxWidth);
    }
}
