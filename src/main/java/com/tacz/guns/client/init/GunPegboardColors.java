package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.GunPegboardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2: upstream's mod-bus listeners ({@code RegisterColorHandlersEvent.Block/Item}, {@code FMLClientSetupEvent})
 * are {@link #register()}, called from {@code ClientSetupEvent#onClientSetupInit} once the item and block colors exist.
 */
@SideOnly(Side.CLIENT)
public final class GunPegboardColors {
    private GunPegboardColors() {
    }

    public static void register() {
        blockColors();
        itemColors();
        setup();
    }

    public static void blockColors() {
        // the tint is read on the chunk render threads: the block entity must not be created there
        Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((state, level, pos, tint) -> level != null && pos != null
                && GunPegboardBlock.getBlockEntity(level, pos) instanceof GunPegboardBlockEntity pegboard && pegboard.getColor() >= 0
                ? pegboard.getColor() : -1, ModBlocks.GUN_PEGBOARD.get(), ModBlocks.GLOW_GUN_PEGBOARD.get());
    }

    public static void itemColors() {
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tint) -> ((GunPegboardItem) stack.getItem()).hasCustomColor(stack)
                ? ((GunPegboardItem) stack.getItem()).getColor(stack) : -1, ModItems.GUN_PEGBOARD.get(), ModItems.GLOW_GUN_PEGBOARD.get());
    }

    /**
     * {@code ItemProperties.register(..., "tacz:dyed", ...)}: the item models switch to the dyed (grayscale) texture.
     */
    public static void setup() {
        for (Item item : new Item[]{ModItems.GUN_PEGBOARD.get(), ModItems.GLOW_GUN_PEGBOARD.get()}) {
            item.addPropertyOverride(new ResourceLocation(GunMod.MOD_ID, "dyed"),
                    (stack, level, entity) -> ((GunPegboardItem) stack.getItem()).hasCustomColor(stack) ? 1 : 0);
        }
    }
}
