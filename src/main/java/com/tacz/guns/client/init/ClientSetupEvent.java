package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.HeatBarOverlay;
import com.tacz.guns.client.gui.overlay.InteractKeyTextOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.input.*;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.legacy.render.GuiGraphics;
import com.tacz.guns.legacy.util.IGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2: there is no mod event bus; {@link com.tacz.guns.legacy.client.LegacyClientSetup} calls the {@code on...}
 * methods at the matching setup stage. Differences from upstream:
 * <ul>
 *     <li>{@code RegisterClientTooltipComponentFactoriesEvent}: tooltip images are created by
 *     {@code legacy.util.TooltipComponentHandler};</li>
 *     <li>{@code RegisterGuiOverlaysEvent}: the overlays are drawn by {@link #onRenderGameOverlay} after the whole HUD;</li>
 *     <li>{@code RegisterClientReloadListenersEvent}: done by {@code LegacyClientSetup#init} ({@code ClientGunPacks}).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class ClientSetupEvent {
    private static final List<IGuiOverlay> OVERLAYS = new ArrayList<>();

    /**
     * {@code RegisterKeyMappingsEvent}
     */
    public static void onClientSetup() {
        // 注册键位
        ClientRegistry.registerKeyBinding(InspectKey.INSPECT_KEY);
        ClientRegistry.registerKeyBinding(ReloadKey.RELOAD_KEY);
        ClientRegistry.registerKeyBinding(ShootKey.SHOOT_KEY);
        ClientRegistry.registerKeyBinding(InteractKey.INTERACT_KEY);
        ClientRegistry.registerKeyBinding(FireSelectKey.FIRE_SELECT_KEY);
        ClientRegistry.registerKeyBinding(AimKey.AIM_KEY);
        ClientRegistry.registerKeyBinding(RefitKey.REFIT_KEY);
        ClientRegistry.registerKeyBinding(ZoomKey.ZOOM_KEY);
        ClientRegistry.registerKeyBinding(MeleeKey.MELEE_KEY);
        ClientRegistry.registerKeyBinding(ConfigKey.OPEN_CONFIG_KEY);
    }

    /**
     * {@code RegisterGuiOverlaysEvent}
     */
    public static void onRegisterGuiOverlays() {
        // 注册 HUD
        OVERLAYS.add(new GunHudOverlay());
        OVERLAYS.add(new HeatBarOverlay());
        OVERLAYS.add(new KillAmountOverlay());
        OVERLAYS.add(new InteractKeyTextOverlay());
    }

    /**
     * {@code FMLClientSetupEvent}: needs the item colors, so it runs in init.
     */
    public static void onClientSetupInit() {
        // 注册自己的的硬编码第三人称动画
        ThirdPersonManager.registerDefault();

        // 注册颜色
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(AmmoBoxItem::getColor, ModItems.AMMO_BOX.get());

        // 注册变种
        Item ammoBox = ModItems.AMMO_BOX.get();
        ammoBox.addPropertyOverride(AmmoBoxItem.PROPERTY_NAME, (stack, world, entity) -> AmmoBoxItem.getStatue(stack, world, entity));

        // 与 Shoulder Surfing Reloaded / Controllable / Accelerated Rendering 的兼容
        // TODO(port): needs ShoulderSurfingCompat (no 1.12.2 version, see README)
        // TODO(port): needs ControllableCompat (no 1.12.2 version, see README)
        // TODO(port): needs ARCompat (no 1.12.2 version, see README)
    }

    /**
     * Draws the overlays above the whole HUD ({@code registerAboveAll}; upstream draws the interact key text right above
     * the crosshair).
     */
    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || OVERLAYS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = event.getResolution();
        GuiGraphics graphics = new GuiGraphics();
        for (IGuiOverlay overlay : OVERLAYS) {
            overlay.render(mc.ingameGUI, graphics, event.getPartialTicks(), resolution.getScaledWidth(), resolution.getScaledHeight());
        }
    }
}
