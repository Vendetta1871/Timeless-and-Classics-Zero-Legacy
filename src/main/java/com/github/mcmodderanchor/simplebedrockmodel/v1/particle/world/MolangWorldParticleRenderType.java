package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleDescription;
import com.tacz.guns.legacy.util.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 世界粒子的自定义 {@code ParticleRenderType}。
 * <p>
 * 每个纹理+材质组合对应一个实例，通过 {@link #get(ParticleDescription.Material, ResourceLocation)} 获取。<br/>
 * todo 目前没用atlas，一个贴图一个，考虑优化 ？
 * <p>
 * 1.12.2: vanilla particles have no render types (only the atlas layers 0-2 and the self-drawn layer 3).
 * {@link SnowStormParticle} lives in layer 3 and only queues itself here while vanilla walks the layer; the queues
 * are drawn after the world pass ({@link RenderWorldLastEvent}, the GL modelview is still the camera transform),
 * one draw call per texture/material.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = SimpleBedrockModel.EVENT_MOD_ID, value = Side.CLIENT)
public final class MolangWorldParticleRenderType {

    private static final Map<String, MolangWorldParticleRenderType> CACHE = new ConcurrentHashMap<>();
    /**
     * Render types with queued particles this frame, in first-use order.
     */
    private static final List<MolangWorldParticleRenderType> ACTIVE = new ArrayList<>();

    private final ParticleDescription.Material material;
    private final ResourceLocation texture;
    private final List<SnowStormParticle> queued = new ArrayList<>();

    private MolangWorldParticleRenderType(ParticleDescription.Material material, ResourceLocation texture) {
        this.material = material;
        this.texture = texture;
    }

    /**
     * 获取或创建指定材质+纹理的 RenderType 实例。
     */
    public static MolangWorldParticleRenderType get(ParticleDescription.Material material, ResourceLocation texture) {
        String key = material.name() + ":" + texture;
        return CACHE.computeIfAbsent(key, k -> new MolangWorldParticleRenderType(material, texture));
    }

    /**
     * 清除缓存（资源重载时调用）。
     */
    public static void clearCache() {
        CACHE.clear();
    }

    void queue(SnowStormParticle particle) {
        if (queued.isEmpty()) {
            ACTIVE.add(this);
        }
        queued.add(particle);
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Camera camera = Camera.getMainCamera();
        float partialTicks = event.getPartialTicks();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();

        mc.entityRenderer.enableLightmap();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for (MolangWorldParticleRenderType type : ACTIVE) {
            type.begin(builder);
            for (SnowStormParticle particle : type.queued) {
                particle.render(builder, camera, partialTicks);
            }
            type.end(tessellator);
            type.queued.clear();
        }
        ACTIVE.clear();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        mc.entityRenderer.disableLightmap();
    }

    public void begin(BufferBuilder builder) {
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        switch (material) {
            case PARTICLES_OPAQUE -> {
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
            }
            case PARTICLES_ALPHA -> {
                GlStateManager.depthMask(true);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            case PARTICLES_BLEND -> {
                GlStateManager.depthMask(true);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            case PARTICLES_ADD -> {
                GlStateManager.depthMask(false);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
            case ENERGY_SWIRL, PARTICLES_EMISSIVE -> {
                GlStateManager.depthMask(false);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
        }

        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
    }

    public void end(Tessellator tesselator) {
        tesselator.draw();
    }

    @Override
    public String toString() {
        return "MolangWorldParticleRenderType{" + material + ", " + texture + "}";
    }
}
