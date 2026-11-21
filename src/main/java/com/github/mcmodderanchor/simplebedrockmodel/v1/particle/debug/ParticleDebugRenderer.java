package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.debug;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2: drawn after the world pass ({@link RenderWorldLastEvent} instead of the {@code AFTER_PARTICLES} level
 * stage) while hitboxes are shown (F3+B). The debug command that spawns emitters is not ported.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = SimpleBedrockModel.EVENT_MOD_ID, value = Side.CLIENT)
public class ParticleDebugRenderer {
    private static final double BOX_RADIUS = 0.25D;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager renderManager = mc.getRenderManager();
        if (mc.world == null || !renderManager.isDebugBoundingBox()) {
            return;
        }

        if (WorldEmitterManager.getInstance().getEmitters().isEmpty()) {
            return;
        }

        // 1.12.2: world rendering is relative to the viewer (render view entity) position
        double originX = renderManager.viewerPosX;
        double originY = renderManager.viewerPosY;
        double originZ = renderManager.viewerPosZ;

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        for (WorldEmitterManager.ActiveWorldEmitter emitter : WorldEmitterManager.getInstance().getEmitters()) {
            if (emitter.level != mc.world) {
                continue;
            }

            AxisAlignedBB box = new AxisAlignedBB(
                    emitter.worldX - BOX_RADIUS - originX,
                    emitter.worldY - BOX_RADIUS - originY,
                    emitter.worldZ - BOX_RADIUS - originZ,
                    emitter.worldX + BOX_RADIUS - originX,
                    emitter.worldY + BOX_RADIUS - originY,
                    emitter.worldZ + BOX_RADIUS - originZ
            );
            RenderGlobal.drawSelectionBoundingBox(box, 0.2F, 1.0F, 1.0F, 1.0F);
        }
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
}
