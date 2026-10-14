package com.tacz.guns.client.event;

import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT)
public class RenderHeadShotAABB {
    @SubscribeEvent
    public static void onRenderEntity(RenderLivingEvent.Post<?> event) {
        boolean canRender = Minecraft.getMinecraft().getRenderManager().isDebugBoundingBox();
        if (!canRender) {
            return;
        }
        if (!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get()) {
            return;
        }
        EntityLivingBase entity = event.getEntity();
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return;
        }
        AxisAlignedBB aabb = HeadShotAABBConfigRead.getAABB(entityId);
        if (aabb == null) {
            float width = entity.width;
            float eyeHeight = entity.getEyeHeight();
            // 扩张 0.01，避免和原版显示重合
            aabb = new AxisAlignedBB(-width / 2, eyeHeight - 0.25, -width / 2, width / 2, eyeHeight + 0.25, width / 2).grow(0.01);
        }
        // 1.12.2: the event gives the camera-relative position instead of a translated pose stack
        GlStateManager.pushMatrix();
        GlStateManager.translate(event.getX(), event.getY(), event.getZ());
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        RenderGlobal.drawSelectionBoundingBox(aabb, 1.0F, 1.0F, 0.0F, 1.0F);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
