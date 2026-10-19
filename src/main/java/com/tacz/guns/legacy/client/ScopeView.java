package com.tacz.guns.legacy.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
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
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
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
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

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
 * image ({@link #drawLens}) while aiming. The image is drawn screen-aligned at the screen's own scale, so an object
 * at angle {@code a} from the view direction appears at about {@code zoom * a} from the lens centre, with a slight
 * fisheye distortion towards the lens edge.
 * <p>
 * From the hip the eye is not behind the lens, and a real lens does not show the world through itself then: it
 * reflects. So when not aiming, a further world pass turned to look back along the view direction renders into
 * {@link #mirrorFramebuffer}, and {@link #drawMirror} blends that image over the lens: dark, tinted and mirrored
 * left to right, wide-angled like a coated convex lens, stronger towards the lens edge (where the view grazes it),
 * fading out as the player aims in.
 * <p>
 * Only the part of the magnified view that the lens can show is rendered: a square around the view direction, as large
 * as the lens on screen (measured in the previous frame), at the screen's pixel density. Its FOV and frustum are
 * narrow, so the pass draws few chunks and few pixels.
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
    /**
     * Extra room around the lens in the scope view, for the lens growing between frames.
     */
    private static final float VIEW_MARGIN = 1.1F;
    /**
     * The largest scope view, in screen half heights from the view direction.
     */
    private static final float MAX_VIEW_EXTENT = 2;
    private static final int FRAMEBUFFER_STEP = 64;
    /**
     * The mirror view: its texture covers the lens plus this margin, for the lens growing between frames, at this
     * fraction of the screen's pixel density. The mirror runs a whole extra world pass, and its reflection is dark
     * and soft, so a low fraction keeps that pass cheap (0.35 draws about half the pixels of 0.5).
     */
    private static final float MIRROR_MARGIN = 1.1F;
    private static final double MIRROR_RESOLUTION = 0.35;

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final Matrix4f MODELVIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final LensOutline LENS_OUTLINE = new LensOutline();

    @Nullable
    private static Framebuffer framebuffer;
    @Nullable
    private static Framebuffer mirrorFramebuffer;
    private static boolean active;
    private static boolean rendering;
    private static boolean renderingMirror;
    private static boolean ready;
    private static boolean mirrorReady;
    private static float zoom = 1;
    /**
     * The lens radius on screen in the last two frames it was drawn, in screen half heights.
     */
    private static float lastLensRadius = 0.5F;
    private static float lensRadiusBefore = 0.5F;
    /**
     * The scope view of this frame: its half size in screen half heights and its side in pixels.
     */
    private static float viewExtent = 1;
    private static int viewSize = 1;
    /**
     * Whether the framebuffer holds a scope view (rendered in this frame or an earlier one).
     */
    private static boolean hasView;
    /**
     * The mirror view of this frame: whether its framebuffer holds an image, its side in pixels, and how opaque the
     * reflection is (1 from the hip, 0 aiming through the scope).
     */
    private static boolean hasMirrorView;
    private static int mirrorViewSize = 1;
    private static float mirrorAmount;
    /**
     * Whether the lens's size on screen was already measured this frame (by {@link #drawLens} or {@link #drawMirror}).
     */
    private static boolean lensMeasured;
    /**
     * The lens measured for the current frame: its centre in normalized device coordinates, its radius in screen
     * half heights, and the projection's aspect ratio.
     */
    private static float lensCenterX;
    private static float lensCenterY;
    private static float lensRadius;
    private static float lensAspect = 1;
    /**
     * An occlusion query counts the lens pixels drawn: while the lens is not seen (turned away, hidden by the gun
     * or off screen, as in reload animations), no new view is rendered and the lens keeps the last one.
     */
    private static int lensQuery = -1;
    private static boolean lensQueryPending;
    private static boolean lensDrawn;
    private static boolean lensSeen = true;
    private static float worldFov = 70;
    private static boolean shadersChecked;
    @Nullable
    private static Field shaderPackLoaded;
    private static boolean renderLibChecked;
    @Nullable
    private static Field renderLibFrame;

    private ScopeView() {
    }

    /**
     * Whether a scope in first person is handled here this frame: its lens does not go black from the hip and its
     * division follows the eye ({@link #alignDivision}). The lens shows the scope view only when {@link #isReady()},
     * and reflects what is behind the player while {@link #getMirrorAmount()} is above zero.
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Whether a world pass is running for the scope view or for the lens mirror (the FOV is that pass's, the hand is
     * not drawn). While the mirror renders, the camera looks back along the view direction.
     */
    public static boolean isRendering() {
        return rendering;
    }

    /**
     * Whether the running world pass is the mirror's rather than the scope view's.
     */
    public static boolean isRenderingMirror() {
        return renderingMirror;
    }

    /**
     * Whether the scope view was rendered for the current frame: the lens shows it and the world FOV is not zoomed.
     */
    public static boolean isReady() {
        return ready;
    }

    /**
     * Whether the mirror holds an image to reflect in the lens this frame.
     */
    public static boolean isMirrorReady() {
        return mirrorReady;
    }

    /**
     * How opaque the lens reflection is this frame: 0 aiming through the scope, 1 from the hip.
     */
    public static float getMirrorAmount() {
        return mirrorAmount;
    }

    /**
     * The FOV of the scope view: the part of the world FOV (of the previous frame) the lens shows, magnified by the
     * scope's zoom.
     */
    public static float getFov() {
        return (float) MathUtil.magnificationToFov(zoom / viewExtent, worldFov);
    }

    /**
     * The FOV of the mirror pass: a wide angle, so the lens mirrors a lot of what is behind the player.
     */
    public static float getMirrorFov() {
        return RenderConfig.SCOPE_MIRROR_FOV.get().floatValue();
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
        hasView = false;
        mirrorReady = false;
        hasMirrorView = false;
        mirrorAmount = 0;
    }

    /**
     * Called by {@code EntityRenderer#renderWorld} right before the main world pass; {@code worldPass} runs
     * {@code renderWorldPass(2, ..)}.
     */
    public static void renderBeforeWorld(Runnable worldPass) {
        ready = false;
        mirrorReady = false;
        lensMeasured = false;
        Minecraft mc = Minecraft.getMinecraft();
        float scopeZoom = activeZoom(mc);
        active = scopeZoom > 0;
        if (!active) {
            return;
        }
        mirrorAmount = RenderConfig.SCOPE_MIRROR.get() ? MathHelper.clamp(1 - aimingProgress(mc), 0, 1) : 0;
        boolean lensSeen = updateLensSeen();
        // a scope without magnification needs no view of its own: from the hip its lens reflects, and aiming through
        // it the world itself is already at the right scale; while the lens is mostly a mirror its view is not seen
        if (scopeZoom > 1.01F && mirrorAmount < 0.95F) {
            if (!lensSeen && hasView && zoom == scopeZoom) {
                ready = true;
            } else {
                zoom = scopeZoom;
                renderViewPass(worldPass, mc);
            }
        }
        if (mirrorAmount > 0.03F) {
            if (!lensSeen && hasMirrorView) {
                mirrorReady = true;
            } else {
                renderMirrorPass(worldPass, mc);
            }
        }
    }

    /**
     * Renders the view through the scope, ahead of the main world pass, into {@link #framebuffer}.
     */
    private static void renderViewPass(Runnable worldPass, Minecraft mc) {
        // the frame's target may be the vanilla framebuffer or, with OptiFine's fast render, the window itself
        // (creating a framebuffer binds the window, so ask first)
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        VIEWPORT.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        // the lens's size in the last frame, growing as fast as it grew then (while aiming in), with room for the
        // fisheye, which samples up to (1 + fisheye) lens radii out
        float predictedRadius = lastLensRadius * MathHelper.clamp(lastLensRadius / lensRadiusBefore, 1, 1.5F);
        float fisheye = RenderConfig.SCOPE_FISHEYE.get().floatValue();
        viewExtent = MathHelper.clamp(predictedRadius * (1 + fisheye) * VIEW_MARGIN, 0.05F, MAX_VIEW_EXTENT);
        double scale = RenderConfig.SCOPE_VIEW_RESOLUTION.get();
        viewSize = MathHelper.clamp(MathHelper.ceil(viewExtent * mc.displayHeight * scale), 16, Minecraft.getGLMaximumTextureSize());
        // the view is rendered into the corner of a square framebuffer that only grows (up to the largest view)
        int largestSize = roundUp(MathHelper.ceil(MAX_VIEW_EXTENT * mc.displayHeight * scale));
        if (framebuffer == null) {
            framebuffer = new Framebuffer(roundUp(viewSize), roundUp(viewSize), true);
            // guns with scopes held by others use the stencil buffer; Forge re-creates the framebuffer to add one,
            // which must not happen in the middle of the pass
            framebuffer.enableStencil();
            framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
        } else if (framebuffer.framebufferWidth < viewSize || framebuffer.framebufferWidth > largestSize) {
            framebuffer.createBindFramebuffer(roundUp(viewSize), roundUp(viewSize));
            framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
        }
        if (runLensPass(worldPass, mc, framebuffer, viewSize, previousFramebuffer, false)) {
            ready = true;
            hasView = true;
        }
    }

    /**
     * Renders what the lens reflects, ahead of the main world pass, into {@link #mirrorFramebuffer}: the world seen
     * from the eye turned around, to look back along the view direction.
     */
    private static void renderMirrorPass(Runnable worldPass, Minecraft mc) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        VIEWPORT.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
        // the reflection is soft and dark, a fixed fraction of the screen's pixel density is enough even while the
        // lens grows as the player aims in and the reflection fades
        double scale = RenderConfig.SCOPE_VIEW_RESOLUTION.get();
        mirrorViewSize = MathHelper.clamp(roundUp(MathHelper.ceil(MIRROR_RESOLUTION * MIRROR_MARGIN * mc.displayHeight * scale)),
                64, Minecraft.getGLMaximumTextureSize());
        if (mirrorFramebuffer == null) {
            mirrorFramebuffer = new Framebuffer(mirrorViewSize, mirrorViewSize, true);
            mirrorFramebuffer.enableStencil();
            configureMirrorTexture(mirrorFramebuffer);
        } else if (mirrorFramebuffer.framebufferWidth != mirrorViewSize) {
            mirrorFramebuffer.createBindFramebuffer(mirrorViewSize, mirrorViewSize);
            configureMirrorTexture(mirrorFramebuffer);
        }
        if (runLensPass(worldPass, mc, mirrorFramebuffer, mirrorViewSize, previousFramebuffer, true)) {
            mirrorReady = true;
            hasMirrorView = true;
        }
    }

    /**
     * Runs {@code worldPass} (the frame's world pass) as the scope view or the mirror: into {@code target}, at
     * {@code renderSize} pixels square, without the hand, and with that pass's FOV (the mirror turns the camera
     * around before the pass and back after it). Restores everything the main world pass expects.
     */
    private static boolean runLensPass(Runnable worldPass, Minecraft mc, Framebuffer target, int renderSize,
                                       int previousFramebuffer, boolean mirror) {
        int displayWidth = mc.displayWidth;
        int displayHeight = mc.displayHeight;
        Framebuffer mainFramebuffer = mc.getFramebuffer();
        EntityRendererAccessor entityRenderer = (EntityRendererAccessor) mc.entityRenderer;
        boolean renderHand = entityRenderer.tacz$getRenderHand();
        RenderGlobalAccessor renderGlobal = (RenderGlobalAccessor) mc.renderGlobal;
        Entity viewEntity = mirror ? mc.getRenderViewEntity() : null;
        float yaw = 0;
        float pitch = 0;
        float prevYaw = 0;
        float prevPitch = 0;

        // the pass renders the frame as usual, only into the scope's framebuffer (renderWorldPass sizes the viewport
        // and the projection from displayWidth/displayHeight), without the hand and with the pass's FOV
        ((MinecraftAccessor) mc).tacz$setFramebuffer(target);
        mc.displayWidth = renderSize;
        mc.displayHeight = renderSize;
        entityRenderer.tacz$setRenderHand(false);
        if (viewEntity != null) {
            // a lens facing the player reflects the world behind them: look back along the view direction
            yaw = viewEntity.rotationYaw;
            pitch = viewEntity.rotationPitch;
            prevYaw = viewEntity.prevRotationYaw;
            prevPitch = viewEntity.prevRotationPitch;
            viewEntity.rotationYaw = yaw + 180F;
            viewEntity.rotationPitch = -pitch;
            viewEntity.prevRotationYaw = viewEntity.rotationYaw;
            viewEntity.prevRotationPitch = viewEntity.rotationPitch;
        }
        target.bindFramebuffer(true);
        // the visible chunk list is rebuilt only when the camera moves or chunks change; the main pass's list would
        // draw every chunk on screen, so it is rebuilt with this pass's frustum (and again with the main one, below
        // in finally)
        renderGlobal.tacz$setDisplayListEntitiesDirty(true);
        rendering = true;
        renderingMirror = mirror;
        boolean rendered = false;
        try {
            worldPass.run();
            rendered = true;
        } catch (RuntimeException e) {
            GunMod.LOGGER.error(mirror ? "Scope mirror rendering failed" : "Scope view rendering failed", e);
        } finally {
            renderingMirror = false;
            rendering = false;
            if (viewEntity != null) {
                viewEntity.rotationYaw = yaw;
                viewEntity.rotationPitch = pitch;
                viewEntity.prevRotationYaw = prevYaw;
                viewEntity.prevRotationPitch = prevPitch;
            }
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
            // Nothirium (its chunk culling runs on RenderLib's frame id, which is per rendered frame, not per world
            // pass): this pass already consumed this frame id, so the main pass must get a fresh one or its chunk
            // graph walk would skip every chunk recorded by this pass's frustum (only the player's own chunk would
            // render)
            bumpRenderLibFrame();
        }
        return rendered;
    }

    /**
     * The aim state of the local player, 0 from the hip and 1 aiming through the scope.
     */
    private static float aimingProgress(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return 0;
        }
        return IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(mc.getRenderPartialTicks());
    }

    /**
     * Whether the lens was seen in the last frame it was drawn (the query result may come a frame or two late).
     */
    private static boolean updateLensSeen() {
        if (!lensDrawn) {
            lensSeen = false;
        } else if (lensQueryPending && GL15.glGetQueryObjecti(lensQuery, GL15.GL_QUERY_RESULT_AVAILABLE) != GL11.GL_FALSE) {
            lensSeen = GL15.glGetQueryObjecti(lensQuery, GL15.GL_QUERY_RESULT) > 0;
            lensQueryPending = false;
        } else if (!lensQueryPending) {
            lensSeen = true;
        }
        lensDrawn = false;
        return lensSeen;
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
        if (!ready || framebuffer == null || !measureLens(x, y, z)) {
            return;
        }
        float fisheye = RenderConfig.SCOPE_FISHEYE.get().floatValue();
        // the view covers viewExtent screen half heights around the view direction, in the corner of the texture
        float textureScale = (float) viewSize / framebuffer.framebufferTextureWidth;
        // the outermost ring reaches every screen corner
        float screenRadius = MathHelper.sqrt((1 + Math.abs(lensCenterX)) * (1 + Math.abs(lensCenterX)) * lensAspect * lensAspect
                + (1 + Math.abs(lensCenterY)) * (1 + Math.abs(lensCenterY)));

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
                lensVertex(builder, inner, angle0, fisheye, textureScale);
                lensVertex(builder, outer, angle0, fisheye, textureScale);
                lensVertex(builder, outer, angle1, fisheye, textureScale);
                lensVertex(builder, inner, angle1, fisheye, textureScale);
            }
        }
        boolean query = beginLensQuery();
        tessellator.draw();
        endLensQuery(query);

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
     * Draws the reflection over the lens (the caller limits it with the stencil): what the lens mirrors, the world
     * behind the player, blended nearly opaquely over whatever the lens shows (the division fades out together with
     * the mirror, so a mirrored lens shows no reticle, see {@code BedrockAttachmentModel}). Dark and slightly cool
     * tinted like a coated lens, mirrored left to right, and wide-angled like a convex one; the reflection grows
     * towards the lens edge, where the view grazes it, and the whole image fades out as the player aims in
     * ({@link #getMirrorAmount()}).
     */
    public static void drawMirror(float x, float y, float z) {
        if (!mirrorReady || mirrorFramebuffer == null || mirrorAmount <= 0.03F || !measureLens(x, y, z)) {
            return;
        }
        float halfFov = (float) Math.toRadians(RenderConfig.SCOPE_MIRROR_FOV.get() * 0.5);
        float tanHalfFov = Math.max((float) Math.tan(halfFov), 1.0E-3F);
        float textureScale = (float) mirrorViewSize / mirrorFramebuffer.framebufferTextureWidth;
        float screenRadius = MathHelper.sqrt((1 + Math.abs(lensCenterX)) * (1 + Math.abs(lensCenterX)) * lensAspect * lensAspect
                + (1 + Math.abs(lensCenterY)) * (1 + Math.abs(lensCenterY)));

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
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        // Blend by a constant alpha (the per-ring reflection strength) instead of the fragment's own alpha:
        // the mirror framebuffer's alpha channel is the world's and unreliable, and forcing it via texture
        // environment (GL_COMBINE) desynchronizes GlStateManager/OptiFine cached env and blacks the world on
        // window resize. RGB still comes MODULATEd from the texture times the per-ring tint below.
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
        mirrorFramebuffer.bindFramebufferTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        // Mark the lens as drawn so the mirror keeps refreshing its view, but do not wrap the blended ring
        // mesh in an occlusion query: a query over blended draws, with the window's framebuffer re-created by a
        // resize (maximizing), leaves the screen black on some drivers — drawLens's query wraps a single opaque
        // draw and does not hit that
        lensDrawn = true;
        // one tint per ring: the lens is a dark, near-opposite mirror of an unlit coated glass; the rim (the
        // grazing view) is the darkest and fully hides what is behind the lens, its centre catches a little more
        // light and shows only a hint of it
        for (int ring = 0; ring <= RINGS; ring++) {
            float inner = ring * MAX_RING / RINGS * lensRadius;
            float outer = ring < RINGS ? (ring + 1) * MAX_RING / RINGS * lensRadius : Math.max(screenRadius, MAX_RING * lensRadius);
            if (ring == RINGS && outer <= inner) {
                break;
            }
            float t = MathHelper.clamp((inner + outer) * 0.5F / lensRadius, 0, 1);
            float shade = 0.42F - 0.14F * t;
            GlStateManager.color(shade * 0.92F, shade, shade * 1.05F, 1);
            GL14.glBlendColor(0, 0, 0, mirrorAmount * (0.88F + 0.12F * t));
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float angle0 = segment * ((float) Math.PI * 2F) / SEGMENTS;
                float angle1 = (segment + 1) * ((float) Math.PI * 2F) / SEGMENTS;
                mirrorVertex(builder, inner, angle0, halfFov, tanHalfFov, textureScale);
                mirrorVertex(builder, outer, angle0, halfFov, tanHalfFov, textureScale);
                mirrorVertex(builder, outer, angle1, halfFov, tanHalfFov, textureScale);
                mirrorVertex(builder, inner, angle1, halfFov, tanHalfFov, textureScale);
            }
            tessellator.draw();
        }

        mirrorFramebuffer.unbindFramebufferTexture();
        GL14.glBlendColor(0, 0, 0, 0);
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
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
     * Measures the lens on screen (its centre and radius, from the outline the caller rendered into
     * {@link #lensOutline()}, and the projection's aspect), for {@link #drawLens} and {@link #drawMirror}. Whether
     * the lens is in front of the eye at all; the radius is tracked once per frame for the next frame's view sizes.
     */
    private static boolean measureLens(float x, float y, float z) {
        readMatrices();
        Vector4f center = project(x, y, z);
        if (center.w <= 0) {
            return false;
        }
        lensCenterX = MathHelper.clamp(center.x / center.w, -2, 2);
        lensCenterY = MathHelper.clamp(center.y / center.w, -2, 2);
        // distances are measured in units of the screen's half height: x is scaled by the aspect ratio
        lensAspect = Math.abs(PROJECTION.m00()) > 1.0E-6F ? Math.abs(PROJECTION.m11() / PROJECTION.m00()) : 1;
        float radius = 0;
        FloatArrayList positions = LENS_OUTLINE.positions;
        for (int i = 0; i + 2 < positions.size(); i += 3) {
            Vector4f vertex = project(positions.getFloat(i), positions.getFloat(i + 1), positions.getFloat(i + 2));
            if (vertex.w > 0) {
                float dx = (vertex.x / vertex.w - lensCenterX) * lensAspect;
                float dy = vertex.y / vertex.w - lensCenterY;
                radius = Math.max(radius, MathHelper.sqrt(dx * dx + dy * dy));
            }
        }
        if (radius <= 0) {
            radius = 0.5F;
        }
        lensRadius = radius;
        if (!lensMeasured) {
            lensMeasured = true;
            lensRadiusBefore = lastLensRadius;
            lastLensRadius = radius;
        }
        return true;
    }

    /**
     * The mirror's texture: soft, and the fragment's alpha comes from the tint it is drawn with (a world
     * framebuffer's own alpha holds nothing useful), so the reflection can blend over the lens.
     */
    /**
     * The mirror's texture: soft. Its alpha is the framebuffer's own (the world writes near-one alpha), which
     * the default MODULATE environment multiplies with the tint {@link #drawMirror} sets, so the reflection can
     * blend over the lens without any texture environment changes (raw glTexEnvi here desynchronizes
     * GlStateManager's/OptiFine's cached texture env and leaves the world black after a window resize).
     */
    private static void configureMirrorTexture(Framebuffer target) {
        target.setFramebufferFilter(GL11.GL_LINEAR);
    }

    /**
     * A vertex of the mirror's mesh at {@code radius} (in screen half heights) and {@code angle} around the lens
     * centre. The lens shows the mirror's whole field of view, compressed towards its edge like a convex lens does
     * (the angle behind grows with the radius, the texture being a perspective view of it); u is flipped, a mirror
     * does not show what is behind the player as turning around would; past the lens edge the image is clamped.
     */
    private static void mirrorVertex(BufferBuilder builder, float radius, float angle, float halfFov, float tanHalfFov,
                                     float textureScale) {
        float r = Math.min(radius / lensRadius, MIRROR_MARGIN);
        float radial = (float) Math.tan(halfFov * r / MIRROR_MARGIN) / tanHalfFov;
        float dx = MathHelper.cos(angle);
        float dy = MathHelper.sin(angle);
        builder.pos(lensCenterX + dx * radius / lensAspect, lensCenterY + dy * radius, 0)
                .tex((0.5F - 0.5F * dx * radial) * textureScale, (0.5F + 0.5F * dy * radial) * textureScale)
                .endVertex();
    }

    /**
     * The occlusion query over the lens pixels about to be drawn by {@link #drawLens} (only an opaque single
     * draw may be wrapped — see {@link #drawMirror} for why); {@link #updateLensSeen()} reads it next frame.
     */
    private static boolean beginLensQuery() {
        lensDrawn = true;
        boolean query = !lensQueryPending && GLContext.getCapabilities().OpenGL15;
        if (query) {
            if (lensQuery < 0) {
                lensQuery = GL15.glGenQueries();
            }
            GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, lensQuery);
        }
        return query;
    }

    private static void endLensQuery(boolean query) {
        if (query) {
            GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
            lensQueryPending = true;
        }
    }

    /**
     * A vertex at {@code radius} (in screen half heights) and {@code angle} around the lens centre, in normalized
     * device coordinates. The image centre sits on the lens centre; towards the lens edge the image is sampled
     * farther out (barrel distortion), growing with the square of the distance up to {@link #MAX_RING} lens radii.
     */
    private static void lensVertex(BufferBuilder builder, float radius, float angle, float fisheye, float textureScale) {
        float dx = MathHelper.cos(angle) * radius;
        float dy = MathHelper.sin(angle) * radius;
        float edge = Math.min(radius / lensRadius, MAX_RING);
        float sample = (1 + fisheye * edge * edge) / viewExtent;
        builder.pos(lensCenterX + dx / lensAspect, lensCenterY + dy, 0)
                .tex((0.5 + 0.5 * dx * sample) * textureScale, (0.5 + 0.5 * dy * sample) * textureScale)
                .endVertex();
    }

    private static int roundUp(int size) {
        return (size + FRAMEBUFFER_STEP - 1) / FRAMEBUFFER_STEP * FRAMEBUFFER_STEP;
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

    /**
     * RenderLib (Nothirium's dependency) counts rendered frames in a static field; Nothirium's chunk culling dedupes
     * its graph walk by that id ({@code RenderChunk#lastTimeRecorded}). The scope view pass consumed the current id,
     * so the main pass needs the next one. Does nothing when RenderLib is not installed.
     */
    private static void bumpRenderLibFrame() {
        if (!renderLibChecked) {
            renderLibChecked = true;
            try {
                renderLibFrame = Class.forName("meldexun.renderlib.util.RenderUtil").getDeclaredField("frame");
                renderLibFrame.setAccessible(true);
            } catch (ReflectiveOperationException | LinkageError e) {
                renderLibFrame = null;
            }
        }
        if (renderLibFrame != null) {
            try {
                renderLibFrame.setInt(null, renderLibFrame.getInt(null) + 1);
            } catch (IllegalAccessException e) {
                renderLibFrame = null;
            }
        }
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
