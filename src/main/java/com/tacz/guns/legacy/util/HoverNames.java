package com.tacz.guns.legacy.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 1.20.1 {@code ItemStack#getHoverName()} for components built on the server. 1.12.2's
 * {@code ItemStack#getDisplayName()} is a string localized on the side that calls it (and TACZ's ammo name override,
 * {@code AmmoItem#getItemStackDisplayName}, is client-only), so a server message would carry an untranslated name.
 * This builds a translatable component the receiving client localizes instead.
 */
public final class HoverNames {
    private HoverNames() {
    }

    public static ITextComponent getHoverName(ItemStack stack) {
        if (stack.hasDisplayName()) {
            return new TextComponentString(stack.getDisplayName());
        }
        if (stack.getItem() instanceof IAmmo ammo) {
            ResourceLocation ammoId = ammo.getAmmoId(stack);
            String name = TimelessAPI.getCommonAmmoIndex(ammoId).map(index -> index.getPojo().getName()).orElse(null);
            if (name != null) {
                return new TextComponentTranslation(name);
            }
        }
        return new TextComponentTranslation(stack.getItem().getTranslationKey(stack) + ".name");
    }
}
