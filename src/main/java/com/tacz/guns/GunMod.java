package com.tacz.guns;

import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.legacy.client.LegacyClientSetup;
import com.tacz.guns.legacy.config.TaczConfigs;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2: the mod constructor runs before the first resource reload, like upstream's, so the config, the export of
 * the default gun pack and the deferred registers are set up here. Upstream's mod-bus listeners
 * ({@code CommonRegistry}, {@code ClientSetupEvent}) and the server reload events become the lifecycle handlers below.
 */
@Mod(modid = GunMod.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    /**
     * 默认模型包文件夹
     */
    public static final String DEFAULT_GUN_PACK_NAME = "tacz_default_gun";

    public GunMod() {
        TaczConfigs.init(Loader.instance().getConfigDir(), FMLCommonHandler.instance().getSide().isClient());

        // TODO(port): ModBlocks, ModCreativeTabs, ModItems, ModEntities, ModContainer, ModParticles, ModAttributes,
        //  ModPainting, ModLootModifiers are not ported yet
        ModSounds.SOUNDS.register(MinecraftForge.EVENT_BUS);
        // TODO(port): KubeJS compat is not ported

        registerDefaultExtraGunPack();
        AttachmentPropertyManager.registerModifier();
    }

    private static void registerDefaultExtraGunPack() {
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        ResourceManager.registerExportResource(GunMod.class, jarDefaultPackPath);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            LegacyClientSetup.preInit();
        }
    }

    /**
     * Upstream {@code CommonRegistry.onSetupEvent} and {@code ClientSetupEvent}.
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            // not in pre-init: the texture manager does not exist yet then
            LegacyClientSetup.init();
        }
        NetworkHandler.init();
        // TODO(port): ModSyncedEntityData.init() (entity/sync is not ported yet)
    }

    /**
     * Upstream {@code AddReloadListenerEvent}: the server loads the gun packs' data before the worlds load.
     */
    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        CommonAssetsManager.onReload();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        CommonAssetsManager.onServerStopped();
    }
}
