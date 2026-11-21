package com.tacz.guns.mixin.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.12.2 only: the client's current {@link Locale}, for {@code legacy.client.ClientLanguage}.
 */
@Mixin(I18n.class)
public interface I18nAccessor {
    @Accessor("i18nLocale")
    static Locale getLocale() {
        throw new AssertionError();
    }
}
