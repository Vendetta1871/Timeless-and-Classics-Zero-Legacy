package com.tacz.guns.client.event;

import net.minecraft.client.renderer.GlStateManager;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateContext;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.renderer.crosshair.CrosshairType;
// TODO(port): needs ShoulderSurfingCompat
//import com.tacz.guns.compat.shouldersurfing.ShoulderSurfingCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.render.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class RenderCrosshairEvent {
    private static final ResourceLocation HIT_ICON = new ResourceLocation(GunMod.MOD_ID, "textures/crosshair/hit/hit_marker.png");
    private static final long KEEP_TIME = 300;
    private static boolean isRefitScreen = false;
    private static long hitTimestamp = -1L;
    private static long killTimestamp = -1L;
    private static long headShotTimestamp = -1L;

    /**
     * 当玩家手上拿着枪时，播放特定动画、或瞄准时需要隐藏准心
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        // 1.12.2: overlay ids are element types; the graphics and the window come from the event's resolution
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            GuiGraphics graphics = new GuiGraphics();
            Window window = Window.of(event.getResolution());
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null) {
                return;
            }
            if (!IGun.mainHandHoldGun(player)) {
                return;
            }
            // 全面替换成自己的
            event.setCanceled(true);
            // 击中显示
            renderHitMarker(graphics, window);
            // 换弹进行时取消准心渲染
            ReloadState reloadState = IGunOperator.fromLivingEntity(player).getSynReloadState();
            if (reloadState.getStateType().isReloading()) {
                return;
            }
            // 打开枪械改装界面的时候，取消准心渲染
            if (isRefitScreen) {
                return;
            }
            // 播放的动画需要隐藏准心时，取消准心渲染
            ItemStack stack = player.getHeldItemMainhand();
            if (!(stack.getItem() instanceof IGun)) {
                return;
            }

            IClientPlayerGunOperator playerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            TimelessAPI.getGunDisplay(stack).ifPresent(gunIndex -> {
                // 瞄准快要完成时，取消准心渲染
                if (playerGunOperator.getClientAimingProgress(event.getPartialTicks()) > 0.9) {
                    // 枪包可以强制显示准星
                    boolean forceShow = gunIndex.isShowCrosshair();
                    // 越肩视角可以强制显示准星
                    // TODO(port): needs ShoulderSurfingCompat
                    // boolean shoulderSurfingForceShow = ShoulderSurfingCompat.showCrosshair();
                    boolean shoulderSurfingForceShow = false;
                    // 两个强制都没有时，那么才允许隐藏
                    if (!forceShow && !shoulderSurfingForceShow) {
                        return;
                    }
                }

                AnimationStateMachine<?> animationStateMachine = gunIndex.getAnimationStateMachine();
                if (animationStateMachine == null) {
                    renderCrosshair(graphics, window);
                    return;
                }
                AnimationStateContext context = animationStateMachine.getContext();
                if (context == null || !context.shouldHideCrossHair()) {
                    renderCrosshair(graphics, window);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        // 奇迹的是，RenderGameOverlayEvent.PreLayer 事件中，screen 还未被赋值...
        isRefitScreen = Minecraft.getMinecraft().currentScreen instanceof GunRefitScreen;
    }

    private static void renderCrosshair(GuiGraphics graphics, Window window) {
        GameSettings options = Minecraft.getMinecraft().gameSettings;
        // 越肩视角可以强制显示准星
        // TODO(port): needs ShoulderSurfingCompat
        // boolean shoulderSurfingForceShow = ShoulderSurfingCompat.showCrosshair();
        boolean shoulderSurfingForceShow = false;
        if (options.thirdPersonView != 0 && !shoulderSurfingForceShow) {
            return;
        }
        if (options.hideGUI) {
            return;
        }
        PlayerControllerMP gameMode = Minecraft.getMinecraft().playerController;
        if (gameMode == null) {
            return;
        }
        if (gameMode.getCurrentGameType() == GameType.SPECTATOR) {
            return;
        }
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        ResourceLocation location = CrosshairType.getTextureLocation(RenderConfig.CROSSHAIR_TYPE.get());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, 0.9f);
        float x = width / 2f - 8;
        float y = height / 2f - 8;
        graphics.blit(location, (int) x, (int) y, 0, 0, 16, 16, 16, 16);
    }

    private static void renderHitMarker(GuiGraphics graphics, Window window) {
        long remainHitTime = System.currentTimeMillis() - hitTimestamp;
        long remainKillTime = System.currentTimeMillis() - killTimestamp;
        long remainHeadShotTime = System.currentTimeMillis() - headShotTimestamp;
        float offset = RenderConfig.HIT_MARKET_START_POSITION.get().floatValue();
        float fadeTime;

        if (remainKillTime > KEEP_TIME) {
            if (remainHitTime > KEEP_TIME) {
                return;
            } else {
                fadeTime = remainHitTime;
            }
        } else {
            // 最大位移为 4 像素
            offset += (remainKillTime * 4f) / KEEP_TIME;
            fadeTime = remainKillTime;
        }

        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();
        float x = width / 2f - 8;
        float y = height / 2f - 8;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (remainHeadShotTime > KEEP_TIME) {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1 - fadeTime / KEEP_TIME);
        } else {
            RenderSystem.setShaderColor(1F, 0, 0, 1 - fadeTime / KEEP_TIME);
        }

        graphics.blit(HIT_ICON, (int) (x - offset), (int) (y - offset), 0, 0, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x + 8 + offset), (int) (y - offset), 8, 0, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x - offset), (int) (y + 8 + offset), 0, 8, 8, 8, 16, 16);
        graphics.blit(HIT_ICON, (int) (x + 8 + offset), (int) (y + 8 + offset), 8, 8, 8, 8, 16, 16);
    }

    public static void markHitTimestamp() {
        RenderCrosshairEvent.hitTimestamp = System.currentTimeMillis();
    }

    public static void markKillTimestamp() {
        RenderCrosshairEvent.killTimestamp = System.currentTimeMillis();
    }

    public static void markHeadShotTimestamp() {
        RenderCrosshairEvent.headShotTimestamp = System.currentTimeMillis();
    }
}
