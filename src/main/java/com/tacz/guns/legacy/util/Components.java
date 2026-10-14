package com.tacz.guns.legacy.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * Factories for the 1.20.1 {@code Component.translatable(..)} / {@code Component.literal(..)} idiom, including the
 * chainable {@code withStyle(TextFormatting...)} of {@code MutableComponent}.
 */
public final class Components {
    private Components() {
    }

    public static Translatable translatable(String key, Object... args) {
        return new Translatable(key, args);
    }

    public static Literal literal(String text) {
        return new Literal(text);
    }

    static void applyFormatting(ITextComponent component, TextFormatting... formats) {
        Style style = component.getStyle();
        for (TextFormatting format : formats) {
            switch (format) {
                case BOLD:
                    style.setBold(true);
                    break;
                case ITALIC:
                    style.setItalic(true);
                    break;
                case UNDERLINE:
                    style.setUnderlined(true);
                    break;
                case STRIKETHROUGH:
                    style.setStrikethrough(true);
                    break;
                case OBFUSCATED:
                    style.setObfuscated(true);
                    break;
                case RESET:
                    break;
                default:
                    style.setColor(format);
                    break;
            }
        }
    }

    public static class Translatable extends TextComponentTranslation {
        Translatable(String key, Object... args) {
            super(key, args);
        }

        public Translatable withStyle(TextFormatting... formats) {
            applyFormatting(this, formats);
            return this;
        }

        /**
         * {@code MutableComponent#append}.
         */
        public Translatable append(ITextComponent sibling) {
            appendSibling(sibling);
            return this;
        }

        public Translatable append(String text) {
            appendText(text);
            return this;
        }
    }

    public static class Literal extends TextComponentString {
        Literal(String text) {
            super(text);
        }

        public Literal withStyle(TextFormatting... formats) {
            applyFormatting(this, formats);
            return this;
        }

        /**
         * {@code MutableComponent#append}.
         */
        public Literal append(ITextComponent sibling) {
            appendSibling(sibling);
            return this;
        }

        public Literal append(String text) {
            appendText(text);
            return this;
        }
    }
}
