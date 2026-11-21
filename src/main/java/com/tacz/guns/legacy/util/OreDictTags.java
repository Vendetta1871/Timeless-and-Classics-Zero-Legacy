package com.tacz.guns.legacy.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

/**
 * 1.20.1 {@code forge:} block tags on 1.12.2's ore dictionary. The common Forge tags (glass, panes, ores, storage
 * blocks...) have no tag files that {@code legacy.tags} could read, their 1.12.2 counterparts are ore dictionary names
 * ({@code #forge:glass} is {@code blockGlass}, {@code #forge:glass_panes/red} is {@code paneGlassRed},
 * {@code #forge:ores/iron} is {@code oreIron}, {@code #forge:storage_blocks/iron} is {@code blockIron}).
 */
public final class OreDictTags {
    private OreDictTags() {
    }

    /**
     * The ore dictionary name of a {@code forge:} tag, or {@code null} for other namespaces.
     */
    @Nullable
    public static String toOreName(ResourceLocation tag) {
        if (!"forge".equals(tag.getNamespace())) {
            return null;
        }
        String path = tag.getPath();
        int slash = path.indexOf('/');
        String group = slash < 0 ? path : path.substring(0, slash);
        String variant = slash < 0 ? "" : upperCamel(path.substring(slash + 1));
        switch (group) {
            case "glass":
                return "blockGlass" + variant;
            case "glass_panes":
                return "paneGlass" + variant;
            case "ores":
                return "ore" + variant;
            case "storage_blocks":
                return "block" + variant;
            case "end_stones":
                return "endstone";
            case "chests":
                return "chest" + variant;
            case "fence_gates":
                return "fenceGate" + variant;
            case "fences":
                return "fence" + variant;
            default:
                // stone, cobblestone, sand, sandstone, gravel, obsidian, netherrack...
                return lowerCamel(path);
        }
    }

    /**
     * Whether the item form of the block state is registered under the ore dictionary name.
     */
    public static boolean matches(IBlockState state, String oreName) {
        if (!OreDictionary.doesOreNameExist(oreName)) {
            return false;
        }
        Block block = state.getBlock();
        Item item = Item.getItemFromBlock(block);
        if (item == net.minecraft.init.Items.AIR) {
            return false;
        }
        ItemStack stack = new ItemStack(item, 1, block.damageDropped(state));
        if (stack.isEmpty()) {
            return false;
        }
        int id = OreDictionary.getOreID(oreName);
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            if (oreId == id) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code light_blue} -> {@code LightBlue}.
     */
    private static String upperCamel(String path) {
        StringBuilder builder = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/') {
                upper = true;
            } else {
                builder.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return builder.toString();
    }

    /**
     * {@code end_stone} -> {@code endStone}.
     */
    private static String lowerCamel(String path) {
        String result = upperCamel(path);
        return result.isEmpty() ? result : Character.toLowerCase(result.charAt(0)) + result.substring(1);
    }
}
