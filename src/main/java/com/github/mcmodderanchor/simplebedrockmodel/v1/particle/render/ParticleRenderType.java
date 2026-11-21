package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render;

import com.tacz.guns.legacy.render.RenderType;
import com.tacz.guns.legacy.util.Util;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

/**
 * 继承 RenderType 以访问 protected 的 RenderStateShard 常量。
 * <p>
 * 1.12.2: the legacy {@link RenderType} shim is fixed-function GL state; the subclass reaches its protected
 * constructor instead of the 1.20 composite states (no shaders: both types are unlit and full-bright).
 */
@SideOnly(Side.CLIENT)
public abstract class ParticleRenderType extends RenderType {
    private static final Function<ResourceLocation, RenderType> ADDITIVE_PARTICLE= Util.memoize(ParticleRenderType::createAdditiveParticle);
    private static final Function<ResourceLocation, RenderType> EMISSIVE_PARTICLE = Util.memoize(ParticleRenderType::createEmissiveParticle);

    private ParticleRenderType(String name, ResourceLocation texture, boolean writeDepth) {
        super(name, texture, GL11.GL_QUADS, DefaultVertexFormats.ITEM, Transparency.ADDITIVE, false, true, writeDepth);
    }

    public static RenderType additiveParticle(ResourceLocation texture) {
        return ADDITIVE_PARTICLE.apply(texture);
    }

    public static RenderType emissiveParticle(ResourceLocation texture) {
        return EMISSIVE_PARTICLE.apply(texture);
    }

    /**
     * 1.20: entity translucent shader, additive transparency, no cull, color write only (no depth write).
     */
    public static RenderType createAdditiveParticle(ResourceLocation texture) {
        return new ParticleRenderType("particle_additive", texture, false) {
        };
    }

    /**
     * 类似 TACZ 镭射的自发光 RenderType：NEW_ENTITY format + emissive shader + 叠加混合。
     * 自发光不依赖光照传递，且与枪体同 format，AR 加速路径下可与枪体同一加速层被模板剔除。
     */
    public static RenderType createEmissiveParticle(ResourceLocation texture) {
        return new ParticleRenderType("particle_emissive", texture, true) {
        };
    }
}
