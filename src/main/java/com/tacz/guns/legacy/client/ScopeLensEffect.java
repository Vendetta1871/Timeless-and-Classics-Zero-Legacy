package com.tacz.guns.legacy.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.renderer.scope.ScopeVisionMode;
import com.tacz.guns.client.resource.pojo.display.attachment.ScopeLensConfig;
import com.tacz.guns.util.ThermalImagingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2 only: upstream's per-attachment lens effects ({@link ScopeLensConfig}: distortion, chromatic aberration,
 * vignette, tint, night vision and thermal imaging) applied to the {@link ScopeView} image.
 * <p>
 * Upstream renders them with core shaders over the lens area of the screen ({@code ScopeLensRenderer}, with a
 * distance field of the lens contour). Here the scope view is a separate image already drawn on a lens-shaped mesh,
 * so a small GLSL 1.20 program is bound while {@link ScopeView#drawLens} draws it; the distance to the lens edge is
 * that of the measured lens circle. Thermal imaging needs to know what is warm: during the scope view's world pass
 * the heat sources ({@link ThermalImagingHelper#isHeatSource}) are drawn once more into the alpha channel of the scope
 * view's framebuffer (depth tested against the pass, so walls hide them), which the lens program reads as heat.
 * <p>
 * Without GLSL support (or if the program does not compile) the lens shows the plain scope view, as before.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class ScopeLensEffect {
    private static final String LENS_VERTEX = "#version 120\n"
            + "varying vec2 texCoord;\n"
            + "void main() {\n"
            + "    gl_Position = ftransform();\n"
            + "    texCoord = gl_MultiTexCoord0.xy;\n"
            + "}\n";
    // upstream's scope_lens.fsh, sampling the scope view texture instead of the screen
    private static final String LENS_FRAGMENT = "#version 120\n"
            + "uniform sampler2D Sampler0;\n"
            + "uniform vec2 ScreenSize;\n"
            + "uniform vec2 LensCenter;\n"
            + "uniform float LensRadius;\n"
            + "uniform float Aspect;\n"
            + "uniform vec2 TexCenter;\n"
            + "uniform float TexMax;\n"
            + "uniform float Distortion;\n"
            + "uniform float Aberration;\n"
            + "uniform float EdgeWidth;\n"
            + "uniform float VignetteStrength;\n"
            + "uniform float VignetteWidth;\n"
            + "uniform vec3 LensTint;\n"
            + "uniform int VisionMode;\n"
            + "uniform int ThermalReady;\n"
            + "uniform float VisionGain;\n"
            + "uniform float VisionNoise;\n"
            + "uniform float SensorTime;\n"
            + "varying vec2 texCoord;\n"
            + "vec2 clampUv(vec2 uv) { return clamp(uv, vec2(0.0), vec2(TexMax)); }\n"
            + "float luminance(vec3 color) { return dot(color, vec3(0.2126, 0.7152, 0.0722)); }\n"
            + "float grain() {\n"
            + "    vec2 pixel = floor(gl_FragCoord.xy);\n"
            + "    float frame = floor(SensorTime * 30.0);\n"
            + "    return fract(sin(dot(pixel, vec2(12.9898, 78.233)) + frame * 37.719) * 43758.5453) - 0.5;\n"
            + "}\n"
            + "vec3 ironbow(float heat) {\n"
            + "    vec3 cold = vec3(0.015, 0.01, 0.06);\n"
            + "    vec3 purple = vec3(0.30, 0.025, 0.48);\n"
            + "    vec3 red = vec3(0.90, 0.10, 0.035);\n"
            + "    vec3 yellow = vec3(1.0, 0.82, 0.12);\n"
            + "    if (heat < 0.35) return mix(cold, purple, heat / 0.35);\n"
            + "    if (heat < 0.65) return mix(purple, red, (heat - 0.35) / 0.30);\n"
            + "    if (heat < 0.85) return mix(red, yellow, (heat - 0.65) / 0.20);\n"
            + "    return mix(yellow, vec3(1.0), (heat - 0.85) / 0.15);\n"
            + "}\n"
            + "void main() {\n"
            + "    vec2 ndc = gl_FragCoord.xy / ScreenSize * 2.0 - 1.0;\n"
            + "    vec2 relative = vec2((ndc.x - LensCenter.x) * Aspect, ndc.y - LensCenter.y);\n"
            // distance to the lens edge in lens radii (1 at the centre, 0 on the edge)
            + "    float distance = 1.0 - clamp(length(relative) / max(LensRadius, 0.00001), 0.0, 1.0);\n"
            + "    float ramp = 0.0;\n"
            + "    float edge = 0.0;\n"
            + "    if (EdgeWidth > 0.0) {\n"
            + "        float width = 0.155 * EdgeWidth;\n"
            + "        ramp = 1.0 - smoothstep(0.0, min(width * 3.0, 1.0), distance);\n"
            + "    }\n"
            + "    if (VignetteStrength > 0.0 && VignetteWidth > 0.0) {\n"
            + "        float falloff = distance / (0.155 * VignetteWidth);\n"
            + "        edge = exp(-falloff * falloff) * VignetteStrength;\n"
            + "    }\n"
            + "    vec2 relativeUv = texCoord - TexCenter;\n"
            + "    vec2 sampleUv = texCoord - relativeUv * Distortion * ramp;\n"
            + "    bool digital = VisionMode == 1 || VisionMode == 2 || (VisionMode >= 3 && ThermalReady != 0);\n"
            + "    vec2 split = digital ? vec2(0.0) : relativeUv * Aberration * ramp;\n"
            + "    vec4 center = texture2D(Sampler0, clampUv(sampleUv));\n"
            + "    vec3 scene = vec3(texture2D(Sampler0, clampUv(sampleUv - split)).r, center.g,\n"
            + "            texture2D(Sampler0, clampUv(sampleUv + split)).b);\n"
            + "    vec3 color = scene * LensTint;\n"
            + "    if (VisionMode == 1 || VisionMode == 2) {\n"
            + "        float signal = 1.0 - exp(-pow(max(luminance(scene), 0.0), 0.65) * VisionGain);\n"
            + "        signal = clamp(signal + grain() * VisionNoise, 0.0, 1.0);\n"
            + "        vec3 phosphor = VisionMode == 1 ? vec3(0.18, 1.0, 0.28) : vec3(0.82, 0.94, 1.0);\n"
            + "        color = phosphor * signal;\n"
            + "    } else if (VisionMode >= 3 && ThermalReady != 0) {\n"
            + "        float detail = clamp(luminance(scene), 0.0, 1.0);\n"
            + "        float target = center.a;\n"
            + "        float heat = mix(0.06 + detail * 0.24, 0.78 + detail * 0.20, target);\n"
            + "        heat = clamp(heat + grain() * VisionNoise, 0.0, 1.0);\n"
            + "        if (VisionMode == 3) color = vec3(heat);\n"
            + "        else if (VisionMode == 4) color = vec3(1.0 - heat);\n"
            + "        else color = ironbow(heat);\n"
            + "    }\n"
            + "    gl_FragColor = vec4(color * (1.0 - edge), 1.0);\n"
            + "}\n";
    private static final String MASK_VERTEX = "#version 120\n"
            + "varying vec2 texCoord;\n"
            + "void main() {\n"
            + "    gl_Position = ftransform();\n"
            + "    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;\n"
            + "}\n";
    // only the alpha channel is written: 1 where a heat source covers the pixel
    private static final String MASK_FRAGMENT = "#version 120\n"
            + "uniform sampler2D Sampler0;\n"
            + "varying vec2 texCoord;\n"
            + "void main() {\n"
            + "    if (texture2D(Sampler0, texCoord).a < 0.1) discard;\n"
            + "    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n"
            + "}\n";

    private static final ScopeLensConfig DEFAULT_CONFIG = new ScopeLensConfig();

    private static boolean initialized;
    private static int lensProgram;
    private static int maskProgram;
    @Nullable
    private static ScopeLensConfig config;
    /**
     * Whether the scope view's image carries a heat mask in its alpha channel (its last pass drew one).
     */
    private static boolean thermalInView;

    static {
        DEFAULT_CONFIG.init();
    }

    private ScopeLensEffect() {
    }

    /**
     * The lens config of the scope being drawn ({@code BedrockAttachmentModel}, right before {@link ScopeView#drawLens}).
     * The config also decides whether the next scope view pass draws the heat mask.
     */
    public static void setLens(@Nullable ScopeLensConfig lens) {
        config = lens;
    }

    private static ScopeLensConfig config() {
        return config == null ? DEFAULT_CONFIG : config;
    }

    /**
     * Binds the lens program for {@link ScopeView#drawLens}. Returns false (nothing bound) when it is unavailable.
     *
     * @param centerX      lens centre, normalized device coordinates
     * @param centerY      lens centre, normalized device coordinates
     * @param radius       lens radius in screen half heights
     * @param aspect       the projection's aspect ratio
     * @param textureScale the part of the scope view texture holding the image (from its corner)
     */
    public static boolean begin(float centerX, float centerY, float radius, float aspect, float textureScale) {
        if (!init() || lensProgram == 0) {
            return false;
        }
        ScopeLensConfig lens = config();
        Minecraft mc = Minecraft.getMinecraft();
        GL20.glUseProgram(lensProgram);
        uniform1i(lensProgram, "Sampler0", 0);
        uniform2f(lensProgram, "ScreenSize", mc.displayWidth, mc.displayHeight);
        uniform2f(lensProgram, "LensCenter", centerX, centerY);
        uniform1f(lensProgram, "LensRadius", radius);
        uniform1f(lensProgram, "Aspect", aspect);
        uniform2f(lensProgram, "TexCenter", 0.5F * textureScale, 0.5F * textureScale);
        uniform1f(lensProgram, "TexMax", textureScale);
        uniform1f(lensProgram, "Distortion", lens.getDistortion());
        uniform1f(lensProgram, "Aberration", lens.getAberration());
        uniform1f(lensProgram, "EdgeWidth", lens.getEdgeWidth());
        uniform1f(lensProgram, "VignetteStrength", lens.getVignetteStrength());
        uniform1f(lensProgram, "VignetteWidth", lens.getVignetteWidth());
        int tint = lens.getTint();
        float strength = lens.getTintStrength();
        uniform3f(lensProgram, "LensTint",
                1 + (((tint >> 16) & 0xFF) / 255F - 1) * strength,
                1 + (((tint >> 8) & 0xFF) / 255F - 1) * strength,
                1 + ((tint & 0xFF) / 255F - 1) * strength);
        uniform1i(lensProgram, "VisionMode", lens.getVisionMode().ordinal());
        uniform1i(lensProgram, "ThermalReady", lens.getVisionMode().isThermal() && thermalInView ? 1 : 0);
        uniform1f(lensProgram, "VisionGain", lens.getVisionGain());
        uniform1f(lensProgram, "VisionNoise", lens.getVisionNoise());
        uniform1f(lensProgram, "SensorTime", (Minecraft.getSystemTime() % 3_600_000L) / 1000F);
        return true;
    }

    public static void end() {
        GL20.glUseProgram(0);
    }

    /**
     * During the scope view pass (not the mirror), after the world: draws the heat sources into the alpha channel of
     * the scope view's framebuffer when the scope images heat. Lowest priority: after everything else drawn at the end
     * of the world (e.g. SBM's world particles), whose alpha writes would otherwise read as heat.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ScopeView.isRendering() || ScopeView.isRenderingMirror()) {
            return;
        }
        ScopeVisionMode mode = config().getVisionMode();
        thermalInView = mode != null && mode.isThermal() && init() && maskProgram != 0 && renderHeatMask(event.getPartialTicks());
    }

    private static boolean renderHeatMask(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return false;
        }
        Entity viewEntity = mc.getRenderViewEntity();
        RenderManager renderManager = mc.getRenderManager();
        List<Entity> sources = new ArrayList<>();
        double maxDistance = mc.gameSettings.renderDistanceChunks * 16.0;
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity != viewEntity && !entity.isInvisible() && ThermalImagingHelper.isHeatSource(entity)
                    && viewEntity != null && entity.getDistanceSq(viewEntity) < maxDistance * maxDistance) {
                sources.add(entity);
            }
        }
        // clear the alpha channel: nothing is warm yet
        GlStateManager.colorMask(false, false, false, true);
        GlStateManager.clearColor(0, 0, 0, 0);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
        if (!sources.isEmpty()) {
            boolean shadows = renderManager.isRenderShadow();
            renderManager.setRenderShadow(false);
            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableDepth();
            GL20.glUseProgram(maskProgram);
            uniform1i(maskProgram, "Sampler0", 0);
            try {
                for (Entity entity : sources) {
                    renderManager.renderEntityStatic(entity, partialTicks, false);
                }
            } catch (RuntimeException e) {
                GunMod.LOGGER.debug("Thermal mask: an entity failed to render", e);
            } finally {
                GL20.glUseProgram(0);
                GlStateManager.depthMask(true);
                renderManager.setRenderShadow(shadows);
                // as RenderGlobal#renderEntities does after the entities: living entity renderers leave the lightmap
                // texture unit enabled (RenderLivingBase#unsetBrightness), and the next frame's sky, drawn before
                // the terrain enables the lightmap, would be multiplied by it (a grey sky without the sunset glow)
                RenderHelper.disableStandardItemLighting();
                mc.entityRenderer.disableLightmap();
            }
        }
        GlStateManager.colorMask(true, true, true, true);
        return true;
    }

    private static boolean init() {
        if (initialized) {
            return lensProgram != 0;
        }
        initialized = true;
        if (!OpenGlHelper.shadersSupported) {
            GunMod.LOGGER.info("Scope lens effects: GLSL is not supported, scopes show the plain view");
            return false;
        }
        lensProgram = createProgram("lens", LENS_VERTEX, LENS_FRAGMENT);
        maskProgram = createProgram("thermal mask", MASK_VERTEX, MASK_FRAGMENT);
        return lensProgram != 0;
    }

    private static int createProgram(String name, String vertexSource, String fragmentSource) {
        int vertex = compile(name, GL20.GL_VERTEX_SHADER, vertexSource);
        int fragment = compile(name, GL20.GL_FRAGMENT_SHADER, fragmentSource);
        if (vertex == 0 || fragment == 0) {
            return 0;
        }
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            GunMod.LOGGER.error("Scope lens effects: the {} program does not link: {}", name,
                    GL20.glGetProgramInfoLog(program, 4096));
            GL20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static int compile(String name, int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GunMod.LOGGER.error("Scope lens effects: the {} shader does not compile: {}", name,
                    GL20.glGetShaderInfoLog(shader, 4096));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static void uniform1i(int program, String name, int value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    private static void uniform1f(int program, String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private static void uniform2f(int program, String name, float x, float y) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    private static void uniform3f(int program, String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }
}
