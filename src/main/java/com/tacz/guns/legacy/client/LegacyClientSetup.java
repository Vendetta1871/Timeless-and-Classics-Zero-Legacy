package com.tacz.guns.legacy.client;

import com.tacz.guns.client.init.ClientSetupEvent;
import com.tacz.guns.client.init.ModEntitiesRender;
import com.tacz.guns.legacy.resource.ClientGunPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only setup called from {@code GunMod} (1.12.2 has no {@code FMLClientSetupEvent} on a mod bus).
 */
@SideOnly(Side.CLIENT)
public final class LegacyClientSetup {
    private LegacyClientSetup() {
    }

    /**
     * Entity renderers must be registered before init on 1.12.2.
     */
    public static void preInit() {
        // upstream: client/init/ModEntitiesRender (EntityRenderers.register, BlockEntityRenderers.register)
        ModEntitiesRender.onEntityRenderers();
        // upstream: client/init/ClientSetupEvent (RegisterKeyMappingsEvent, RegisterGuiOverlaysEvent)
        ClientSetupEvent.onClientSetup();
        ClientSetupEvent.onRegisterGuiOverlays();
    }

    public static void init() {
        // upstream: client/init/ClientSetupEvent (FMLClientSetupEvent: item colors, item properties)
        ClientSetupEvent.onClientSetupInit();
        // RegisterClientReloadListenersEvent: registering also runs the listeners once, on the packs that the first
        // resource reload installed
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ClientGunPacks.RELOAD_LISTENER);
    }
}
