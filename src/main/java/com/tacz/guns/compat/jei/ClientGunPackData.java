package com.tacz.guns.compat.jei;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.legacy.crafting.RecipeManager;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2: JEI asks for the recipes and builds its item list while the game is starting
 * ({@code FMLLoadCompleteEvent}), but the client only receives the gun pack data when it joins a world
 * ({@link CommonNetworkCache}); upstream the client has the data at that point. So, like upstream, the JEI plugin
 * reads the local gun packs into the client cache first. Joining a server replaces that data with the server's copy,
 * as before.
 */
@SideOnly(Side.CLIENT)
public final class ClientGunPackData {
    private static boolean loaded;

    private ClientGunPackData() {
    }

    /**
     * Reads the local gun packs into {@link CommonNetworkCache} unless a running server or a finished server sync
     * already provided the data. Safe to call more than once; failures are logged instead of breaking JEI.
     */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (CommonAssetsManager.getInstance() != null
                || !TimelessAPI.getAllCommonBlockIndex().isEmpty()
                || !TimelessAPI.getAllCommonGunIndex().isEmpty()) {
            return;
        }
        try {
            GunMod.LOGGER.info("[JEI] Reading the local gun packs (on 1.12.2 the client normally gets them from a server)");
            // publishes the instance for the cross-references between the loaders, like the server side
            CommonAssetsManager.onReload();
            CommonAssetsManager manager = CommonAssetsManager.getInstance();
            if (manager != null) {
                CommonNetworkCache.INSTANCE.fromNetwork(manager.getNetworkCache());
                // the client-side indexes (localised names, models) are normally rebuilt on the resource reload that
                // follows a server sync; JEI reads them now (item list, search), so rebuild them here too
                ClientIndexManager.reload();
            }
        } catch (RuntimeException e) {
            GunMod.LOGGER.error("[JEI] Failed to read the local gun packs", e);
        } finally {
            // the client uses the cache filled above; the instance belongs to the server
            CommonAssetsManager.clearInstance();
        }
    }

    /**
     * The gun smith table recipes of the current data source: the server's, or the client's (synced, or read from the
     * local packs by {@link #ensureLoaded()}).
     */
    public static RecipeManager recipeManager() {
        CommonAssetsManager manager = CommonAssetsManager.getInstance();
        if (manager != null && manager.recipeManager != null) {
            return manager.recipeManager;
        }
        return CommonNetworkCache.INSTANCE.recipeManager;
    }
}
