package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.*;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.legacy.registry.DeferredRegister;
import com.tacz.guns.legacy.registry.RegistryObject;
import com.tacz.guns.legacy.tags.BlockTags;
import com.tacz.guns.legacy.tags.TagKey;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GunMod.MOD_ID);

    // 旧方块就让他独占一个了
    public static RegistryObject<Block> GUN_SMITH_TABLE = BLOCKS.register("gun_smith_table", GunSmithTableBlockB::new);
    public static RegistryObject<Block> WORKBENCH_111 = BLOCKS.register("workbench_a", GunSmithTableBlockA::new);
    public static RegistryObject<Block> WORKBENCH_211 = BLOCKS.register("workbench_b", GunSmithTableBlockB::new);
    public static RegistryObject<Block> WORKBENCH_121 = BLOCKS.register("workbench_c", GunSmithTableBlockC::new);

    public static RegistryObject<Block> TARGET = BLOCKS.register("target", TargetBlock::new);
    public static RegistryObject<Block> STATUE = BLOCKS.register("statue", StatueBlock::new);

    public static final TagKey<Block> BULLET_IGNORE_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "bullet_ignore"));

    /**
     * 1.12.2: block entities have no registry (upstream's {@code TILE_ENTITIES} DeferredRegister and
     * {@code BlockEntityType}s); they are registered by class with {@link GameRegistry}.
     */
    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(GunSmithTableBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "gun_smith_table"));
        GameRegistry.registerTileEntity(TargetBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "target"));
        GameRegistry.registerTileEntity(StatueBlockEntity.class, new ResourceLocation(GunMod.MOD_ID, "statue"));
    }
}
