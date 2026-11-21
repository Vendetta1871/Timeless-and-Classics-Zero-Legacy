package com.tacz.guns.mixin.client;

import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 1.12.2 only: the raw translations of a {@link Locale}, for {@code legacy.client.ClientLanguage}.
 */
@Mixin(Locale.class)
public interface LocaleAccessor {
    @Accessor("properties")
    Map<String, String> getProperties();
}
