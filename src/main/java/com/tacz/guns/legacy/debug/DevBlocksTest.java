package com.tacz.guns.legacy.debug;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.block.entity.GunDisplayBlockEntity;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.client.input.InteractKey;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.inventory.AmmoStorageEntry;
import com.tacz.guns.inventory.AmmoStorageInventory;
import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.legacy.util.MinecraftAccess;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageAmmoStorageClick;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.FMLCommonHandler;
import javax.annotation.Nullable;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * TACZ_AUTOTEST_BLOCKS=1: the TACZ 1.2.0 blocks (gun display case, gun pegboards, ammo storage) driven through the
 * real client input path, in survival: the player is turned towards a point, then the use / attack / interact key
 * actions run exactly as a key press would run them ({@code Minecraft#rightClickMouse}, {@code Minecraft#clickMouse},
 * {@link InteractKey#onInteractPress()}), so the client ray trace, the client and server block code and the packets
 * all take part. Every step logs {@code [dev] blocks:} lines with what the crosshair hit and the block entity
 * contents on both sides; the last step compares them. Integrated server only. Best run with
 * {@code TACZ_AUTOTEST_WORLD=fresh TACZ_AUTOTEST_TARGET=none}.
 * <p>
 * Layout: the player faces south (+Z); three blocks ahead stand, from screen right to left (west to east), the ammo
 * storage, the glow pegboard, the pegboard and two display cases (they connect into one case).
 */
@SideOnly(Side.CLIENT)
final class DevBlocksTest {
    private static final Method CLICK_MOUSE = ObfuscationReflectionHelper.findMethod(Minecraft.class, "func_147116_af", void.class);
    private static final Field LEFT_CLICK_COUNTER = ObfuscationReflectionHelper.findField(Minecraft.class, "field_71429_W");
    /** Ticks per step: aim on the first tick, act on {@link #ACT}, log on {@link #LOG}. */
    private static final int STEP = 12;
    private static final int ACT = 4;
    private static final int LOG = 10;

    private static final int SLOT_CASES = 0, SLOT_PEGBOARD = 1, SLOT_GLOW = 2, SLOT_STORAGE = 3, SLOT_GUN_A = 4,
            SLOT_GUN_B = 5, SLOT_GUN_C = 6, SLOT_AMMO_BOX = 7, SLOT_EMPTY = 8;
    private static final ResourceLocation AMMO_762 = new ResourceLocation(GunMod.MOD_ID, "762x39");

    private static BlockPos base;
    private static int tick;
    private static final List<Step> STEPS = new ArrayList<>();
    private static int failures;
    private static int takenOut;

    private DevBlocksTest() {
    }

    /**
     * @return true while the blocks test runs (it replaces the shooting sequence)
     */
    static boolean run(Minecraft mc) {
        if (System.getenv("TACZ_AUTOTEST_BLOCKS") == null) {
            return false;
        }
        IntegratedServer server = mc.getIntegratedServer();
        if (server == null) {
            GunMod.LOGGER.warn("[dev] blocks test needs the integrated server");
            mc.shutdown();
            return true;
        }
        if (base == null) {
            base = new BlockPos(mc.player);
            buildSteps();
            setup(mc, server);
            tick = -20;
        }
        int t = tick++;
        if (t < 0) {
            return true;
        }
        int index = t / STEP;
        if (index >= STEPS.size()) {
            if (t == STEPS.size() * STEP + 10) {
                GunMod.LOGGER.info("[dev] blocks: done, {} failed check(s)", failures);
                if (System.getenv("TACZ_AUTOTEST_STAY") == null) {
                    mc.shutdown();
                }
            }
            return true;
        }
        Step step = STEPS.get(index);
        int phase = t % STEP;
        if (phase == 0) {
            if (step.slot >= 0) {
                mc.player.inventory.currentItem = step.slot;
            }
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), step.sneak);
            if (step.aim != null) {
                aim(mc, step.aim);
            }
        } else if (phase < ACT && step.aim != null) {
            // held every tick until the action: the eye height changes while the player starts or stops sneaking,
            // and the server must have the final rotation (its own ray trace decides what an attack hits)
            aim(mc, step.aim);
        } else if (phase == ACT) {
            if (step.aim != null) {
                float yaw = mc.player.rotationYaw, pitch = mc.player.rotationPitch;
                aim(mc, step.aim);
                if (Math.abs(yaw - mc.player.rotationYaw) > 0.01F || Math.abs(pitch - mc.player.rotationPitch) > 0.01F) {
                    GunMod.LOGGER.info("[dev] blocks: {}: the view drifted by yaw {} pitch {} since the last tick", step.name,
                            yaw - mc.player.rotationYaw, pitch - mc.player.rotationPitch);
                }
                // the crosshair target of this tick (the frame may not have run since the turn)
                mc.entityRenderer.getMouseOver(1.0F);
                RayTraceResult hit = mc.objectMouseOver;
                GunMod.LOGGER.info("[dev] blocks: {}: crosshair {} {} {} holding {} sneaking {}", step.name,
                        hit == null ? null : hit.typeOfHit, hit == null ? null : hit.getBlockPos(),
                        hit == null || hit.hitVec == null ? null : String.format("(%.3f %.3f %.3f)", hit.hitVec.x, hit.hitVec.y, hit.hitVec.z),
                        DevClient.describeStack(mc.player.getHeldItemMainhand()), mc.player.isSneaking());
            }
            if (step.action != null) {
                step.action.accept(mc);
            }
        } else if (phase == LOG && step.check != null) {
            step.check.accept(mc);
        }
        return true;
    }

    private static void buildSteps() {
        STEPS.clear();
        BlockPos caseA = base.add(2, 0, 3), caseB = base.add(1, 0, 3), pegboard = base.add(0, 0, 3),
                glow = base.add(-1, 0, 3), storage = base.add(-2, 0, 3);
        // placing: the top face of the ground block under each position
        STEPS.add(new Step("place display case A", SLOT_CASES, false, top(caseA.down()), DevBlocksTest::use, null));
        // a display case connects to the one it is placed against (clicked on): case A's west side
        STEPS.add(new Step("place display case B (against case A)", SLOT_CASES, false,
                new Vec3d(caseA.getX(), caseA.getY() + 0.5, caseA.getZ() + 0.6), DevBlocksTest::use, null));
        STEPS.add(new Step("place the pegboard", SLOT_PEGBOARD, false, top(pegboard.down()), DevBlocksTest::use, null));
        STEPS.add(new Step("place the glow pegboard", SLOT_GLOW, false, top(glow.down()), DevBlocksTest::use, null));
        STEPS.add(new Step("place the ammo storage", SLOT_STORAGE, false, top(storage.down()), DevBlocksTest::use,
                mc -> {
                    logBlocks(mc, "placed", caseA, caseB, pegboard, glow, storage);
                    expectBlock(mc, caseA, "gun_display_case");
                    expectBlock(mc, caseB, "gun_display_case");
                    expectBlock(mc, pegboard, "gun_pegboard");
                    expectBlock(mc, glow, "glow_gun_pegboard");
                    expectBlock(mc, storage, "ammo_storage");
                    expect("the placed blocks were taken from the hotbar", mc.player.inventory.getStackInSlot(SLOT_CASES).isEmpty()
                            && mc.player.inventory.getStackInSlot(SLOT_STORAGE).isEmpty());
                }));
        STEPS.add(new Step("screenshot placed", SLOT_EMPTY, false, center(pegboard), mc -> DevClient.screenshot(mc, "blocks_placed"), null));
        // the display case is placed locked: sneak + use with empty hands unlocks it (and the case connected to it)
        STEPS.add(new Step("unlock the display case", SLOT_EMPTY, true, center(caseA), DevBlocksTest::use,
                mc -> {
                    logBlocks(mc, "unlocked", caseA, caseB);
                    expect("display case A unlocked", !locked(mc.world, caseA));
                    expect("display case B (connected) unlocked", !locked(mc.world, caseB));
                }));
        STEPS.add(new Step("gun into display case A (interact key)", SLOT_GUN_A, false, face(caseA, 0.6, 0.6), DevBlocksTest::interact, null));
        STEPS.add(new Step("gun into display case B (interact key)", SLOT_GUN_B, false, face(caseB, 0.6, 0.6), DevBlocksTest::interact, null));
        STEPS.add(new Step("gun onto the pegboard (interact key)", SLOT_GUN_C, false, face(pegboard, 0.6, 0.6), DevBlocksTest::interact,
                mc -> {
                    logBlocks(mc, "guns placed", caseA, caseB, pegboard);
                    expect("display case A holds a gun", hasItem(mc, caseA));
                    expect("display case B holds a gun", hasItem(mc, caseB));
                    expect("the pegboard holds a gun", hasItem(mc, pegboard));
                    expect("survival: the placed guns left the hotbar", mc.player.inventory.getStackInSlot(SLOT_GUN_A).isEmpty()
                            && mc.player.inventory.getStackInSlot(SLOT_GUN_C).isEmpty());
                }));
        STEPS.add(new Step("rotate the pegboard gun (empty hand)", SLOT_EMPTY, false, face(pegboard, 0.6, 0.6), DevBlocksTest::use,
                mc -> {
                    logBlocks(mc, "rotated", pegboard);
                    expect("the pegboard gun (center slot 10) rotated", rotation(mc.world, pegboard, 10) != 0);
                }));
        STEPS.add(new Step("screenshot filled (crosshair on the pegboard slot)", SLOT_EMPTY, false, face(pegboard, 0.6, 0.6),
                mc -> DevClient.screenshot(mc, "blocks_filled"), null));
        STEPS.add(new Step("take the gun out of display case A (attack)", SLOT_EMPTY, false, face(caseA, 0.6, 0.6), DevBlocksTest::attack,
                mc -> {
                    logBlocks(mc, "taken", caseA);
                    expectBlock(mc, caseA, "gun_display_case");
                    expect("display case A is empty again", display(mc.world, caseA) != null && !hasItem(mc, caseA));
                    expect("the gun was dropped (survival)", droppedGuns(mc, caseA) == 1);
                    logDrops(mc, caseA);
                }));
        // a fourth gun for the glow pegboard, in a slot that the placed gun left empty
        STEPS.add(new Step("give a gun for the glow pegboard", SLOT_EMPTY, false, null, mc -> onServer(mc, player ->
                player.inventory.setInventorySlotContents(SLOT_GUN_A, gun("hcar"))), null));
        // the lower west corner slot (slot 0), not the center one
        STEPS.add(new Step("gun onto the glow pegboard, corner slot (interact key)", SLOT_GUN_A, false,
                face(glow, 0.1, 0.1), DevBlocksTest::interact,
                mc -> {
                    logBlocks(mc, "glow pegboard", glow);
                    expect("the glow pegboard holds a gun in slot 0", display(mc.world, glow) != null && !display(mc.world, glow).getItem(0).isEmpty());
                }));
        STEPS.add(new Step("open the ammo storage", SLOT_EMPTY, false, center(storage), DevBlocksTest::use,
                mc -> {
                    GunMod.LOGGER.info("[dev] blocks: ammo storage screen {}", mc.currentScreen == null ? null : mc.currentScreen.getClass().getSimpleName());
                    expect("the ammo storage screen opened", mc.player.openContainer instanceof AmmoStorageMenu);
                }));
        // AmmoStorageMenu: slots 0-26 are the main inventory (player slots 9-35), 27-35 the hotbar
        STEPS.add(new Step("shift click 60 ammo into the storage", -1, false, null, mc -> windowClick(mc, 0, 0, ClickType.QUICK_MOVE), null));
        STEPS.add(new Step("shift click 40 ammo into the storage", -1, false, null, mc -> windowClick(mc, 1, 0, ClickType.QUICK_MOVE),
                mc -> {
                    logStorage(mc, storage);
                    expect("the storage holds 100 7.62x39", storedCount(mc, storage) == 100);
                }));
        STEPS.add(new Step("screenshot ammo storage", -1, false, null, mc -> DevClient.screenshot(mc, "ammo_storage"), null));
        STEPS.add(new Step("take half of an entry", -1, false, null, mc -> {
            if (mc.player.openContainer instanceof AmmoStorageMenu menu && !menu.getEntries().isEmpty()) {
                NetworkHandler.CHANNEL.sendToServer(new ClientMessageAmmoStorageClick(menu.windowId, menu.getEntries().get(0).id(), AmmoStorageMenu.Action.PICKUP_HALF));
            }
        }, mc -> GunMod.LOGGER.info("[dev] blocks: carried {}", DevClient.describeStack(mc.player.inventory.getItemStack()))));
        STEPS.add(new Step("put the carried ammo into an empty slot", -1, false, null, mc -> windowClick(mc, 2, 0, ClickType.PICKUP),
                mc -> {
                    logStorage(mc, storage);
                    takenOut = mc.player.inventory.getStackInSlot(11).getCount();
                    expect("half a stack was taken out (" + takenOut + ")", takenOut > 0 && storedCount(mc, storage) == 100 - takenOut);
                }));
        STEPS.add(new Step("close the ammo storage", -1, false, null, mc -> mc.player.closeScreen(), null));
        STEPS.add(new Step("fill the ammo box from the storage", SLOT_AMMO_BOX, false, center(storage), DevBlocksTest::use,
                mc -> {
                    logStorage(mc, storage);
                    GunMod.LOGGER.info("[dev] blocks: ammo box {}", DevClient.describeStack(mc.player.inventory.getStackInSlot(SLOT_AMMO_BOX)));
                    expect("the ammo box was filled", boxCount(mc) > 1);
                }));
        STEPS.add(new Step("unload the ammo box into the storage (sneak)", SLOT_AMMO_BOX, true, center(storage), DevBlocksTest::use,
                mc -> {
                    logStorage(mc, storage);
                    GunMod.LOGGER.info("[dev] blocks: ammo box {}", DevClient.describeStack(mc.player.inventory.getStackInSlot(SLOT_AMMO_BOX)));
                    // the box went in with 1 round and was filled from the storage
                    expect("the ammo box was unloaded", boxCount(mc) == 0 && storedCount(mc, storage) == 100 - takenOut + 1);
                }));
        STEPS.add(new Step("screenshot final", SLOT_EMPTY, false, new Vec3d(base.getX() + 0.5, base.getY(), base.getZ() + 3.5),
                mc -> DevClient.screenshot(mc, "blocks_final"),
                mc -> compareSides(mc, caseA, caseB, pegboard, glow)));
        buildAdvancementSteps();
    }

    /**
     * The gun pack advancements (GunPackAdvancements): loaded on both sides, and granted by the inventory, the
     * display case use above, kills with a gun in hand and a crafting table recipe.
     */
    private static void buildAdvancementSteps() {
        STEPS.add(new Step("advancements: loaded, inventory and display case", SLOT_EMPTY, false, null, null, mc -> {
            int server = 0;
            for (Advancement advancement : mc.getIntegratedServer().getAdvancementManager().getAdvancements()) {
                if (GunMod.MOD_ID.equals(advancement.getId().getNamespace())) {
                    server++;
                }
            }
            int client = 0;
            for (Advancement advancement : mc.player.connection.getAdvancementManager().getAdvancementList().getAdvancements()) {
                if (GunMod.MOD_ID.equals(advancement.getId().getNamespace())) {
                    client++;
                }
            }
            GunMod.LOGGER.info("[dev] blocks: tacz advancements: server {}, client {}", server, client);
            // the default pack has 23; wings_of_theve needs a phantom, which 1.12.2 does not have. The client only
            // gets the visible ones (done, or next to a done one)
            expect("22 gun pack advancements on the server", server == 22);
            expect("the client received the visible ones", client > 0);
            expectDone(mc, "root", "craft_first_gun", "arms_exhibition", "bigger_ammo_reserve", "ammo_reserve",
                    "display_case_place_gun");
            expectNotDone(mc, "times_changed", "dodge_this", "sniper_showdown", "pegboard_place_gun");
        }));
        STEPS.add(new Step("give a gun to kill with", SLOT_EMPTY, false, null, mc -> onServer(mc, player ->
                player.inventory.setInventorySlotContents(SLOT_GUN_A, gun("ak47"))), null));
        STEPS.add(new Step("kill a zombie, an enderman and a far skeleton with the gun in hand", SLOT_GUN_A, false, null,
                mc -> onServer(mc, player -> {
                    GunMod.LOGGER.info("[dev] blocks: killing with {}", DevClient.describeStack(player.getHeldItemMainhand()));
                    kill(player, new EntityZombie(player.world), base.add(0, 0, 8));
                    kill(player, new EntityEnderman(player.world), base.add(2, 0, 8));
                    kill(player, new EntitySkeleton(player.world), base.add(0, 0, 105));
                }),
                mc -> {
                    expectDone(mc, "times_changed", "dodge_this", "sniper_showdown");
                    // a zombie, not a 75 m kill with a Springfield 1873
                    expectNotDone(mc, "hunt_showdown");
                }));
        STEPS.add(new Step("craft: dye a pegboard", SLOT_EMPTY, false, null, mc -> onServer(mc, player -> {
            InventoryCrafting grid = new InventoryCrafting(new Container() {
                @Override
                public boolean canInteractWith(EntityPlayer playerIn) {
                    return true;
                }
            }, 3, 3);
            grid.setInventorySlotContents(0, new ItemStack(ModItems.GUN_PEGBOARD.get()));
            grid.setInventorySlotContents(1, new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()));
            IRecipe recipe = CraftingManager.findMatchingRecipe(grid, player.world);
            GunMod.LOGGER.info("[dev] blocks: crafting {}", recipe == null ? null : recipe.getRegistryName());
            if (recipe != null) {
                // what SlotCrafting#onTake fires before it consumes the grid
                FMLCommonHandler.instance().firePlayerCraftingEvent(player, recipe.getCraftingResult(grid), grid);
            }
        }), mc -> expectDone(mc, "pegboard_place_gun")));
        STEPS.add(new Step("screenshot advancements", SLOT_EMPTY, false, null,
                mc -> mc.displayGuiScreen(new GuiScreenAdvancements(mc.player.connection.getAdvancementManager())), null));
        STEPS.add(new Step("screenshot advancements", -1, false, null, mc -> DevClient.screenshot(mc, "advancements"), null));
        STEPS.add(new Step("close the advancements", -1, false, null, mc -> mc.displayGuiScreen(null), null));
    }

    private static void kill(EntityPlayerMP player, EntityLiving entity, BlockPos pos) {
        entity.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        player.world.spawnEntity(entity);
        entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 10000F);
        GunMod.LOGGER.info("[dev] blocks: {} at {} m dead {}", entity.getName(), String.format("%.1f", entity.getDistance(player)), entity.getHealth() <= 0);
    }

    /** Server side progress (the integrated server shares the JVM). */
    @Nullable
    private static AdvancementProgress progress(Minecraft mc, String id) {
        IntegratedServer server = mc.getIntegratedServer();
        Advancement advancement = server.getAdvancementManager().getAdvancement(new ResourceLocation(GunMod.MOD_ID, id));
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
        return advancement == null || player == null ? null : player.getAdvancements().getProgress(advancement);
    }

    private static void expectDone(Minecraft mc, String... ids) {
        for (String id : ids) {
            AdvancementProgress progress = progress(mc, id);
            expect("advancement " + id + " done", progress != null && progress.isDone());
        }
    }

    private static void expectNotDone(Minecraft mc, String... ids) {
        for (String id : ids) {
            AdvancementProgress progress = progress(mc, id);
            expect("advancement " + id + " loaded, not done yet", progress != null && !progress.isDone());
        }
    }

    /** Revokes the tacz advancements, which the reused test world may have granted in an earlier run. */
    private static void revokeAdvancements(EntityPlayerMP player) {
        for (Advancement advancement : player.server.getAdvancementManager().getAdvancements()) {
            if (GunMod.MOD_ID.equals(advancement.getId().getNamespace())) {
                AdvancementProgress progress = player.getAdvancements().getProgress(advancement);
                for (String criterion : progress.getCompletedCriteria()) {
                    player.getAdvancements().revokeCriterion(advancement, criterion);
                }
            }
        }
    }

    private static int droppedGuns(Minecraft mc, BlockPos pos) {
        World world = mc.getIntegratedServer().getWorld(0);
        int count = 0;
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos).grow(3))) {
            if (item.getItem().getItem() instanceof com.tacz.guns.api.item.IGun) {
                count++;
            }
        }
        return count;
    }

    private static void setup(Minecraft mc, IntegratedServer server) {
        onServer(mc, player -> {
            player.setGameType(GameType.SURVIVAL);
            // the flat world spawns slimes: nothing may kill the player
            player.capabilities.disableDamage = true;
            player.sendPlayerAbilities();
            player.inventory.clear();
            revokeAdvancements(player);
            World world = player.world;
            // a clean row in front, whatever earlier runs left in the save
            for (BlockPos pos : BlockPos.getAllInBox(base.add(-3, 0, 1), base.add(3, 3, 5))) {
                world.setBlockToAir(pos);
            }
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (!(entity instanceof EntityPlayer)) {
                    entity.setDead();
                }
            }
            player.connection.setPlayerLocation(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0F, 0F);
            player.inventory.setInventorySlotContents(SLOT_CASES, new ItemStack(ModItems.GUN_DISPLAY_CASE.get(), 2));
            player.inventory.setInventorySlotContents(SLOT_PEGBOARD, new ItemStack(ModItems.GUN_PEGBOARD.get()));
            player.inventory.setInventorySlotContents(SLOT_GLOW, new ItemStack(ModItems.GLOW_GUN_PEGBOARD.get()));
            player.inventory.setInventorySlotContents(SLOT_STORAGE, new ItemStack(ModItems.AMMO_STORAGE.get()));
            player.inventory.setInventorySlotContents(SLOT_GUN_A, gun("ak47"));
            player.inventory.setInventorySlotContents(SLOT_GUN_B, gun("hk_mp7a1"));
            player.inventory.setInventorySlotContents(SLOT_GUN_C, gun("ak12"));
            ItemStack box = new ItemStack(ModItems.AMMO_BOX.get());
            IAmmoBox ammoBox = (IAmmoBox) box.getItem();
            ammoBox.setAmmoId(box, AMMO_762);
            ammoBox.setAmmoCount(box, 1);
            player.inventory.setInventorySlotContents(SLOT_AMMO_BOX, box);
            player.inventory.setInventorySlotContents(9, AmmoItemBuilder.create().setId(AMMO_762).setCount(60).build());
            player.inventory.setInventorySlotContents(10, AmmoItemBuilder.create().setId(AMMO_762).setCount(40).build());
            // the selected slot stays as the client knows it: a server side change would not reach the client, which
            // then sends no slot change before its next use and the server would use the wrong slot
            GunMod.LOGGER.info("[dev] blocks: base {}, inventory {}", base, DevClient.describeInventory(player));
        });
    }

    private static ItemStack gun(String id) {
        return GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, id)).setAmmoCount(1).build();
    }

    // --- input -----------------------------------------------------------------------------------------------

    private static void aim(Minecraft mc, Vec3d point) {
        double dx = point.x - mc.player.posX;
        double dy = point.y - (mc.player.posY + mc.player.getEyeHeight());
        double dz = point.z - mc.player.posZ;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        mc.player.rotationYaw = mc.player.prevRotationYaw = yaw;
        mc.player.rotationPitch = mc.player.prevRotationPitch = pitch;
        mc.player.rotationYawHead = mc.player.prevRotationYawHead = yaw;
    }

    /** The use key (right mouse button). */
    private static void use(Minecraft mc) {
        MinecraftAccess.startUseItem(mc);
    }

    /** The attack key (left mouse button). */
    private static void attack(Minecraft mc) {
        try {
            // a screen closed before sets a click delay, as a real click would have to wait it out
            LEFT_CLICK_COUNTER.setInt(mc, 0);
            CLICK_MOUSE.invoke(mc);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The TACZ interact key (with a gun in the main hand the use key aims instead). */
    private static void interact(Minecraft mc) {
        KeyBinding.setKeyBindState(InteractKey.INTERACT_KEY.getKeyCode(), true);
        try {
            GunMod.LOGGER.info("[dev] blocks: interact key handled: {}", InteractKey.onInteractPress());
        } finally {
            KeyBinding.setKeyBindState(InteractKey.INTERACT_KEY.getKeyCode(), false);
        }
    }

    private static void windowClick(Minecraft mc, int slot, int button, ClickType type) {
        mc.playerController.windowClick(mc.player.openContainer.windowId, slot, button, type, mc.player);
    }

    // --- checks ----------------------------------------------------------------------------------------------

    private static Vec3d top(BlockPos ground) {
        return new Vec3d(ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5);
    }

    /**
     * A point on the display face of a north facing display case or pegboard (both 13/16 deep): {@code u} from west
     * to east, {@code v} from bottom to top; a 4x4 grid, slot = row * 4 + column.
     */
    private static Vec3d face(BlockPos pos, double u, double v) {
        return new Vec3d(pos.getX() + u, pos.getY() + v, pos.getZ() + 13.0 / 16.0);
    }

    private static Vec3d center(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void expect(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        GunMod.LOGGER.info("[dev] blocks: check {}: {}", ok ? "OK" : "FAILED", what);
    }

    private static void expectBlock(Minecraft mc, BlockPos pos, String id) {
        expect(id + " at " + pos, new ResourceLocation(GunMod.MOD_ID, id).equals(mc.world.getBlockState(pos).getBlock().getRegistryName()));
    }

    private static boolean locked(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return !state.getPropertyKeys().contains(com.tacz.guns.block.GunDisplayCaseBlock.LOCKED) || state.getValue(com.tacz.guns.block.GunDisplayCaseBlock.LOCKED);
    }

    private static GunDisplayBlockEntity display(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof GunDisplayBlockEntity display ? display : null;
    }

    private static int slotCount(GunDisplayBlockEntity display) {
        return display instanceof GunPegboardBlockEntity ? GunPegboardBlockEntity.SLOT_COUNT : GunDisplayCaseBlockEntity.SLOT_COUNT;
    }

    /** Client side, as the player sees it. */
    private static boolean hasItem(Minecraft mc, BlockPos pos) {
        GunDisplayBlockEntity display = display(mc.world, pos);
        if (display == null) {
            return false;
        }
        for (int i = 0; i < slotCount(display); i++) {
            if (!display.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int rotation(World world, BlockPos pos, int slot) {
        GunDisplayBlockEntity display = display(world, pos);
        return display == null ? 0 : display.getRotation(slot);
    }

    private static String describeDisplay(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);
        StringBuilder out = new StringBuilder(String.valueOf(state.getBlock().getRegistryName()))
                .append(' ').append(state.getActualState(world, pos)).append(' ')
                .append(te == null ? "no block entity" : te.getClass().getSimpleName());
        if (te instanceof GunDisplayBlockEntity display) {
            for (int i = 0; i < slotCount(display); i++) {
                ItemStack item = display.getItem(i);
                if (!item.isEmpty()) {
                    out.append(" [").append(i).append(": ").append(DevClient.describeStack(item)).append(" rot ").append(display.getRotation(i)).append(']');
                }
            }
        }
        return out.toString();
    }

    private static void logBlocks(Minecraft mc, String when, BlockPos... positions) {
        for (BlockPos pos : positions) {
            GunMod.LOGGER.info("[dev] blocks: {}: client {} {}", when, pos, describeDisplay(mc.world, pos));
        }
        onServer(mc, player -> {
            for (BlockPos pos : positions) {
                GunMod.LOGGER.info("[dev] blocks: {}: server {} {}", when, pos, describeDisplay(player.world, pos));
            }
        });
    }

    private static void logDrops(Minecraft mc, BlockPos pos) {
        onServer(mc, player -> {
            List<EntityItem> items = player.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos).grow(3));
            for (EntityItem item : items) {
                GunMod.LOGGER.info("[dev] blocks: dropped {} at {}", DevClient.describeStack(item.getItem()), item.getPositionVector());
            }
            GunMod.LOGGER.info("[dev] blocks: {} dropped item(s), inventory {}", items.size(), DevClient.describeInventory(player));
        });
    }

    private static void logStorage(Minecraft mc, BlockPos pos) {
        onServer(mc, player -> {
            if (player.world.getTileEntity(pos) instanceof AmmoStorageBlockEntity storage) {
                List<String> contents = new ArrayList<>();
                for (AmmoStorageInventory.Content content : storage.getContents()) {
                    contents.add(content.count() + "x " + DevClient.describeStack(content.sample()));
                }
                GunMod.LOGGER.info("[dev] blocks: server storage {} used stacks {}, inventory {}", contents, storage.getUsedStacks(), DevClient.describeInventory(player));
            }
        });
        if (mc.player.openContainer instanceof AmmoStorageMenu menu) {
            GunMod.LOGGER.info("[dev] blocks: client menu entries {} used stacks {}/{}", menu.getEntries(), menu.getUsedStacks(), menu.getCapacityStacks());
        }
    }

    /** Server side contents of the storage (7.62x39 only); the integrated server shares the JVM. */
    private static long storedCount(Minecraft mc, BlockPos pos) {
        World world = mc.getIntegratedServer().getWorld(0);
        long count = 0;
        if (world.getTileEntity(pos) instanceof AmmoStorageBlockEntity storage) {
            for (AmmoStorageInventory.Content content : storage.getContents()) {
                count += content.count();
            }
        }
        return count;
    }

    private static int boxCount(Minecraft mc) {
        ItemStack box = mc.player.inventory.getStackInSlot(SLOT_AMMO_BOX);
        return box.getItem() instanceof IAmmoBox ammoBox ? ammoBox.getAmmoCount(box) : -1;
    }

    /** The client copies of the display block entities must match the server ones (items and rotations). */
    private static void compareSides(Minecraft mc, BlockPos... positions) {
        World serverWorld = mc.getIntegratedServer().getWorld(0);
        for (BlockPos pos : positions) {
            String client = describeDisplay(mc.world, pos);
            String server = describeDisplay(serverWorld, pos);
            GunMod.LOGGER.info("[dev] blocks: final {} {}", pos, server);
            expect("client and server agree at " + pos, client.equals(server));
        }
    }

    private static void onServer(Minecraft mc, Consumer<EntityPlayerMP> task) {
        IntegratedServer server = mc.getIntegratedServer();
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            if (player != null) {
                task.accept(player);
            }
        });
    }

    private static final class Step {
        final String name;
        final int slot;
        final boolean sneak;
        final Vec3d aim;
        final Consumer<Minecraft> action;
        final Consumer<Minecraft> check;

        Step(String name, int slot, boolean sneak, Vec3d aim, Consumer<Minecraft> action, Consumer<Minecraft> check) {
            this.name = name;
            this.slot = slot;
            this.sneak = sneak;
            this.aim = aim;
            this.action = action;
            this.check = check;
        }
    }
}
