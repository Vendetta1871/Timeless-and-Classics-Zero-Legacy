package com.tacz.guns.legacy.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.legacy.render.VertexConsumer;
import com.tacz.guns.mixin.client.EntityRendererAccessor;
import com.tacz.guns.mixin.client.MinecraftAccessor;
import com.tacz.guns.mixin.client.RenderGlobalAccessor;
import com.tacz.guns.util.math.MathUtil;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 1.12.2 only: picture-in-picture scopes.
 * <p>
 * Upstream magnifies a scope by zooming the whole world (the world FOV) while aiming and leaves the lens see-through,
 * black when not aiming. Here the world keeps its FOV. Before the main world pass, the view through the scope is
 * rendered by a second world pass with the magnified FOV into {@link #framebuffer}, and the scope's lens shows that
 * image ({@link #drawLens}), aimed or not. The image is drawn screen-aligned at the screen's own scale, so an object
 * at angle {@code a} from the view direction appears at about {@code zoom * a} from the lens centre, with a slight
 * fisheye distortion towards the lens edge.
 * <p>
 * Off (upstream behaviour) when disabled in the config, when framebuffers are unavailable and under OptiFine
 * shaders.
 */
@SideOnly(Side.CLIENT)
public final class ScopeView {
    /**
     * The polar mesh of the lens image: rings (in lens radii) up to where the distortion stops growing.
     */
    private static final int RINGS = 24;
    private static final float MAX_RING = 1.5F;
    private static final int SEGMENTS = 48;

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final Matrix4f MODELVIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final LensOutline LENS_OUTLINE = new LensOutline();

    @Nullable
    private static Framebuffer framebuffer;
    private static boolean active;
    private static boolean rendering;
    private static boolean ready;
    private static float zoom = 1;
    private static float worldFov = 70;
    private static boolean shadersChecked;
    @Nullable
    private static Field shaderPackLoaded;

    private ScopeView() {
    }

    /**
     * Whether a scope in first person is handled here this frame: its lens stays clear from the hip and its division
     * follows the eye ({@link #alignDivision}). The lens shows the scope view only when {@link #isReady()}.
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Whether the world pass for the scope view is running (the FOV is the scope's, the hand is not drawn).
     */
    public static boolean isRendering() {
        return rendering;
    }

    /**
     * Whether the scope view was rendered for the current frame: the lens shows it and the world FOV is not zoomed.
     */
    public static boolean isReady() {
        return ready;
    }

    /**
     * The FOV of the scope view: the world FOV (of the previous frame) magnified by the scope's zoom.
     */
    public static float getFov() {
        return (float) MathUtil.magnificationToFov(zoom, worldFov);
    }

    public static void recordWorldFov(float fov) {
        worldFov = fov;
    }

    /**
     * Called by {@code EntityRenderer#renderWorld} for a world pass other than the normal one (anaglyph).
     */
    public static void reset() {
        active = false;
        ready = false;
    }

    /**
     * Called by {@code EntityRenderer#renderWorld} right before the main world pass; {@code worldPass} runs
     * {@code renderWorldPass(2, ..)}.
     */
    public static void renderBeforeWorld(Runnable worldPass) {
        ready = false;
        Minecraft mc = Minecraft.getMinecraft();
        float scopeZoom = activeZoom(mc);
        active = scopeZoom > 0;
        // a scope without magnification needs no view of its own, its lens just stays clear
        if (scopeZoom <= 1.01F) {
            return;
        }
        zoom = scopeZoom;
        // the frame's target may be the vanilla framebuffer or, with OptiFine's fast render, the window itself
        // (creating a framebuffer binds the window, so ask first)
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        VIEWPORT.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        double scale = RenderConfig.SCOPE_VIEW_RESOLUTION.get();
        int width = Math.max(1, (int) (mc.displayWidth * scale));
        int height = Math.max(1, (int) (mc.displayHeight * scale));
        if (framebuffer == null) {
            framebuffer = new Framebuffer(width, height, true);
            // guns with scopes held by others use the stencil buffer; Forge re-creates the framebuffer to add one,
            // which must not happen in the middle of the pass
            framebuffer.enableStencil();
            framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
        } else if (framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            framebuffer.createBindFramebuffer(width, height);
            framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
        }

        int displayWidth = mc.displayWidth;
        int displayHeight = mc.displayHeight;
        Framebuffer mainFramebuffer = mc.getFramebuffer();
        EntityRendererAccessor entityRenderer = (EntityRendererAccessor) mc.entityRenderer;
        boolean renderHand = entityRenderer.tacz$getRenderHand();
        RenderGlobalAccessor renderGlobal = (RenderGlobalAccessor) mc.renderGlobal;

        // the pass renders the frame as usual, only into the scope framebuffer (renderWorldPass sizes the viewport
        // and the projection from displayWidth/displayHeight), without the hand and with the scope's FOV
        ((MinecraftAccessor) mc).tacz$setFramebuffer(framebuffer);
        mc.displayWidth = width;
        mc.displayHeight = height;
        entityRenderer.tacz$setRenderHand(false);
        framebuffer.bindFramebuffer(true);
        // the visible chunk list is rebuilt only when the camera moves; if this pass rebuilds it, it is built with
        // the narrow frustum, so the main pass (below, in finally) is forced to rebuild it with the wide one. When the
        // camera stands still this pass reuses the main pass's list, which is a superset of the scope frustum.
        rendering = true;
        try {
            worldPass.run();
            ready = true;
        } catch (RuntimeException e) {
            GunMod.LOGGER.error("Scope view rendering failed", e);
        } finally {
            rendering = false;
            // force the main pass to rebuild the visible chunk list with its own (wide) frustum
            renderGlobal.tacz$setDisplayListEntitiesDirty(true);
            entityRenderer.tacz$setRenderHand(renderHand);
            mc.displayWidth = displayWidth;
            mc.displayHeight = displayHeight;
            ((MinecraftAccessor) mc).tacz$setFramebuffer(mainFramebuffer);
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, previousFramebuffer);
            GlStateManager.viewport(VIEWPORT.get(0), VIEWPORT.get(1), VIEWPORT.get(2), VIEWPORT.get(3));
            // the state renderWorld sets up before its world pass
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.5F);
        }
    }

    /**
     * The zoom of the scope on the gun in first person, or 0 when there is none to handle.
     */
    private static float activeZoom(Minecraft mc) {
        if (!RenderConfig.SCOPE_PICTURE_IN_PICTURE.get() || !OpenGlHelper.isFramebufferEnabled() || isShaderPackLoaded()) {
            return 0;
        }
        if (mc.player == null || mc.getRenderViewEntity() != mc.player || mc.gameSettings.thirdPersonView != 0
                || mc.gameSettings.anaglyph) {
            return 0;
        }
        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) {
            return 0;
        }
        ResourceLocation scopeId = iGun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (scopeId.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeId = iGun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        }
        if (DefaultAssets.isEmptyAttachmentId(scopeId)
                || !TimelessAPI.getClientAttachmentIndex(scopeId).map(ClientAttachmentIndex::isScope).orElse(false)) {
            return 0;
        }
        return Math.max(iGun.getAimingZoom(stack), 1);
    }

    /**
     * The division (reticle and the black plate around it) sits far ahead of the lens and lines up with it only from
     * the scope's axis; from anywhere else (from the hip) the plate covers the lens. This shears the division's pose
     * along the scope's axis so that the axis runs through the eye: the division then stays centred on the lens, as
     * if the eye were behind the scope. Poses are relative to the current GL modelview matrix.
     *
     * @param divisionPose the pose the division is rendered with, changed in place
     * @param ocularPose   the pose of the lens (its origin is the lens centre, its -Z the direction of the scope)
     */
    public static void alignDivision(Matrix4f divisionPose, Matrix4f ocularPose) {
        readMatrices();
        Vector4f eye = MODELVIEW.invert(new Matrix4f()).transform(new Vector4f(0, 0, 0, 1));
        Vector3f lens = ocularPose.getTranslation(new Vector3f());
        Vector3f axis = ocularPose.transformDirection(new Vector3f(0, 0, -1));
        Vector3f toLens = new Vector3f(lens).sub(eye.x, eye.y, eye.z);
        if (axis.lengthSquared() < 1.0E-12F || toLens.lengthSquared() < 1.0E-12F) {
            return;
        }
        axis.normalize();
        float depth = toLens.dot(axis);
        if (depth < 0) {
            axis.negate();
            depth = -depth;
        }
        // seen from the side, the shear would stretch the plate across the screen
        if (depth < 0.2F * toLens.length()) {
            return;
        }
        // p' = p + u * ((p - lens) . axis), u = (the part of toLens across the axis) / depth: a point on the axis at
        // distance d ahead of the lens moves to lens + toLens * d / depth, on the line from the eye through the lens
        Vector3f u = new Vector3f(axis).mul(-depth).add(toLens).div(depth);
        float offset = -lens.dot(axis);
        Matrix4f shear = new Matrix4f(
                1 + u.x * axis.x, u.y * axis.x, u.z * axis.x, 0,
                u.x * axis.y, 1 + u.y * axis.y, u.z * axis.y, 0,
                u.x * axis.z, u.y * axis.z, 1 + u.z * axis.z, 0,
                u.x * offset, u.y * offset, u.z * offset, 1);
        divisionPose.mulLocal(shear);
    }

    /**
     * A vertex consumer that collects the lens's vertices (render the lens into it before {@link #drawLens}); the
     * fisheye distortion is scaled to the lens's size on screen.
     */
    public static VertexConsumer lensOutline() {
        LENS_OUTLINE.positions.clear();
        return LENS_OUTLINE;
    }

    /**
     * Draws the scope view over the lens (the caller limits it with the stencil), centred on the lens centre
     * {@code (x, y, z)}, given relative to the current GL modelview matrix.
     */
    public static void drawLens(float x, float y, float z) {
        if (!ready || framebuffer == null) {
            return;
        }
        readMatrices();
        Vector4f center = project(x, y, z);
        if (center.w <= 0) {
            return;
        }
        float centerX = MathHelper.clamp(center.x / center.w, -2, 2);
        float centerY = MathHelper.clamp(center.y / center.w, -2, 2);
        // distances are measured in units of the screen's half height: x is scaled by the aspect ratio
        float aspect = Math.abs(PROJECTION.m00()) > 1.0E-6F ? Math.abs(PROJECTION.m11() / PROJECTION.m00()) : 1;
        float lensRadius = 0;
        FloatArrayList positions = LENS_OUTLINE.positions;
        for (int i = 0; i + 2 < positions.size(); i += 3) {
            Vector4f vertex = project(positions.getFloat(i), positions.getFloat(i + 1), positions.getFloat(i + 2));
            if (vertex.w > 0) {
                float dx = (vertex.x / vertex.w - centerX) * aspect;
                float dy = vertex.y / vertex.w - centerY;
                lensRadius = Math.max(lensRadius, MathHelper.sqrt(dx * dx + dy * dy));
            }
        }
        if (lensRadius <= 0) {
            lensRadius = 0.5F;
        }
        float fisheye = RenderConfig.SCOPE_FISHEYE.get().floatValue();
        // the outermost ring reaches every screen corner
        float screenRadius = MathHelper.sqrt((1 + Math.abs(centerX)) * (1 + Math.abs(centerX)) * aspect * aspect
                + (1 + Math.abs(centerY)) * (1 + Math.abs(centerY)));

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        framebuffer.bindFramebufferTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (int ring = 0; ring <= RINGS; ring++) {
            float inner = ring * MAX_RING / RINGS * lensRadius;
            float outer = ring < RINGS ? (ring + 1) * MAX_RING / RINGS * lensRadius : Math.max(screenRadius, MAX_RING * lensRadius);
            if (ring == RINGS && outer <= inner) {
                break;
            }
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float angle0 = segment * ((float) Math.PI * 2F) / SEGMENTS;
                float angle1 = (segment + 1) * ((float) Math.PI * 2F) / SEGMENTS;
                lensVertex(builder, inner, angle0, centerX, centerY, aspect, lensRadius, fisheye);
                lensVertex(builder, outer, angle0, centerX, centerY, aspect, lensRadius, fisheye);
                lensVertex(builder, outer, angle1, centerX, centerY, aspect, lensRadius, fisheye);
                lensVertex(builder, inner, angle1, centerX, centerY, aspect, lensRadius, fisheye);
            }
        }
        tessellator.draw();

        framebuffer.unbindFramebufferTexture();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        if (fog) {
            GlStateManager.enableFog();
        }
        if (lighting) {
            GlStateManager.enableLighting();
        }
        if (lightmap) {
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }

    /**
     * A vertex at {@code radius} (in screen half heights) and {@code angle} around the lens centre, in normalized
     * device coordinates. The image centre sits on the lens centre; towards the lens edge the image is sampled
     * farther out (barrel distortion), growing with the square of the distance up to {@link #MAX_RING} lens radii.
     */
    private static void lensVertex(BufferBuilder builder, float radius, float angle, float centerX, float centerY,
                                   float aspect, float lensRadius, float fisheye) {
        float dx = MathHelper.cos(angle) * radius;
        float dy = MathHelper.sin(angle) * radius;
        float edge = Math.min(radius / lensRadius, MAX_RING);
        float sample = 1 + fisheye * edge * edge;
        builder.pos(centerX + dx / aspect, centerY + dy, 0)
                .tex(0.5 + 0.5 * dx * sample / aspect, 0.5 + 0.5 * dy * sample)
                .endVertex();
    }

    private static void readMatrices() {
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        MODELVIEW.set(MATRIX);
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, MATRIX);
        PROJECTION.set(MATRIX);
    }

    private static Vector4f project(float x, float y, float z) {
        Vector4f vector = new Vector4f(x, y, z, 1);
        MODELVIEW.transform(vector);
        PROJECTION.transform(vector);
        return vector;
    }

    private static boolean isShaderPackLoaded() {
        if (!shadersChecked) {
            shadersChecked = true;
            try {
                shaderPackLoaded = Class.forName("net.optifine.shaders.Shaders").getField("shaderPackLoaded");
            } catch (ReflectiveOperationException | LinkageError e) {
                shaderPackLoaded = null;
            }
        }
        if (shaderPackLoaded == null) {
            return false;
        }
        try {
            return shaderPackLoaded.getBoolean(null);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static final class LensOutline implements VertexConsumer {
        private final FloatArrayList positions = new FloatArrayList();

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }
    }
}
