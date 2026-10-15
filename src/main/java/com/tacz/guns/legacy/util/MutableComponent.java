package com.tacz.guns.legacy.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

/**
 * 1.20.1 {@code MutableComponent}: the common type of {@link Components#literal} and {@link Components#translatable}
 * results, with the chainable {@code withStyle} / {@code append}.
 */
public interface MutableComponent extends ITextComponent {
    MutableComponent withStyle(TextFormatting... formats);

    MutableComponent append(ITextComponent sibling);

    MutableComponent append(String text);

    /**
     * {@code Component#getString()}
     */
    default String getString() {
        return getUnformattedText();
    }
}
