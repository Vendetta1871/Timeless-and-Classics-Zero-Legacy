package com.tacz.guns.legacy.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The subset of 1.20.1 {@code RenderType} that TACZ uses, expressed as fixed-function GL state.
 * <p>
 * Geometry is drawn with vanilla vertex formats ({@link DefaultVertexFormats#ITEM} for entity-like types), and the
 * lightmap is set per draw call rather than per vertex, so OptiFine shaders treat it like vanilla item rendering.
 * Diffuse lighting is inherited from the caller (vanilla enables it around item and hand rendering) unless the type
 * is emissive.
 */
@SideOnly(Side.CLIENT)
public class RenderType {
    private static final Map<String, RenderType> CACHE = new HashMap<>();

    private final String name;
    @Nullable
    private final ResourceLocation texture;
    private final int glMode;
    private final VertexFormat format;
    private final Transparency transparency;
    private final boolean cull;
    private final boolean emissive;
    private final boolean writeDepth;

    protected RenderType(String name, @Nullable ResourceLocation texture, int glMode, VertexFormat format,
                         Transparency transparency, boolean cull, boolean emissive, boolean writeDepth) {
        this.name = name;
        this.texture = texture;
        this.glMode = glMode;
        this.format = format;
        this.transparency = transparency;
        this.cull = cull;
        this.emissive = emissive;
        this.writeDepth = writeDepth;
    }

    private static RenderType entity(String name, ResourceLocation texture, Transparency transparency, boolean cull, boolean emissive) {
        return CACHE.computeIfAbsent(name + ':' + texture, key ->
                new RenderType(name, texture, GL11.GL_QUADS, DefaultVertexFormats.ITEM, transparency, cull, emissive, true));
    }

    public static RenderType entitySolid(ResourceLocation texture) {
        return entity("entity_solid", texture, Transparency.NONE, true, false);
    }

    public static RenderType entityCutout(ResourceLocation texture) {
        return entity("entity_cutout", texture, Transparency.CUTOUT, true, false);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation texture) {
        return entity("entity_cutout_no_cull", texture, Transparency.CUTOUT, false, false);
    }

    public static RenderType entityTranslucent(ResourceLocation texture) {
        return entity("entity_translucent", texture, Transparency.TRANSLUCENT, false, false);
    }

    public static RenderType entityTranslucentCull(ResourceLocation texture) {
        return entity("entity_translucent_cull", texture, Transparency.TRANSLUCENT, true, false);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return entity("entity_translucent_emissive", texture, Transparency.TRANSLUCENT, false, true);
    }

    /**
     * Vanilla scrolls the texture by {@code u, v}; TACZ only passes whole-texture offsets, which are a no-op.
     */
    public static RenderType energySwirl(ResourceLocation texture, float u, float v) {
        return entity("energy_swirl", texture, Transparency.ADDITIVE, false, true);
    }

    /**
     * Stands in for {@code RenderType.create(..)} with a composite state: a textured quad type with the given
     * transparency, culling and emissive (full-bright, unlit) flags.
     */
    public static RenderType create(String name, ResourceLocation texture, Transparency transparency, boolean cull, boolean emissive) {
        return entity(name, texture, transparency, cull, emissive);
    }

    public static RenderType lines() {
        return CACHE.computeIfAbsent("lines", key ->
                new RenderType("lines", null, GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR, Transparency.TRANSLUCENT, false, true, true));
    }

    public String getName() {
        return name;
    }

    @Nullable
    public ResourceLocation getTexture() {
        return texture;
    }

    public int getGlMode() {
        return glMode;
    }

    public VertexFormat getFormat() {
        return format;
    }

    public boolean isTranslucent() {
        return transparency == Transparency.TRANSLUCENT || transparency == Transparency.ADDITIVE || transparency == Transparency.LIGHTNING;
    }

    public boolean isEmissive() {
        return emissive;
    }

    public void setupRenderState() {
        if (texture != null) {
            GlStateManager.enableTexture2D();
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        } else {
            GlStateManager.disableTexture2D();
        }
        switch (transparency) {
            case NONE -> {
                GlStateManager.disableBlend();
                GlStateManager.disableAlpha();
            }
            case CUTOUT -> {
                GlStateManager.disableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            }
            case TRANSLUCENT -> {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            }
            case LIGHTNING -> {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            }
            case ADDITIVE -> {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            }
        }
        if (cull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        if (emissive) {
            GlStateManager.disableLighting();
        }
        GlStateManager.depthMask(writeDepth);
        GlStateManager.enableRescaleNormal();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
    }

    public void clearRenderState(boolean lightingWasEnabled) {
        if (texture == null) {
            GlStateManager.enableTexture2D();
        }
        if (transparency != Transparency.NONE) {
            GlStateManager.disableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        if (emissive && lightingWasEnabled) {
            GlStateManager.enableLighting();
        }
        GlStateManager.depthMask(true);
        GlStateManager.shadeModel(GL11.GL_FLAT);
    }

    /**
     * Applies a packed light value ({@code sky << 20 | block << 4}) as the current lightmap coordinate.
     */
    public void applyLight(int packedLight) {
        int light = emissive ? LightTexture.FULL_BRIGHT : packedLight;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF, light >> 16 & 0xFFFF);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RenderType other)) {
            return false;
        }
        return name.equals(other.name) && Objects.equals(texture, other.texture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, texture);
    }

    @Override
    public String toString() {
        return "RenderType[" + name + (texture == null ? "" : ":" + texture) + "]";
    }

    public enum Transparency {
        NONE, CUTOUT, TRANSLUCENT, ADDITIVE,
        /**
         * {@code SRC_ALPHA, ONE} (vanilla lightning / TACZ laser beams).
         */
        LIGHTNING
    }
}
