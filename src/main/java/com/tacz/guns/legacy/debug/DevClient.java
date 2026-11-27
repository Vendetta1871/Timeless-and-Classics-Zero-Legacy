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
    private static int warmupTicks;
    private static final long SHOT_DELAY_MS = 30;
    /** The screenshot to take {@link #SHOT_DELAY_MS} after {@link #shotTime}, or null. */
    @Nullable
    private static String shotScreenshot;
    private static long shotTime;
    /** A frame later than this after the shot may miss the flash (50 ms): the shot is repeated. */
    private static final long SHOT_LATE_MS = 45;
    private static final int SHOT_TRIES = 3;
    /** The shot screenshot to shoot again for, {@link #reshootDelay} client ticks from now (past the fire rate). */
    @Nullable
    private static String reshoot;
    private static int reshootDelay;
    private static int shotTries;
    /** The sound levels are zeroed once, on the first client tick. */
    private static boolean muted;
    /** First tick of the shooting sequence; -1 until the aiming screenshot is taken. */
    private static int aimBase = -1;
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
        if (!muted) {
            muted = true;
            mute(mc);
        }
        switch (stage) {
            case 0 -> {
                if (mc.currentScreen instanceof GuiMainMenu) {
                    mc.gameSettings.pauseOnLostFocus = false;
                    // TACZ_AUTOTEST_SERVER=host:port joins a (dev) server instead; the player must be op there
                    String server = System.getenv("TACZ_AUTOTEST_SERVER");
                    if (server != null) {
                        mc.displayGuiScreen(new net.minecraft.client.multiplayer.GuiConnecting(mc.currentScreen, mc,
                                new net.minecraft.client.multiplayer.ServerData("tacz_autotest", server, false)));
                        stage = 1;
                        return;
                    }
                    // TACZ_AUTOTEST_WORLD=default: generated terrain (a separate save) instead of the flat world
                    boolean terrain = "default".equals(System.getenv("TACZ_AUTOTEST_WORLD"));
                    WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, terrain ? WorldType.DEFAULT : WorldType.FLAT);
                    settings.enableCommands();
                    String save = terrain ? "tacz_autotest_terrain" : freshWorld() ? "tacz_autotest_fresh" : "tacz_autotest";
                    if (freshWorld()) {
                        // TACZ_AUTOTEST_WORLD=fresh: a new flat world every run, so nothing left by earlier runs shows up
                        mc.getSaveLoader().deleteWorldDirectory(save);
                    }
                    mc.launchIntegratedServer(save, save, settings);
                    stage = 1;
                }
            }
            case 1 -> {
                if (mc.player != null && mc.currentScreen instanceof net.minecraft.client.gui.GuiGameOver) {
                    // the save kept a dead player (e.g. a survival craft test run): respawn instead of waiting forever
                    mc.player.respawnPlayer();
                    mc.displayGuiScreen(null);
                }
                if (mc.player != null && mc.world != null && mc.currentScreen == null && ++ticks > 60) {
                    IntegratedServer server = mc.getIntegratedServer();
                    if (server == null && DevTools.PACK_DIR == null) {
                        // remote server: the same setup through commands
                        String gun = System.getenv("TACZ_AUTOTEST_GUN");
                        mc.player.sendChatMessage("/gamemode 1");
                        mc.player.sendChatMessage("/time set 6000");
                        mc.player.sendChatMessage("/tp @p ~ ~ ~ 0 0");
                        mc.player.sendChatMessage("/give @p tacz:modern_kinetic_gun 1 0 {GunId:\"tacz:" + (gun == null ? "ak47" : gun) + "\",GunCurrentAmmoCount:30}");
                        if (targetKind().equals("zombie")) {
                            mc.player.sendChatMessage("/summon zombie ~ ~ ~8 {NoAI:1b}");
                        } else if (targetKind().equals("enderman")) {
                            mc.player.sendChatMessage("/summon enderman ~ ~ ~8 {NoAI:1b}");
                        }
                    }
                    if (server != null) {
                        server.addScheduledTask(() -> {
                            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                            if (player != null) {
                                // the save may hold a survival player (a craft test run); the sequence expects creative
                                player.setGameType(GameType.CREATIVE);
                                player.inventory.setInventorySlotContents(0, testItem());
                                player.inventory.currentItem = 0;
                                // TACZ_AUTOTEST_LOOK=yaw,pitch: where to face (the default straight ahead may be
                                // blocked by terrain; the scope screenshots want an open view)
                                float yaw = 0F, pitch = 0F;
                                String look = System.getenv("TACZ_AUTOTEST_LOOK");
                                if (look != null) {
                                    String[] parts = look.split(",");
                                    yaw = Float.parseFloat(parts[0].trim());
                                    pitch = Float.parseFloat(parts[1].trim());
                                }
                                player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, yaw, pitch);
                            }
                            // TACZ_AUTOTEST_TIME: the time of day (default noon)
                            server.getWorld(0).setWorldTime(envInt("TACZ_AUTOTEST_TIME", 6000));
                            if (freshWorld()) {
                                // a clean background: no mob spawns, no animals of the world generation, no day cycle
                                net.minecraft.world.GameRules rules = server.getWorld(0).getGameRules();
                                rules.setOrCreateGameRule("doMobSpawning", "false");
                                rules.setOrCreateGameRule("doDaylightCycle", "false");
                                for (net.minecraft.entity.Entity entity : new java.util.ArrayList<>(server.getWorld(0).loadedEntityList)) {
                                    if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
                                        entity.setDead();
                                    }
                                }
                            }
                            if (player != null && DevTools.PACK_DIR == null && (targetKind().equals("zombie") || targetKind().equals("enderman"))) {
                                // a target 8 blocks ahead (the player looks south, yaw 0); body height covers the eye line
                                net.minecraft.entity.monster.EntityMob mob;
                                if (targetKind().equals("enderman")) {
                                    // 1.12.2 vanilla endermen ignore indirect damage sources; bullets must still damage them
                                    mob = new net.minecraft.entity.monster.EntityEnderman(player.world);
                                } else {
                                    net.minecraft.entity.monster.EntityZombie zombie = new net.minecraft.entity.monster.EntityZombie(player.world);
                                    // a helmet keeps it from burning in daylight (the flames would cover the view)
                                    zombie.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD, new ItemStack(net.minecraft.init.Items.LEATHER_HELMET));
                                    mob = zombie;
                                }
                                mob.setPosition(player.posX, player.posY, player.posZ + 8);
                                mob.setNoAI(true);
                                player.world.spawnEntity(mob);
                                target = mob;
                            }
                            if (player != null && DevTools.PACK_DIR == null && targetKind().equals("minecart")) {
                                // a target minecart 4 blocks ahead, seen from the side, with hitboxes shown (the model
                                // must stay inside its bounding box); the save keeps the carts of earlier runs
                                player.world.getEntities(com.tacz.guns.entity.TargetMinecart.class, e -> true).forEach(net.minecraft.entity.Entity::setDead);
                                com.tacz.guns.entity.TargetMinecart cart = new com.tacz.guns.entity.TargetMinecart(player.world, player.posX + 0.5, player.posY, player.posZ + 4);
                                cart.rotationYaw = 90F;
                                player.world.spawnEntity(cart);
                            }
                        });
                    }
                    mc.player.inventory.currentItem = 0;
                    stage = 2;
                    ticks = 0;
                }
            }
            case 2 -> {
                if (mc.player == null) {
                    // disconnected (e.g. the server stopped)
                    mc.shutdown();
                    return;
                }
                // TACZ_AUTOTEST_WARMUP: ticks to idle first (chunks, shader/mixin warm-up), logging the frame rate
                int warmup = Integer.getInteger("tacz.autotest.warmup", envInt("TACZ_AUTOTEST_WARMUP", 0));
                if (warmupTicks < warmup) {
                    if (++warmupTicks % 40 == 0) {
                        GunMod.LOGGER.info("[dev] warmup fps {}", Minecraft.getDebugFPS());
                    }
                    return;
                }
                if (craftTest(mc)) {
                    return;
                }
                if (ammoBoxTest(mc)) {
                    return;
                }
                if (DevBlocksTest.run(mc)) {
                    return;
                }
                if (ticks == 0 && targetKind().equals("dummy")) {
                    spawnDummy(mc);
                }
                if (ticks == 0 && targetKind().equals("minecart")) {
                    mc.getRenderManager().setDebugBoundingBox(true);
                }
                if (++ticks == 40) {
                    screenshot(mc, "first_person");
                    if (DevTools.PACK_DIR == null) {
                        // The dev window sits on a live desktop, so real right-button clicks keep toggling or
                        // cancelling the aim (the test config may even run toggle-aim), and the aiming screenshot
                        // would randomly come out from the hip. Rebind the aim key to a key no real input event
                        // can generate for the rest of the test, and register it there so the synthetic key state
                        // below actually reaches it.
                        try {
                            java.lang.reflect.Field keyCode = net.minecraft.client.settings.KeyBinding.class.getDeclaredField("keyCode");
                            keyCode.setAccessible(true);
                            keyCode.setInt(com.tacz.guns.client.input.AimKey.AIM_KEY, 255);
                            java.lang.reflect.Field hash = net.minecraft.client.settings.KeyBinding.class.getDeclaredField("HASH");
                            hash.setAccessible(true);
                            ((net.minecraftforge.client.settings.KeyBindingMap) hash.get(null)).addKey(255, com.tacz.guns.client.input.AimKey.AIM_KEY);
                        } catch (ReflectiveOperationException e) {
                            GunMod.LOGGER.warn("[dev] failed to rebind the aim key", e);
                        }
                        // hold the aim key: with hold-to-aim (the default) a released key cancels aiming next tick
                        net.minecraft.client.settings.KeyBinding.setKeyBindState(com.tacz.guns.client.input.AimKey.AIM_KEY.getKeyCode(), true);
                        IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(true);
                    }
                } else if (ticks >= 70 && aimBase < 0) {
                    if (DevTools.PACK_DIR != null) {
                        // debug pack mode: no aim/shoot sequence, the checkpoints below stay on the old fixed ticks
                        aimBase = 70;
                    } else {
                        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(mc.player);
                        // Wait until the aim actually engaged and the transition finished before the screenshot: a
                        // stray real mouse event over the dev window can cancel hold-to-aim between the re-assert
                        // below and a fixed tick, which silently made the aiming screenshot come out from the hip.
                        // The deadline only guards against a broken aim, it should not normally be reached.
                        if ((operator.isAim() && operator.getClientAimingProgress(0) >= 1f) || ticks > 110) {
                            GunMod.LOGGER.info("[dev] aim state: isAim={} progress={}", operator.isAim(), operator.getClientAimingProgress(0));
                            screenshot(mc, "aiming");
                            GunMod.LOGGER.info("[dev] target health before: {}", target == null ? null : target.getHealth());
                            aimBase = ticks;
                        }
                    }
                } else if (DevTools.PACK_DIR == null && aimBase >= 0 && ticks > aimBase && ticks <= aimBase + 14
                        && (ticks - aimBase) % 5 == 1) {
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
                    if (ticks == aimBase + 11) {
                        shotPending("shooting");
                    }
                } else if (aimBase >= 0 && ticks > aimBase + 11 && ticks < aimBase + 19 && targetKind().equals("minecart")) {
                    // the target wobble after the last shot, one screenshot per tick (wobble12..wobble18)
                    screenshot(mc, "wobble" + (ticks - aimBase));
                    mc.world.getEntities(com.tacz.guns.entity.TargetMinecart.class, e -> true).forEach(cart -> GunMod.LOGGER.info(
                            "[dev] wobble{}: rolling amplitude {} direction {} damage {}", ticks - aimBase, cart.getRollingAmplitude(), cart.getRollingDirection(), cart.getDamage()));
                } else if (aimBase >= 0 && ticks == aimBase + 19) {
                    if (DevTools.PACK_DIR == null) {
                        GunMod.LOGGER.info("[dev] target health after: {} dead={}", target == null ? null : target.getHealth(), target != null && target.isDead);
                        net.minecraft.client.settings.KeyBinding.setKeyBindState(com.tacz.guns.client.input.AimKey.AIM_KEY.getKeyCode(), false);
                        IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(false);
                    }
                } else if (aimBase >= 0 && ticks == aimBase + 30) {
                    if (DevTools.PACK_DIR == null) {
                        IClientPlayerGunOperator.fromLocalPlayer(mc.player).reload();
                        GunMod.LOGGER.info("[dev] reload");
                    }
                } else if (aimBase >= 0 && ticks == aimBase + 50) {
                    screenshot(mc, "reloading");
                } else if (aimBase >= 0 && ticks == aimBase + 60) {
                    mc.gameSettings.thirdPersonView = 2;
                } else if (aimBase >= 0 && ticks == aimBase + 80) {
                    screenshot(mc, "third_person_front");
                } else if (aimBase >= 0 && ticks == aimBase + 85 && dummy != null) {
                    // the dummy shoots the way a server-broadcast shot of another player arrives: its third-person flash
                    com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager.onShoot(dummy, dummy.getHeldItemMainhand());
                    com.tacz.guns.client.model.functional.MuzzleFlashRender.onEntityShoot(dummy);
                    flashShot("dummy_shooting");
                    GunMod.LOGGER.info("[dev] dummy shoots");
                } else if (aimBase >= 0 && ticks == aimBase + 90) {
                    // the muzzle flash other players see (the reload has finished by now)
                    if (DevTools.PACK_DIR == null) {
                        GunMod.LOGGER.info("[dev] shoot (third person): {}", IClientPlayerGunOperator.fromLocalPlayer(mc.player).shoot());
                        shotPending("third_person_shooting");
                    }
                } else if (aimBase >= 0 && ticks == aimBase + 95) {
                    // the usual third person view, from behind the shooter
                    mc.gameSettings.thirdPersonView = 1;
                } else if (aimBase >= 0 && ticks == aimBase + 105) {
                    if (DevTools.PACK_DIR == null) {
                        GunMod.LOGGER.info("[dev] shoot (third person, back): {}", IClientPlayerGunOperator.fromLocalPlayer(mc.player).shoot());
                        flashShot("third_person_back_shooting");
                    }
                } else if (aimBase >= 0 && ticks == aimBase + 110) {
                    mc.gameSettings.thirdPersonView = 0;
                } else if (aimBase >= 0 && ticks == aimBase + 115 && System.getenv("TACZ_AUTOTEST_STAY") == null) {
                    mc.shutdown();
                }
                // Keep the aim asserted every tick while the sequence wants aiming: a stray real mouse event
                // (the window sits on a desktop) would otherwise cancel hold-to-aim mid-sequence
                if (DevTools.PACK_DIR == null && ticks > 40 && (aimBase < 0 || ticks < aimBase + 19)) {
                    net.minecraft.client.settings.KeyBinding.setKeyBindState(com.tacz.guns.client.input.AimKey.AIM_KEY.getKeyCode(), true);
                    IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(mc.player);
                    if (!operator.isAim()) {
                        operator.aim(true);
                    }
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
    /**
     * The autotest target: {@code zombie} (default), {@code enderman} (vanilla 1.12.2 ignores indirect damage
     * sources, bullets must still damage it) or {@code none} (an unobstructed view, for scope screenshots).
     */
    /**
     * TACZ_AUTOTEST_TARGET=dummy: a client-side copy of a player holding the test item, 3 blocks ahead, a bit to the left and
     * turned sideways, so the screenshots show the third-person gun pose the way other players see it (Player Animator
     * compat or the hardcoded animations).
     */
    /**
     * TACZ_AUTOTEST_CRAFT=<recipe id> ({@code 1} = {@code tacz:gun/ak47}): survival mode, the recipe inputs in the
     * inventory, a gun smith table placed and opened through the real block code, then the "Create" button packet
     * is sent exactly as {@code GunSmithTableScreen#addCraftButton} sends it. Logs the window ids on both sides and
     * the inventory before and after, then quits. Integrated server only.
     *
     * @return true while the craft test runs (it replaces the shooting sequence)
     */
    private static boolean craftTest(Minecraft mc) {
        String value = System.getenv("TACZ_AUTOTEST_CRAFT");
        if (value == null) {
            return false;
        }
        IntegratedServer server = mc.getIntegratedServer();
        if (server == null) {
            GunMod.LOGGER.warn("[dev] craft test needs the integrated server");
            mc.shutdown();
            return true;
        }
        ResourceLocation recipeId = new ResourceLocation("1".equals(value) ? "tacz:gun/ak47" : value);
        int tick = ticks++;
        if (tick == 0) {
            server.addScheduledTask(() -> {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                if (player == null) {
                    return;
                }
                player.setGameType(GameType.SURVIVAL);
                // survival for the real inventory rules, but the flat world spawns slimes: nothing may kill the player
                player.capabilities.disableDamage = true;
                player.sendPlayerAbilities();
                player.inventory.clear();
                String handGun = System.getenv("TACZ_AUTOTEST_CRAFT_HAND");
                if (handGun != null) {
                    // the gun goes into the selected hotbar slot, so the table opens with it in the main hand
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, GunItemBuilder.create()
                            .setId(new ResourceLocation(handGun.contains(":") ? handGun : GunMod.MOD_ID + ":" + handGun)).setAmmoCount(1).build());
                }
                com.tacz.guns.crafting.GunSmithTableRecipe recipe = com.tacz.guns.legacy.crafting.RecipeManager.of(player.world).byKey(recipeId).orElse(null);
                if (recipe == null) {
                    GunMod.LOGGER.warn("[dev] craft: no recipe {}", recipeId);
                    return;
                }
                for (com.tacz.guns.crafting.GunSmithTableIngredient input : recipe.getInputs()) {
                    ItemStack[] matching = input.getIngredient().getMatchingStacks();
                    if (matching.length == 0) {
                        GunMod.LOGGER.warn("[dev] craft: an input of {} matches no item", recipeId);
                        continue;
                    }
                    int left = input.getCount();
                    while (left > 0) {
                        ItemStack stack = matching[0].copy();
                        int count = Math.min(left, stack.getMaxStackSize());
                        stack.setCount(count);
                        player.inventory.addItemStackToInventory(stack);
                        left -= count;
                    }
                }
                net.minecraft.world.World world = player.world;
                net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(player).south(2);
                net.minecraft.block.Block block = com.tacz.guns.init.ModBlocks.GUN_SMITH_TABLE.get();
                net.minecraft.block.state.IBlockState state = block.getDefaultState();
                world.setBlockState(pos, state);
                block.onBlockPlacedBy(world, pos, state, player, com.tacz.guns.api.item.builder.BlockItemBuilder.create(com.tacz.guns.init.ModItems.GUN_SMITH_TABLE.get())
                        .setId(com.tacz.guns.api.DefaultAssets.DEFAULT_BLOCK_ID).build());
                GunMod.LOGGER.info("[dev] craft: recipe {} -> {}, inventory before: {}", recipeId, recipe.getOutput(), describeInventory(player));
            });
        } else if (tick == 20) {
            // a separate tick: the client must have the block entity before the gui handler builds the screen
            server.addScheduledTask(() -> {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                if (player != null) {
                    net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(player).south(2);
                    net.minecraft.block.state.IBlockState state = player.world.getBlockState(pos);
                    state.getBlock().onBlockActivated(player.world, pos, state, player, EnumHand.MAIN_HAND, net.minecraft.util.EnumFacing.UP, 0.5F, 1F, 0.5F);
                }
            });
        } else if (tick == 40) {
            if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiContainer gui) {
                // GunSmithTableScreen#menu is GuiContainer#inventorySlots
                int menuId = gui.inventorySlots.windowId;
                try {
                    java.lang.reflect.Field keysField = gui.getClass().getDeclaredField("recipeKeys");
                    keysField.setAccessible(true);
                    java.lang.reflect.Field listField = gui.getClass().getDeclaredField("filterList");
                    listField.setAccessible(true);
                    com.tacz.guns.client.gui.components.GunPackList list = (com.tacz.guns.client.gui.components.GunPackList) listField.get(gui);
                    GunMod.LOGGER.info("[dev] craft: screen tabs {} namespaces {} byHand {}", ((java.util.Map<?, ?>) keysField.get(gui)).keySet(),
                            list == null ? null : list.namespaceList(), list == null ? null : list.isByHandSelected());
                } catch (ReflectiveOperationException e) {
                    GunMod.LOGGER.warn("[dev] craft: cannot read the screen tabs", e);
                }
                GunMod.LOGGER.info("[dev] craft: client screen={} menu.windowId={} openContainer is menu={} inventoryContainer.windowId={}",
                        gui.getClass().getSimpleName(), menuId, mc.player.openContainer == gui.inventorySlots, mc.player.inventoryContainer.windowId);
                server.addScheduledTask(() -> {
                    EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                    if (player != null) {
                        GunMod.LOGGER.info("[dev] craft: server openContainer={} windowId={}",
                                player.openContainer.getClass().getSimpleName(), player.openContainer.windowId);
                    }
                });
                com.tacz.guns.network.NetworkHandler.CHANNEL.sendToServer(new com.tacz.guns.network.message.ClientMessageCraft(recipeId, menuId));
            } else {
                GunMod.LOGGER.warn("[dev] craft: the gun smith table screen did not open, screen={}", mc.currentScreen);
            }
        } else if (tick == 80) {
            server.addScheduledTask(() -> {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                if (player != null) {
                    GunMod.LOGGER.info("[dev] craft: inventory after: {}", describeInventory(player));
                }
            });
            // the table is still open: the client must already see the inputs gone and the crafted gun picked up
            GunMod.LOGGER.info("[dev] craft: client inventory with the table open: {}", describeInventory(mc.player));
            screenshot(mc, "craft");
        } else if (tick == 90) {
            mc.player.closeScreen();
        } else if (tick == 100) {
            GunMod.LOGGER.info("[dev] craft: after closing, client openContainer.windowId={} inventoryContainer.windowId={}",
                    mc.player.openContainer.windowId, mc.player.inventoryContainer.windowId);
        } else if (tick == 110 && System.getenv("TACZ_AUTOTEST_STAY") == null) {
            mc.shutdown();
        }
        return true;
    }

    /**
     * TACZ_AUTOTEST_AMMOBOX=1: survival mode, an empty ammo box and two stacks of 7.62x39 ammo in the main inventory,
     * the inventory screen open; then the clicks go through {@code PlayerControllerMP#windowClick} exactly as the
     * screen sends them: the box is picked up, right clicked on both ammo stacks (stores them), right clicked on an
     * empty slot (takes a stack out), dragged over a single empty slot (the quick craft that 1.20 turns into a plain
     * click) and put back. Logs the client and server inventories and cursor after each step ({@code [dev] ammobox:}
     * lines), then quits. Integrated server only.
     *
     * @return true while the ammo box test runs (it replaces the shooting sequence)
     */
    private static boolean ammoBoxTest(Minecraft mc) {
        if (System.getenv("TACZ_AUTOTEST_AMMOBOX") == null) {
            return false;
        }
        IntegratedServer server = mc.getIntegratedServer();
        if (server == null) {
            GunMod.LOGGER.warn("[dev] ammo box test needs the integrated server");
            mc.shutdown();
            return true;
        }
        int tick = ticks++;
        // ContainerPlayer: main inventory slots 9-35 keep their numbers
        int boxSlot = 9, ammoSlot1 = 10, ammoSlot2 = 11, emptySlot1 = 12, emptySlot2 = 13;
        if (tick == 0) {
            server.addScheduledTask(() -> {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                if (player == null) {
                    return;
                }
                player.setGameType(GameType.SURVIVAL);
                // the flat world spawns slimes: nothing may kill the player
                player.capabilities.disableDamage = true;
                player.sendPlayerAbilities();
                player.inventory.clear();
                ResourceLocation ammoId = new ResourceLocation(GunMod.MOD_ID, "762x39");
                player.inventory.setInventorySlotContents(boxSlot, new ItemStack(com.tacz.guns.init.ModItems.AMMO_BOX.get()));
                player.inventory.setInventorySlotContents(ammoSlot1, com.tacz.guns.api.item.builder.AmmoItemBuilder.create().setId(ammoId).setCount(60).build());
                player.inventory.setInventorySlotContents(ammoSlot2, com.tacz.guns.api.item.builder.AmmoItemBuilder.create().setId(ammoId).setCount(50).build());
                GunMod.LOGGER.info("[dev] ammobox: server inventory before: {}", describeInventory(player));
            });
        } else if (tick == 20) {
            mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.player));
        } else if (tick == 25) {
            screenshot(mc, "ammobox_before");
        } else if (tick == 30) {
            ammoBoxClick(mc, server, "pick up the box", boxSlot, 0, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 40) {
            ammoBoxClick(mc, server, "right click 60 ammo", ammoSlot1, 1, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 50) {
            ammoBoxClick(mc, server, "right click 50 ammo", ammoSlot2, 1, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 55) {
            // the filled box goes back into its slot for a screenshot (a left click is not overridden)
            ammoBoxClick(mc, server, "put the filled box down", boxSlot, 0, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 58) {
            screenshot(mc, "ammobox_stored");
        } else if (tick == 60) {
            ammoBoxClick(mc, server, "pick up the filled box", boxSlot, 0, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 65) {
            ammoBoxClick(mc, server, "right click an empty slot", emptySlot1, 1, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 70) {
            // GuiContainer#mouseReleased after the mouse moved inside one slot with the right button held
            int window = mc.player.openContainer.windowId;
            mc.playerController.windowClick(window, -999, net.minecraft.inventory.Container.getQuickcraftMask(0, 1), net.minecraft.inventory.ClickType.QUICK_CRAFT, mc.player);
            mc.playerController.windowClick(window, emptySlot2, net.minecraft.inventory.Container.getQuickcraftMask(1, 1), net.minecraft.inventory.ClickType.QUICK_CRAFT, mc.player);
            ammoBoxClick(mc, server, "right drag over a single empty slot", -999, net.minecraft.inventory.Container.getQuickcraftMask(2, 1), net.minecraft.inventory.ClickType.QUICK_CRAFT);
        } else if (tick == 80) {
            ammoBoxClick(mc, server, "put the box back", boxSlot, 0, net.minecraft.inventory.ClickType.PICKUP);
        } else if (tick == 85) {
            screenshot(mc, "ammobox_taken");
        } else if (tick == 90) {
            mc.player.closeScreen();
            server.addScheduledTask(() -> {
                EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
                if (player != null) {
                    GunMod.LOGGER.info("[dev] ammobox: server inventory after: {}", describeInventory(player));
                }
            });
            GunMod.LOGGER.info("[dev] ammobox: client inventory after: {}", describeInventory(mc.player));
        } else if (tick == 100 && System.getenv("TACZ_AUTOTEST_STAY") == null) {
            mc.shutdown();
        }
        return true;
    }

    private static void ammoBoxClick(Minecraft mc, IntegratedServer server, String step, int slot, int button, net.minecraft.inventory.ClickType type) {
        mc.playerController.windowClick(mc.player.openContainer.windowId, slot, button, type, mc.player);
        GunMod.LOGGER.info("[dev] ammobox: {}: client cursor {} inventory {}", step, describeStack(mc.player.inventory.getItemStack()), describeInventory(mc.player));
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            if (player != null) {
                GunMod.LOGGER.info("[dev] ammobox: {}: server cursor {} inventory {}", step, describeStack(player.inventory.getItemStack()), describeInventory(player));
            }
        });
    }

    static String describeStack(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getCount() + "x " + stack.getItem().getRegistryName() + (stack.hasTagCompound() ? stack.getTagCompound() : "");
    }

    /**
     * The autotest runs silently: every sound category is set to zero in memory only (options.txt keeps the user's
     * levels, {@code GameSettings#setSoundLevel} would save them), so {@code SoundManager#playSound} skips each sound;
     * what already plays (the main menu music) is stopped.
     */
    private static void mute(Minecraft mc) {
        java.util.Map<net.minecraft.util.SoundCategory, Float> levels = net.minecraftforge.fml.common.ObfuscationReflectionHelper
                .getPrivateValue(net.minecraft.client.settings.GameSettings.class, mc.gameSettings, "field_186714_aM");
        for (net.minecraft.util.SoundCategory category : net.minecraft.util.SoundCategory.values()) {
            levels.put(category, 0F);
        }
        mc.getSoundHandler().stopSounds();
    }

    static String describeInventory(net.minecraft.entity.player.EntityPlayer player) {
        java.util.List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack.getCount() + "x " + stack.getItem().getRegistryName() + (stack.hasTagCompound() ? stack.getTagCompound() : ""));
            }
        }
        return items.toString();
    }

    private static void spawnDummy(Minecraft mc) {
        dummy = new net.minecraft.client.entity.EntityOtherPlayerMP(mc.world,
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("tacz_dummy".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "tacz_dummy"));
        // the player looks south (+Z); yaw 90 faces west, so the camera sees the dummy's left side
        // 1.5 blocks east (left on screen), clear of the first-person gun
        // TACZ_AUTOTEST_DUMMY_PITCH: the dummy looks up (negative) or down (positive), to check that the gun follows the head
        float pitch = envInt("TACZ_AUTOTEST_DUMMY_PITCH", 0);
        dummy.setLocationAndAngles(mc.player.posX + 1.5, mc.player.posY, mc.player.posZ + 3, 90F, pitch);
        dummy.prevRotationPitch = pitch;
        dummy.rotationYawHead = dummy.prevRotationYawHead = 90F;
        dummy.renderYawOffset = dummy.prevRenderYawOffset = 90F;
        dummy.setHeldItem(EnumHand.MAIN_HAND, testItem());
        // armor layers must follow the gun pose too
        dummy.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST, new ItemStack(net.minecraft.init.Items.IRON_CHESTPLATE));
        dummy.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD, new ItemStack(net.minecraft.init.Items.IRON_HELMET));
        mc.world.addEntityToWorld(-4242, dummy);
    }

    /**
     * TACZ_AUTOTEST_WORLD=fresh: a new flat world for this run (the save {@code tacz_autotest_fresh} is recreated) with
     * mob spawning and the day cycle off, for screenshots with a clean background.
     */
    private static boolean freshWorld() {
        return "fresh".equals(System.getenv("TACZ_AUTOTEST_WORLD"));
    }

    private static String targetKind() {
        return System.getenv().getOrDefault("TACZ_AUTOTEST_TARGET", "zombie");
    }

    private static int envInt(String name, int fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static ItemStack testItem() {
        if (DevTools.PACK_DIR != null) {
            return new ItemStack(DevTools.debugGun);
        }
        String gun = System.getenv("TACZ_AUTOTEST_GUN");
        if ("none".equals(gun)) {
            return ItemStack.EMPTY;
        }
        GunItemBuilder builder = GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, gun == null ? "ak47" : gun)).setAmmoCount(30);
        String scope = System.getenv("TACZ_AUTOTEST_SCOPE");
        if (scope != null) {
            builder.putAttachment(com.tacz.guns.api.item.attachment.AttachmentType.SCOPE, new ResourceLocation(GunMod.MOD_ID, scope));
        }
        String grip = System.getenv("TACZ_AUTOTEST_GRIP");
        if (grip != null) {
            builder.putAttachment(com.tacz.guns.api.item.attachment.AttachmentType.GRIP, new ResourceLocation(GunMod.MOD_ID, grip));
        }
        return builder.build();
    }

    /**
     * The screenshot of a shot is taken {@link #SHOT_DELAY_MS} after it: the muzzle flash quad grows from nothing
     * over its first 25 ms and is gone after 50 ms ({@code MuzzleFlashRender}), so the first frame after the shot
     * showed it at a few pixels at most.
     */
    /** TACZ_AUTOTEST_TARGET=dummy: the client-side dummy, or null. */
    private static net.minecraft.client.entity.EntityOtherPlayerMP dummy;
    /**
     * A third-person shot whose flash is filmed twice: {@code <name>} right after the shot, {@code <name>_late} 70 ms
     * after it. The third-person flash is a world particle emitter: its particles appear on the next client tick and
     * live a few frames, so the first frame may come before them.
     */
    private static String flashShotName;
    private static long flashShotTime = -1;
    private static int flashShots;

    private static void flashShot(String name) {
        flashShotName = name;
        flashShotTime = com.tacz.guns.util.time.GunTime.nowMillis();
        flashShots = 0;
    }

    private static void shotPending(String name) {
        if (!name.equals(shotScreenshot) && !name.equals(reshoot)) {
            shotTries = 0;
        }
        shotScreenshot = name;
        shotTime = com.tacz.guns.util.time.GunTime.nowMillis();
        shotTries++;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (flashShotTime >= 0) {
            long elapsed = com.tacz.guns.util.time.GunTime.nowMillis() - flashShotTime;
            if (flashShots == 0 && elapsed >= SHOT_DELAY_MS) {
                GunMod.LOGGER.info("[dev] {} screenshot {} ms after the shot", flashShotName, elapsed);
                screenshot(Minecraft.getMinecraft(), flashShotName);
                flashShots++;
            } else if (flashShots == 1 && elapsed >= 70) {
                GunMod.LOGGER.info("[dev] {}_late screenshot {} ms after the shot", flashShotName, elapsed);
                screenshot(Minecraft.getMinecraft(), flashShotName + "_late");
                flashShotTime = -1;
            }
        }
        if (shotScreenshot == null) {
            return;
        }
        long elapsed = com.tacz.guns.util.time.GunTime.nowMillis() - shotTime;
        if (elapsed < SHOT_DELAY_MS) {
            return;
        }
        if (elapsed > SHOT_LATE_MS && shotTries < SHOT_TRIES) {
            // a stalled frame: the flash is over, shoot once more
            GunMod.LOGGER.info("[dev] {}: the frame came {} ms after the shot, shooting again", shotScreenshot, elapsed);
            reshoot = shotScreenshot;
            reshootDelay = 3;
            shotScreenshot = null;
            return;
        }
        GunMod.LOGGER.info("[dev] {} screenshot {} ms after the shot", shotScreenshot, elapsed);
        screenshot(Minecraft.getMinecraft(), shotScreenshot);
        shotScreenshot = null;
    }

    @SubscribeEvent
    public static void onClientTickReshoot(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || reshoot == null || mc.player == null || --reshootDelay > 0) {
            return;
        }
        String name = reshoot;
        reshoot = null;
        GunMod.LOGGER.info("[dev] shoot again for {}: {}", name, IClientPlayerGunOperator.fromLocalPlayer(mc.player).shoot());
        shotPending(name);
    }

    static void screenshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(mc.gameDir, "tacz_" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        GunMod.LOGGER.info("[dev] screenshot {} (fps {})", name, Minecraft.getDebugFPS());
    }
}
