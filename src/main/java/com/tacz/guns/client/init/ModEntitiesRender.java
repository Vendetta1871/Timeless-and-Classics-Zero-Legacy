package com.tacz.guns.client.init;

import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.renderer.block.StatueRenderer;
import com.tacz.guns.client.renderer.block.TargetRenderer;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.client.renderer.entity.TargetMinecartRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2: called by {@link com.tacz.guns.legacy.client.LegacyClientSetup#preInit()} (entity renderers must be
 * registered before init).
 */
@SideOnly(Side.CLIENT)
public class ModEntitiesRender {
    /**
     * {@code EntityRenderersEvent.RegisterRenderers}
     */
    public static void onEntityRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityKineticBullet.class, EntityBulletRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(TargetMinecart.class, TargetMinecartRenderer::new);
        ClientRegistry.bindTileEntitySpecialRenderer(GunSmithTableBlockEntity.class, new GunSmithTableRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TargetBlockEntity.class, new TargetRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(StatueBlockEntity.class, new StatueRenderer());
    }
}
