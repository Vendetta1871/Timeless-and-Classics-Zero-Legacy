package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.*;
import com.tacz.guns.block.entity.*;
import com.tacz.guns.legacy.registry.DeferredRegister;
import com.tacz.guns.legacy.registry.RegistryObject;
import com.tacz.guns.legacy.tags.BlockTags;
import com.tacz.guns.legacy.tags.TagKey;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GunMod.MOD_ID);

    // 旧方块就让他独占一个了
    public static RegistryObject<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table", named("gun_smith_table", GunSmithTableBlockB::new));
    public static RegistryObject<Block> WORKBENCH_111 = BLOCKS.register("workbench_a", named("workbench_a", GunSmithTableBlockA::new));
    public static RegistryObject<Block> WORKBENCH_211 = BLOCKS.register("workbench_b", named("workbench_b", GunSmithTableBlockB::new));
    public static RegistryObject<Block> WORKBENCH_121 = BLOCKS.register("workbench_c", named("workbench_c", GunSmithTableBlockC::new));

    public static RegistryObject<Block> TARGET = BLOCKS.register("target", named("target", TargetBlock::new));
    public static RegistryObject<Block> STATUE = BLOCKS.register("statue", named("statue", StatueBlock::new));
    public static RegistryObject<Block> GUN_PEGBOARD = BLOCKS.register("gun_pegboard", named("gun_pegboard", GunPegboardBlock::new));
    public static RegistryObject<Block> GLOW_GUN_PEGBOARD = BLOCKS.register("glow_gun_pegboard", named("glow_gun_pegboard", () -> new GunPegboardBlock(true)));
    public static RegistryObject<Block> AMMO_STORAGE = BLOCKS.register("ammo_storage", named("ammo_storage", () -> new AmmoStorageBlock(AmmoStorageBlock.IRON_CAPACITY_STACKS)));
    public static RegistryObject<Block> AMMO_STORAGE_GOLD = BLOCKS.register("ammo_storage_gold", named("ammo_storage_gold", () -> new AmmoStorageBlock(AmmoStorageBlock.GOLD_CAPACITY_STACKS)));
    public static RegistryObject<Block> AMMO_STORAGE_DIAMOND = BLOCKS.register("ammo_storage_diamond", named("ammo_storage_diamond", () -> new AmmoStorageBlock(AmmoStorageBlock.DIAMOND_CAPACITY_STACKS)));
    public static RegistryObject<Block> GUN_DISPLAY_CASE = BLOCKS.register("gun_display_case", named("gun_display_case", GunDisplayCaseBlock::new));

    /**
     * 1.12.2: blocks get their translation key ({@code tile.tacz.<name>.name}) when they are created.
     */
    private static Supplier<Block> named(String name, Supplier<? extends Block> supplier) {
        return () -> supplier.get().setTranslationKey(GunMod.MOD_ID + "." + name);
    }

    public static final TagKey<Block> BULLET_IGNORE_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "bullet_ignore"));

    /**
     * 1.12.2: block entities have no registry (upstream's {@code TILE_ENTITIES} DeferredRegister and
     * {@code BlockEntityType}s); they are registered by class with {@link GameRegistry}.
     */
    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(GunSmithTableBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "gun_smith_table"));
        GameRegistry.registerTileEntity(TargetBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "target"));
        GameRegistry.registerTileEntity(StatueBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "statue"));
        GameRegistry.registerTileEntity(GunPegboardBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "gun_pegboard"));
        GameRegistry.registerTileEntity(AmmoStorageBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "ammo_storage"));
        GameRegistry.registerTileEntity(GunDisplayCaseBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "gun_display_case"));
    }
}
