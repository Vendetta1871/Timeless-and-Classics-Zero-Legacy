package com.tacz.guns.legacy.resource;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.resource.GunPackLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The client half of TACZ's gun pack loading on 1.12.2.
 * <p>
 * Upstream adds a pack finder to the client pack repository, so gun packs are rescanned whenever resources reload
 * and their {@code assets} are visible to vanilla (textures, sounds, languages). Here {@link #rescan()} runs at the
 * start of every {@code Minecraft#refreshResources} ({@code MinecraftMixin}), including the first one during startup,
 * and puts the packs (plus the JSON language bridge of the mod's own assets) into Minecraft's built-in resource
 * packs. {@link #RELOAD_LISTENER} then runs TACZ's client reload listeners ({@link ClientAssetsManager}) on the same
 * packs, like upstream's {@code RegisterClientReloadListenersEvent}.
 */
@SideOnly(Side.CLIENT)
public final class ClientGunPacks {
    private static final Field DEFAULT_RESOURCE_PACKS = ObfuscationReflectionHelper.findField(Minecraft.class, "field_110449_ao");
    private static final List<IResourcePack> INSTALLED = new ArrayList<>();
    private static GunPackRepository repository = GunPackRepository.empty();
    /**
     * The previous scan, closed once the reload listeners have switched to the new one.
     */
    @Nullable
    private static GunPackRepository retired;

    public static final IResourceManagerReloadListener RELOAD_LISTENER = ClientGunPacks::onResourceManagerReload;

    private ClientGunPacks() {
    }

    public static GunPackRepository repository() {
        return repository;
    }

    public static ResourceManager resourceManager() {
        return repository.resourceManager(PackType.CLIENT_RESOURCES);
    }

    public static void rescan() {
        GunPackRepository next = GunPackLoader.INSTANCE.discoverExtensions();
        install(next);
        if (retired != null) {
            retired.close();
        }
        retired = repository;
        repository = next;
    }

    private static void onResourceManagerReload(IResourceManager vanillaResourceManager) {
        ResourceManager resourceManager = resourceManager();
        ClientAssetsManager.INSTANCE.reloadAndRegister(listener -> listener.reload(resourceManager));
        if (retired != null) {
            retired.close();
            retired = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void install(GunPackRepository repository) {
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
