package com.tacz.guns;

import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.event.LoadingConfigEvent;
import com.tacz.guns.init.*;
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
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 1.12.2: the mod constructor runs before the first resource reload, like upstream's, so the config, the export of
 * the default gun pack and the deferred registers are set up here. Upstream's mod-bus listeners
 * ({@code CommonRegistry}, {@code ClientSetupEvent}) and the server reload events become the lifecycle handlers below.
 */
@Mod(modid = GunMod.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
        // 1.12.2: the Config button of the Mods screen reads the factory from the annotation, not from mcmod.info
        // (FMLModContainer#getGuiClassName); the mcmod.info guiFactory field is parsed but never used by FML
        guiFactory = "com.tacz.guns.legacy.client.TaczGuiFactory")
public class GunMod {
    public static final String MOD_ID = "tacz";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    /**
     * 默认模型包文件夹
     */
    public static final String DEFAULT_GUN_PACK_NAME = "tacz_default_gun";

    public GunMod() {
        TaczConfigs.init(Loader.instance().getConfigDir(), FMLCommonHandler.instance().getSide().isClient());

        LoadingConfigEvent.onLoadingConfig();

        ModBlocks.BLOCKS.register(MinecraftForge.EVENT_BUS);
        ModCreativeTabs.register();
        ModItems.ITEMS.register(MinecraftForge.EVENT_BUS);
        ModEntities.ENTITY_TYPES.register(MinecraftForge.EVENT_BUS);
        // 1.12.2: no registry (or no counterpart) for these, see the classes: ModContainer (IGuiHandler, registered in
        // pre-init), ModParticles, ModAttributes (ModAttributes#registerAttributes), ModPainting, ModLootModifiers
        ModSounds.SOUNDS.register(MinecraftForge.EVENT_BUS);
        // TODO(port): KubeJS compat is not ported

        registerDefaultExtraGunPack();
        AttachmentPropertyManager.registerModifier();
    }

    private static void registerDefaultExtraGunPack() {
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        ResourceManager.registerExportResource(GunMod.class, jarDefaultPackPath);
    }

    /**
     * 1.12.2: capabilities, block entities and the gui handler are registered in pre-init, and so are entity
     * renderers on the client.
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CapabilityRegistry.onRegisterCapabilities();
        ModBlocks.registerTileEntities();
        ModContainer.register(this);
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
        ModSyncedEntityData.init();
    }

    /**
     * Upstream {@code CompatRegistry.onEnqueue} ({@code InterModEnqueueEvent}).
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        CompatRegistry.onEnqueue();
    }

    /**
     * Upstream {@code CommonRegistry.onLoadComplete} ({@code FMLLoadCompleteEvent}).
     */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        CommonRegistry.onLoadComplete();
    }

    /**
     * Upstream {@code RegisterCommandsEvent}.
     */
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        CommandRegistry.onServerStaring(event);
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
