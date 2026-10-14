package com.tacz.guns.legacy.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.client.resource.pojo.model.CubesItem;
import com.tacz.guns.legacy.render.Axis;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.OverlayTexture;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Renders one gun model from {@link DevTools#PACK_DIR} through the ported bedrock code and, when
 * {@code TACZ_AUTOTEST} is set, opens a test world and saves screenshots (first person, third person) before quitting.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, value = Side.CLIENT)
public final class DevClient {
    private static final String GUN = System.getenv().getOrDefault("TACZ_DEBUG_GUN", "ak47");
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "debug/gun_texture");
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer()).create();

    private static BedrockModel model;
    private static boolean loadFailed;
    private static int stage;
    private static int ticks;
    private static net.minecraft.entity.EntityLiving target;

    private DevClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!DevTools.ENABLED) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(DevTools.debugGun, 0,
                new ModelResourceLocation(new ResourceLocation(GunMod.MOD_ID, "debug_gun"), "inventory"));
        DevTools.debugGun.setTileEntityItemStackRenderer(new Renderer());
    }

    @Nullable
    private static BedrockModel model() {
        if (model != null || loadFailed) {
            return model;
        }
        try {
            File pack = new File(DevTools.PACK_DIR, "assets/tacz");
            try (Reader reader = Files.newBufferedReader(new File(pack, "geo_models/gun/" + GUN + "_geo.json").toPath(), StandardCharsets.UTF_8)) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                BedrockModelPOJO pojo = GSON.fromJson(jsonReader, BedrockModelPOJO.class);
                BedrockVersion version = BedrockVersion.isLegacyVersion(pojo) ? BedrockVersion.LEGACY : BedrockVersion.NEW;
                model = new BedrockModel(pojo, version);
            }
            DynamicTexture texture = new DynamicTexture(ImageIO.read(new File(pack, "textures/gun/uv/" + GUN + ".png")));
            Minecraft.getMinecraft().getTextureManager().loadTexture(TEXTURE, texture);
            GunMod.LOGGER.info("[dev] loaded debug gun model {}", GUN);
        } catch (Exception e) {
            loadFailed = true;
            GunMod.LOGGER.error("[dev] failed to load debug gun model", e);
        }
        return model;
    }

    private static void renderGun(PoseStack poseStack, ItemCameraTransforms.TransformType transformType, int light) {
        BedrockModel gunModel = model();
        if (gunModel == null) {
            return;
        }
        gunModel.render(poseStack, transformType, RenderType.entityCutout(TEXTURE), light, OverlayTexture.NO_OVERLAY);
    }

    /**
     * Item renderer path (GUI, third person, ground) — same base transform as upstream GunItemRendererWrapper.
     */
    private static final class Renderer extends TileEntityItemStackRenderer {
        @Override
        public void renderByItem(ItemStack stack, float partialTicks) {
            PoseStack poseStack = new PoseStack();
            poseStack.translate(0.5, 2, 0.5);
            poseStack.scale(-1, -1, 1);
            renderGun(poseStack, ItemCameraTransforms.TransformType.NONE, LightTexture.current());
        }
    }

    /**
     * First person: vanilla hand rendering is replaced, as upstream does.
     */
    @SubscribeEvent
    public static void onRenderHand(RenderSpecificHandEvent event) {
        if (event.getHand() != EnumHand.MAIN_HAND || DevTools.debugGun == null || event.getItemStack().getItem() != DevTools.debugGun) {
            return;
        }
        event.setCanceled(true);
        GlStateManager.pushMatrix();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.25, -0.35, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        poseStack.translate(0, 1.5, 0);
        poseStack.scale(-1, -1, 1);
        renderGun(poseStack, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, LightTexture.current());
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!DevTools.ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        switch (stage) {
            case 0 -> {
                if (mc.currentScreen instanceof GuiMainMenu) {
                    mc.gameSettings.pauseOnLostFocus = false;
                    WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
                    settings.enableCommands();
                    mc.launchIntegratedServer("tacz_autotest", "tacz_autotest", settings);
                    stage = 1;
                }
            }
            case 1 -> {
                if (mc.player != null && mc.world != null && mc.currentScreen == null && ++ticks > 60) {
                    IntegratedServer server = mc.getIntegratedServer();
                    if (server != null) {
                        server.addScheduledTask(() -> {
                            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                            if (player != null) {
                                player.inventory.setInventorySlotContents(0, testItem());
                                player.inventory.currentItem = 0;
                                player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, 0F, 0F);
                            }
                            server.getWorld(0).setWorldTime(6000);
                            if (player != null && DevTools.PACK_DIR == null) {
                                // a zombie target 8 blocks ahead (the player looks south, yaw 0); body height covers the eye line
                                net.minecraft.entity.monster.EntityZombie pig = new net.minecraft.entity.monster.EntityZombie(player.world);
                                pig.setPosition(player.posX, player.posY, player.posZ + 8);
                                pig.setNoAI(true);
                                player.world.spawnEntity(pig);
                                target = pig;
                            }
                        });
                    }
                    mc.player.inventory.currentItem = 0;
                    stage = 2;
                    ticks = 0;
                }
            }
            case 2 -> {
                if (++ticks == 40) {
                    screenshot(mc, "first_person");
                    if (DevTools.PACK_DIR == null) {
                        IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(true);
                    }
                } else if (ticks == 70) {
                    if (DevTools.PACK_DIR == null) {
                        screenshot(mc, "aiming");
                        GunMod.LOGGER.info("[dev] target health before: {}", target == null ? null : target.getHealth());
                    }
                } else if (DevTools.PACK_DIR == null && ticks > 70 && ticks <= 85 && ticks % 5 == 1) {
                    GunMod.LOGGER.info("[dev] shoot: {}", IClientPlayerGunOperator.fromLocalPlayer(mc.player).shoot());
                    IntegratedServer srv = mc.getIntegratedServer();
                    if (srv != null) {
                        srv.addScheduledTask(() -> {
                            net.minecraft.world.World w = srv.getWorld(0);
                            GunMod.LOGGER.info("[dev] bullets={} target={} player={} look={}",
                                    w.getEntities(com.tacz.guns.entity.EntityKineticBullet.class, e -> true).stream()
                                            .map(b -> String.format("(%.1f %.1f %.1f v=%.2f %.2f %.2f)", b.posX, b.posY, b.posZ, b.motionX, b.motionY, b.motionZ))
                                            .collect(java.util.stream.Collectors.toList()),
                                    target == null ? null : target.getPositionVector(), mc.player.getPositionVector(), mc.player.getLookVec());
                        });
                    }
                    if (ticks == 81) {
                        screenshot(mc, "shooting");
                    }
                } else if (ticks == 89) {
                    if (DevTools.PACK_DIR == null) {
                        GunMod.LOGGER.info("[dev] target health after: {} dead={}", target == null ? null : target.getHealth(), target != null && target.isDead);
                        IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(false);
                    }
                    mc.gameSettings.thirdPersonView = 2;
                } else if (ticks == 110) {
                    screenshot(mc, "third_person_front");
                    mc.gameSettings.thirdPersonView = 0;
                } else if (ticks == 120 && System.getenv("TACZ_AUTOTEST_STAY") == null) {
                    mc.shutdown();
                }
            }
            default -> {
            }
        }
    }

    /**
     * The real gun item ({@code TACZ_AUTOTEST_GUN}, default {@code ak47}), or the debug model when
     * {@code TACZ_DEBUG_PACK} points at an unpacked gun pack.
     */
    private static ItemStack testItem() {
        if (DevTools.PACK_DIR != null) {
            return new ItemStack(DevTools.debugGun);
        }
        String gun = System.getenv("TACZ_AUTOTEST_GUN");
        return GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, gun == null ? "ak47" : gun)).setAmmoCount(30).build();
    }

    private static void screenshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(mc.gameDir, "tacz_" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        GunMod.LOGGER.info("[dev] screenshot {}", name);
    }
}
