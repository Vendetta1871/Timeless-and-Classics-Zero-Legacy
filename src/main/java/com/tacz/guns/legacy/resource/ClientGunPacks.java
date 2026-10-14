package com.tacz.guns.legacy.resource;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds gun packs (and the JSON language bridge for the mod's own assets) to Minecraft's built-in resource packs,
 * the 1.12.2 counterpart of registering TACZ's pack finder for client resources. Takes effect on the next resource
 * reload.
 */
@SideOnly(Side.CLIENT)
public final class ClientGunPacks {
    private static final Field DEFAULT_RESOURCE_PACKS = ObfuscationReflectionHelper.findField(Minecraft.class, "field_110449_ao");
    private static final List<IResourcePack> INSTALLED = new ArrayList<>();

    private ClientGunPacks() {
    }

    @SuppressWarnings("unchecked")
    public static void install(GunPackRepository repository) {
        List<IResourcePack> defaultPacks;
        try {
            defaultPacks = (List<IResourcePack>) DEFAULT_RESOURCE_PACKS.get(Minecraft.getMinecraft());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access Minecraft#defaultResourcePacks", e);
        }
        defaultPacks.removeAll(INSTALLED);
        INSTALLED.clear();
        INSTALLED.add(new ModJsonLangPack(GunMod.MOD_ID));
        for (GunPackRepository.PackSource pack : repository.getPacks()) {
            INSTALLED.add(new GunPackResourcePack(pack));
        }
        defaultPacks.addAll(INSTALLED);
    }
}
