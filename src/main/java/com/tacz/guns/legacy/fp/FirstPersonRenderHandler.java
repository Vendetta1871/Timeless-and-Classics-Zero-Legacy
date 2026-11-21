package com.tacz.guns.legacy.fp;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderBuffers;
import com.tacz.guns.legacy.util.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.Optional;

/**
 * Port of SimpleBedrockModel's (2.6.3) {@code v1.client.handler.FirstPersonRenderHandler}: tracks the held item,
 * plays draw / put-away transitions, renders {@link IFPGeoItemRenderer} items in place of the vanilla first-person
 * hand and owns the global first-person particle system.
 * <p>
 * 1.12.2 differences: hand rendering is hooked through {@link RenderSpecificHandEvent} (the GL matrix already holds
 * view bobbing and hand sway, as the 1.20.1 pose stack does, so the pose stack starts at identity), and there is no
 * off-hand swap event.
 * TODO(port): only the main hand renders custom items (upstream keeps a full draw / put-away state per hand).
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Side.CLIENT)
public final class FirstPersonRenderHandler {
    private static final AnimationClock CLOCK = AnimationClocks.client();

    /** 全局第一人称粒子系统。所有自定义物品共享此实例，统一管理粒子生命周期和渲染。 */
    private static final FirstPersonParticleSystem PARTICLE_SYSTEM = new FirstPersonParticleSystem();
    private static long lastParticleTickNanos = 0L;

    private static int realSelectedSlot = -1;
    private static ItemStack realMainHand = ItemStack.EMPTY;
    private static boolean transitioning = false;
    private static IFPAnimationInstance activeInstance = null;
    private static IFPAnimationInstance previousInstance = null;
    private static ItemStack pendingTarget = ItemStack.EMPTY;
    /** 当前活跃 instance 的渲染变体键（{@link IFPGeoItemRenderer#getRenderVariantKey}）。 */
    private static Object variantKey = null;
    private static long switchStartTime = 0L;
    private static long currentSheatheDuration = 0L;
    private static boolean lockVanilla = false;
    private static boolean nextIsCustom = false;
    private static boolean wasFirstPerson = true;

    private FirstPersonRenderHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // 1.12.2: fired on the network thread
        Minecraft.getMinecraft().addScheduledTask(FirstPersonRenderHandler::reset);
    }

    public static void reset() {
        realSelectedSlot = -1;
        realMainHand = ItemStack.EMPTY;
        transitioning = false;
        activeInstance = null;
        previousInstance = null;
        pendingTarget = ItemStack.EMPTY;
        variantKey = null;
        lockVanilla = false;
        nextIsCustom = false;
        wasFirstPerson = true;
        PARTICLE_SYSTEM.clear();
        lastParticleTickNanos = 0L;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !CLOCK.shouldTick()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return;
        }

        boolean isFirstPerson = mc.gameSettings.thirdPersonView == 0;
        if (wasFirstPerson && !isFirstPerson) {
            // 离开第一人称后不再有 RenderHandEvent 更新定位器；保留 loop emitter 会以最后
            // 一帧手部变换持续发射，故立即废弃本地载体。
            PARTICLE_SYSTEM.discardEmitters();
        }
        wasFirstPerson = isFirstPerson;

        int newSlot = player.inventory.currentItem;
        ItemStack newMain = player.getHeldItemMainhand();

        boolean slotChanged = newSlot != realSelectedSlot;
        boolean itemChanged = !isSameItemStacks(realMainHand, newMain);
        boolean variantChanged = variantKeyChanged(newMain);

        realSelectedSlot = newSlot;
        // 存拷贝而非引用：物品被取走时 Minecraft 会原地把槽位 ItemStack 的 count 改为 0
        if (itemChanged) {
            realMainHand = newMain.copy();
        }

        if (slotChanged || itemChanged || variantChanged) {
            beginSwitch(newMain, slotChanged);
        }
        if (activeInstance != null) {
            activeInstance.updateItem(newMain);
        }
        tickStates(newMain);
    }

    private static boolean variantKeyChanged(ItemStack heldItem) {
        if (activeInstance == null || heldItem.isEmpty()) {
            return false;
        }
        return !Objects.equals(getRenderVariantKey(heldItem), variantKey);
    }

    private static Object getRenderVariantKey(ItemStack stack) {
        return getRenderer(stack)
                .map(r -> r.getRenderVariantKey(stack, EnumHand.MAIN_HAND))
                .orElse(null);
    }

    /**
     * 切换物品 / 渲染变体：旧 instance 若为自定义物品，进入收枪过渡；否则直接切到新 instance。
     *
     * @param force 是否穿透幂等短路（切格子等「值层面无差异但确需重建」的明确外部事件）
     */
    private static void beginSwitch(ItemStack newStack, boolean force) {
        Object newVariantKey = getRenderVariantKey(newStack);
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);
        if (transitioning) {
            return;
        }
        // 幂等保护：当前活跃 instance 已代表「同一持有物 + 同一渲染变体」时不重复切换
        if (!force
                && !newStack.isEmpty()
                && activeInstance != null
                && isSameItemStacks(activeInstance.currentItem(), newStack)
                && Objects.equals(variantKey, newVariantKey)) {
            return;
        }
        boolean oldIsCustom = activeInstance != null;
        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);
        variantKey = newVariantKey;
        if (oldIsCustom) {
            // Custom → Any：播放 Putaway
            transitioning = true;
            lockVanilla = true;
            switchStartTime = CLOCK.nowMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());
            previousInstance.triggerPutAway();
            // 零时长收枪没有后续动画帧可供 tickStates 收尾，必须在当前 tick 完成切换
            if (currentSheatheDuration <= 0L) {
                finishTransition(newStack);
            }
        } else {
            // Vanilla → Custom / Vanilla：直接切
            transitioning = false;
            lockVanilla = false;
        }
    }

    private static void tickStates(ItemStack heldItem) {
        if (transitioning && getSheatheProgress() >= 1.0f) {
            finishTransition(heldItem);
        }
    }

    private static void finishTransition(ItemStack heldItem) {
        // previousInstance 不再渲染，局部粒子也失去正确的模型空间基准
        PARTICLE_SYSTEM.discardEmitters(EnumHand.MAIN_HAND);
        transitioning = false;
        lockVanilla = false;
        ItemStack target = heldItem.copy();
        activeInstance = createInstance(target);
        variantKey = getRenderVariantKey(target);
        previousInstance = null;
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !CLOCK.shouldTick()) {
            return;
        }

        // 统一 tick 所有第一人称粒子（不依赖具体物品）
        long now = CLOCK.nowNanos();
        float dt = lastParticleTickNanos == 0L ? 0f : (now - lastParticleTickNanos) / 1_000_000_000f;
        dt = Math.min(dt, 0.1f);
        lastParticleTickNanos = now;
        if (Minecraft.getMinecraft().world != null) {
            PARTICLE_SYSTEM.tick(dt);
        }

        IFPAnimationInstance animation = getActiveAnimationInstance();
        if (animation != null) {
            animation.triggerDraw();
            animation.tick(event.renderTickTime);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderSpecificHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) {
            return;
        }
        EnumHand hand = event.getHand();
        float partialTicks = event.getPartialTicks();
        int light = mc.world.getCombinedLight(new BlockPos(player.getPositionEyes(partialTicks)), 0);

        IFPAnimationInstance instance = getActiveAnimationInstance();
        ItemStack stack = instance == null ? ItemStack.EMPTY : instance.currentItem();
        Optional<IFPGeoItemRenderer> optRenderer = stack.isEmpty() ? Optional.empty() : getRenderer(stack);
        if (!optRenderer.isPresent()) {
            renderParticlesIfAny(hand, light, partialTicks);
            return;
        }
        IFPGeoItemRenderer renderer = optRenderer.get();

        if (hand == EnumHand.OFF_HAND) {
            // 主手物品霸占副手视野（如双手长枪）
            if (renderer.blockOffhandRender(stack)) {
                renderParticlesIfAny(hand, light, partialTicks);
                event.setCanceled(true);
            }
            return;
        }

        // 物品自身判定能否在该手渲染
        if (!renderer.canRenderInHand(stack, hand)) {
            renderParticlesIfAny(hand, light, partialTicks);
            event.setCanceled(true);
            return;
        }

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = RenderBuffers.bufferSource();
        // 更新该手粒子发射器变换（在自己的 pass 内捕获基准、绑定枪口）。
        renderer.updateParticleEmitterTransforms(PARTICLE_SYSTEM, poseStack, hand);
        renderer.renderFirstPerson(player, stack, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,
                poseStack, bufferSource, light, partialTicks);
        // 渲染器声明在模型渲染窗口内自行渲染粒子时，跳过事件末尾的自动渲染，避免双重绘制
        if (!renderer.renderParticlesInModelWindow()) {
            renderParticlesNow(hand, poseStack, bufferSource, light, partialTicks);
        }
        bufferSource.endBatch();
        event.setCanceled(true);
    }

    /**
     * 渲染当前 pass 这只手的活跃粒子（没有自定义渲染器接管这只手时）。
     */
    private static void renderParticlesIfAny(EnumHand hand, int light, float partialTicks) {
        if (PARTICLE_SYSTEM.getParticleCount() == 0) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = RenderBuffers.bufferSource();
        renderParticlesNow(hand, new PoseStack(), bufferSource, light, partialTicks);
        bufferSource.endBatch();
    }

    /**
     * 立即渲染指定手的活跃粒子。供模组在枪体模板窗口内调用（如让粒子被镜内剔除）。
     * <p>
     * 调用方需持有与该手枪体渲染一致的 {@code poseStack} / {@code bufferSource}。
     */
    public static void renderParticlesNow(EnumHand hand, PoseStack poseStack, MultiBufferSource bufferSource,
                                          int light, float partialTick) {
        if (PARTICLE_SYSTEM.getParticleCount() == 0) {
            return;
        }

        Camera camera = Camera.getMainCamera();
        float cameraPitchRad = (float) Math.toRadians(camera.getXRot());
        float cameraRollRad = CameraStateCache.getCameraRollRadians();
        Matrix4f cameraRotation = buildCameraRotation(camera, cameraRollRad);

        PARTICLE_SYSTEM.render(
                hand,
                poseStack,
                bufferSource,
                light,
                partialTick,
                cameraPitchRad, cameraRollRad, cameraRotation
        );
    }

    /**
     * 用摄像机的 pitch/yaw/roll 构建视图旋转矩阵（与 Minecraft 内部一致）。
     */
    private static Matrix4f buildCameraRotation(Camera camera, float rollRadians) {
        return new Matrix4f()
                .rotationX((float) Math.toRadians(camera.getXRot()))
                .rotateY((float) Math.toRadians(camera.getYRot() + 180f))
                .rotateZ(rollRadians);
    }

    /**
     * 获取全局第一人称粒子系统。所有自定义物品渲染器通过此方法添加发射器。
     */
    public static FirstPersonParticleSystem getParticleSystem() {
        return PARTICLE_SYSTEM;
    }

    public static boolean shouldLockVanilla() {
        return lockVanilla;
    }

    public static float getTargetHeight() {
        return nextIsCustom ? 1.0F : 0.0F;
    }

    public static IFPAnimationInstance getActiveAnimationInstance() {
        return transitioning ? previousInstance : activeInstance;
    }

    private static IFPAnimationInstance createInstance(ItemStack stack) {
        return getRenderer(stack)
                .map(r -> r.createAnimationInstance(stack, Minecraft.getMinecraft().getRenderViewEntity(), EnumHand.MAIN_HAND))
                .orElse(null);
    }

    private static boolean isCustomItem(ItemStack stack) {
        return getRenderer(stack).isPresent();
    }

    private static long calculateSheatheDuration(ItemStack stack) {
        return getRenderer(stack).map(r -> r.getPutAwayDuration(stack)).orElse(0L);
    }

    private static float getSheatheProgress() {
        if (currentSheatheDuration <= 0) {
            return 1.0f;
        }
        long elapsed = CLOCK.nowMillis() - switchStartTime;
        return Math.min(1.0f, (float) elapsed / currentSheatheDuration);
    }

    private static Optional<IFPGeoItemRenderer> getRenderer(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        TileEntityItemStackRenderer renderer = stack.getItem().getTileEntityItemStackRenderer();
        if (renderer instanceof IFPGeoItemRenderer fpRenderer) {
            return Optional.of(fpRenderer);
        }
        return Optional.empty();
    }

    private static boolean isSameItemStacks(ItemStack oldStack, ItemStack newStack) {
        if (oldStack == newStack) {
            return true;
        }
        if (oldStack.isEmpty() && newStack.isEmpty()) {
            return true;
        }
        if (oldStack.isEmpty() || newStack.isEmpty()) {
            return false;
        }
        return getRenderer(oldStack)
                .map(r -> r.isSameItem(oldStack, newStack))
                .orElseGet(() -> ItemStack.areItemsEqual(oldStack, newStack));
    }
}
