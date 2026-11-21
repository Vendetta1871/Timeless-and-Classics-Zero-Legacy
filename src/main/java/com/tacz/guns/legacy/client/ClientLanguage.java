package com.tacz.guns.legacy.client;

import com.tacz.guns.mixin.client.I18nAccessor;
import com.tacz.guns.mixin.client.LocaleAccessor;
import net.minecraft.client.resources.Locale;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

/**
 * 1.12.2 stand-in for 1.20's {@code Language.getInstance().getOrDefault(key)}: the unformatted translation of a key
 * in the client's current language ({@code I18n.format} would run it through {@code String.format}).
 */
@SideOnly(Side.CLIENT)
public final class ClientLanguage {
    private ClientLanguage() {
    }

    public static String getOrDefault(String key) {
        Locale locale = I18nAccessor.getLocale();
        if (locale == null) {
            return key;
        }
        Map<String, String> properties = ((LocaleAccessor) locale).getProperties();
        String value = properties.get(key);
        return value == null ? key : value;
    }
}
