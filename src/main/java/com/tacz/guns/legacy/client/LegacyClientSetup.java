package com.tacz.guns.legacy.client;

import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.legacy.resource.ClientGunPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
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
        // upstream: client/init/ModEntitiesRender (EntityRenderers.register)
        RenderingRegistry.registerEntityRenderingHandler(EntityKineticBullet.class, EntityBulletRenderer::new);
    }

    public static void init() {
        // RegisterClientReloadListenersEvent: registering also runs the listeners once, on the packs that the first
        // resource reload installed
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ClientGunPacks.RELOAD_LISTENER);
    }
}
