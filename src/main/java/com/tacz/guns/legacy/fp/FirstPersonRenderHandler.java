package com.tacz.guns.legacy.fp;

import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderBuffers;
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

import java.util.Optional;

/**
 * Port of SimpleBedrockModel's {@code FirstPersonRenderHandler}: tracks the held item, plays draw / put-away
 * transitions and renders {@link IFPGeoItemRenderer} items in place of the vanilla first-person hand.
 * <p>
 * 1.12.2 differences: hand rendering is hooked through {@link RenderSpecificHandEvent} (the GL matrix already holds
 * view bobbing and hand sway, as the 1.20.1 pose stack does), and there is no off-hand swap event.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Side.CLIENT)
public final class FirstPersonRenderHandler {
    private static int realSelectedSlot = -1;
    private static ItemStack realMainHand = ItemStack.EMPTY;
    private static boolean transitioning = false;
    private static IFPAnimationInstance activeInstance = null;
    private static IFPAnimationInstance previousInstance = null;
    private static ItemStack pendingTarget = ItemStack.EMPTY;
    private static long switchStartTime = 0L;
    private static long currentSheatheDuration = 0L;
    private static boolean lockVanilla = false;
    private static boolean nextIsCustom = false;

    private FirstPersonRenderHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(FirstPersonRenderHandler::reset);
    }

    public static void reset() {
        realSelectedSlot = -1;
        realMainHand = ItemStack.EMPTY;
        transitioning = false;
        activeInstance = null;
        previousInstance = null;
        pendingTarget = ItemStack.EMPTY;
        lockVanilla = false;
        nextIsCustom = false;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        int newSlot = player.inventory.currentItem;
        ItemStack newMain = player.getHeldItemMainhand();

        boolean slotChanged = newSlot != realSelectedSlot;
        boolean itemChanged = !isSameItemStacks(realMainHand, newMain);

        realSelectedSlot = newSlot;
        realMainHand = newMain;

        if (slotChanged || itemChanged) {
            onHeldItemChanged(newMain);
        }
        if (activeInstance != null) {
            activeInstance.updateItem(newMain);
        }
        tickStates();
    }

    /**
     * Upstream has separate slot / same-slot handlers with identical bodies.
     */
    private static void onHeldItemChanged(ItemStack newStack) {
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);
        if (transitioning) {
            return;
        }
        boolean oldIsCustom = activeInstance != null;
        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);
        if (oldIsCustom) {
            // Custom → Any：播放 Putaway
            transitioning = true;
            lockVanilla = true;
            switchStartTime = System.currentTimeMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());
            previousInstance.triggerPutAway();
        } else {
            // Vanilla → Custom / Vanilla：直接切
            transitioning = false;
            lockVanilla = false;
        }
    }

    private static void tickStates() {
        if (transitioning && getSheatheProgress() >= 1.0f) {
            // Putaway 完成
            transitioning = false;
            lockVanilla = false;
            activeInstance = createInstance(pendingTarget);
            previousInstance = null;
        }
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
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
        IFPAnimationInstance instance = getActiveAnimationInstance();
        if (instance == null) {
            return;
        }
        ItemStack stack = instance.currentItem();
        if (stack.isEmpty()) {
            return;
        }
        getRenderer(stack).ifPresent(renderer -> {
            if (event.getHand() == EnumHand.OFF_HAND) {
                if (renderer.blockOffhandRender()) {
                    event.setCanceled(true);
                }
                return;
            }
            float partialTicks = event.getPartialTicks();
            int light = mc.world.getCombinedLight(new BlockPos(player.getPositionEyes(partialTicks)), 0);
            renderer.renderFirstPerson(player, stack, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,
                    new PoseStack(), RenderBuffers.bufferSource(), light, partialTicks);
            RenderBuffers.bufferSource().endBatch();
            event.setCanceled(true);
        });
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
                .map(r -> r.createAnimationInstance(stack, Minecraft.getMinecraft().getRenderViewEntity()))
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
        long elapsed = System.currentTimeMillis() - switchStartTime;
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
