package com.tacz.guns.legacy.client;

import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.render.PerspectiveCaptureModel;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 model glue that 1.20.1 does implicitly: every TACZ block state uses the single variant of its blockstate
 * file, every TACZ item uses {@code models/item/<name>.json} (the
 * upstream {@code builtin/entity} models), and the models of items drawn by a {@code BlockEntityWithoutLevelRenderer}
 * are wrapped so the renderer learns the display context ({@link PerspectiveCaptureModel}).
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public final class LegacyItemModels {
    private LegacyItemModels() {
    }

    private static ModelResourceLocation location(Item item) {
        return new ModelResourceLocation(item.getRegistryName(), "inventory");
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        // TACZ blocks are drawn by their block entity renderers; every state uses the "normal" variant
        // (src/main/resources has 1.12 copies of upstream's single-variant blockstate files)
        for (Block block : ForgeRegistries.BLOCKS) {
            if (GunMod.MOD_ID.equals(block.getRegistryName().getNamespace())) {
                ModelResourceLocation single = new ModelResourceLocation(block.getRegistryName(), "normal");
                ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                    @Override
                    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                        return single;
                    }
                });
            }
        }
        for (Item item : ForgeRegistries.ITEMS) {
            if (GunMod.MOD_ID.equals(item.getRegistryName().getNamespace())) {
                ModelLoader.setCustomModelResourceLocation(item, 0, location(item));
            }
        }
    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (GunMod.MOD_ID.equals(item.getRegistryName().getNamespace())
                    && item.getTileEntityItemStackRenderer() instanceof com.tacz.guns.legacy.render.BlockEntityWithoutLevelRenderer) {
                PerspectiveCaptureModel.wrap(event, location(item));
            }
        }
    }
}
