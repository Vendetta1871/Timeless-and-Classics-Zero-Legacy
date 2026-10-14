package com.tacz.guns.legacy.util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads 1.20.1 recipe ingredients (as gun packs write them) on 1.12.2:
 * <ul>
 *     <li>{@code {"tag": "forge:ingots/iron"}} becomes an ore dictionary ingredient ({@code ingotIron}); tags whose ore
 *     name is empty fall back to the vanilla items listed in {@link #TAG_ITEMS};</li>
 *     <li>{@code {"item": "minecraft:oak_planks"}} is mapped through {@link #ITEMS} (the 1.13 flattening, reversed);</li>
 *     <li>anything else goes to Forge's {@link CraftingHelper#getIngredient}, so 1.12.2 style JSON also works.</li>
 * </ul>
 * Items or tags that have no 1.12.2 counterpart (e.g. copper, amethyst, netherite) give an empty ingredient, which
 * makes the recipe uncraftable unless another mod adds those items to the ore dictionary.
 */
public final class LegacyIngredients {
    /**
     * forge tag folder → ore dictionary prefix
     */
    private static final Map<String, String> TAG_PREFIXES = ImmutableMap.<String, String>builder()
            .put("ingots", "ingot").put("nuggets", "nugget").put("gems", "gem").put("dusts", "dust")
            .put("storage_blocks", "block").put("ores", "ore").put("rods", "rod").put("plates", "plate")
            .put("dyes", "dye").put("glass", "blockGlass").put("glass_panes", "paneGlass").put("chests", "chest")
            .build();

    /**
     * tags without a folder or whose 1.12 ore name is different
     */
    private static final Map<String, String> TAG_ORES = ImmutableMap.<String, String>builder()
            .put("forge:gunpowder", "gunpowder").put("forge:leather", "leather").put("forge:string", "string")
            .put("forge:feathers", "feather").put("forge:bones", "bone").put("forge:slimeballs", "slimeball")
            .put("forge:ender_pearls", "enderpearl").put("forge:obsidian", "obsidian").put("forge:stone", "stone")
            .put("forge:cobblestone", "cobblestone").put("forge:sand", "sand").put("forge:gravel", "gravel")
            .put("forge:glass", "blockGlass").put("forge:glass_panes", "paneGlass").put("forge:rods/wooden", "stickWood")
            .put("forge:chests/wooden", "chestWood").put("forge:netherrack", "netherrack").put("forge:end_stones", "endstone")
            .put("forge:eggs", "egg").put("forge:crops/wheat", "cropWheat").put("forge:paper", "paper")
            .put("minecraft:logs", "logWood").put("minecraft:planks", "plankWood").put("minecraft:wool", "wool")
            .put("minecraft:wooden_slabs", "slabWood").put("minecraft:wooden_stairs", "stairWood")
            .put("minecraft:saplings", "treeSapling").put("minecraft:leaves", "treeLeaves").put("minecraft:coals", "coal")
            .build();

    /**
     * Vanilla items for tags that the 1.12 ore dictionary does not cover (name, meta).
     */
    private static final Map<String, String[]> TAG_ITEMS = ImmutableMap.<String, String[]>builder()
            .put("forge:rods/blaze", new String[]{"minecraft:blaze_rod"})
            .put("forge:nether_stars", new String[]{"minecraft:nether_star"})
            .put("forge:heads", new String[]{"minecraft:skull@*"})
            .build();

    /**
     * 1.20.1 item id → 1.12.2 "id@meta" (only items whose id changed).
     */
    private static final Map<String, String> ITEMS = ImmutableMap.<String, String>builder()
            .put("minecraft:oak_planks", "minecraft:planks@0").put("minecraft:spruce_planks", "minecraft:planks@1")
            .put("minecraft:birch_planks", "minecraft:planks@2").put("minecraft:jungle_planks", "minecraft:planks@3")
            .put("minecraft:acacia_planks", "minecraft:planks@4").put("minecraft:dark_oak_planks", "minecraft:planks@5")
            .put("minecraft:oak_log", "minecraft:log@0").put("minecraft:spruce_log", "minecraft:log@1")
            .put("minecraft:birch_log", "minecraft:log@2").put("minecraft:jungle_log", "minecraft:log@3")
            .put("minecraft:acacia_log", "minecraft:log2@0").put("minecraft:dark_oak_log", "minecraft:log2@1")
            .put("minecraft:lapis_lazuli", "minecraft:dye@4").put("minecraft:bone_meal", "minecraft:dye@15")
            .put("minecraft:ink_sac", "minecraft:dye@0").put("minecraft:cocoa_beans", "minecraft:dye@3")
            .put("minecraft:red_dye", "minecraft:dye@1").put("minecraft:green_dye", "minecraft:dye@2")
            .put("minecraft:black_dye", "minecraft:dye@0").put("minecraft:white_dye", "minecraft:dye@15")
            .put("minecraft:white_wool", "minecraft:wool@0").put("minecraft:black_wool", "minecraft:wool@15")
            .put("minecraft:oak_slab", "minecraft:wooden_slab@0").put("minecraft:stone_slab", "minecraft:stone_slab@0")
            .put("minecraft:smooth_stone_slab", "minecraft:stone_slab@0").put("minecraft:smooth_stone", "minecraft:stone@0")
            .put("minecraft:oak_fence", "minecraft:fence@0").put("minecraft:oak_door", "minecraft:wooden_door@0")
            .put("minecraft:oak_trapdoor", "minecraft:trapdoor@0").put("minecraft:oak_pressure_plate", "minecraft:wooden_pressure_plate@0")
            .put("minecraft:oak_button", "minecraft:wooden_button@0").put("minecraft:crafting_table", "minecraft:crafting_table@0")
            .put("minecraft:chiseled_quartz_block", "minecraft:quartz_block@1").put("minecraft:terracotta", "minecraft:hardened_clay@0")
            .put("minecraft:iron_bars", "minecraft:iron_bars@0").put("minecraft:scute", "minecraft:prismarine_shard@0")
            .put("minecraft:melon_slice", "minecraft:melon@0").put("minecraft:glistering_melon_slice", "minecraft:speckled_melon@0")
            .put("minecraft:popped_chorus_fruit", "minecraft:chorus_fruit_popped@0").put("minecraft:fire_charge", "minecraft:fire_charge@0")
            .put("minecraft:end_crystal", "minecraft:end_crystal@0").put("minecraft:gunpowder", "minecraft:gunpowder@0")
            .build();

    private LegacyIngredients() {
    }

    public static Ingredient fromJson(JsonElement json) {
        if (json.isJsonArray()) {
            List<ItemStack> stacks = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                for (ItemStack stack : fromJson(element).getMatchingStacks()) {
                    stacks.add(stack);
                }
            }
            return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
        }
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (object.has("tag")) {
                return fromTag(GsonHelper.getAsString(object, "tag"));
            }
            if (object.has("item") && !object.has("data") && !object.has("nbt") && !object.has("type")) {
                ItemStack stack = flattenItem(GsonHelper.getAsString(object, "item"));
                if (stack != null) {
                    return Ingredient.fromStacks(stack);
                }
                return Ingredient.EMPTY;
            }
        }
        return CraftingHelper.getIngredient(json, new JsonContext(GunMod.MOD_ID));
    }

    /**
     * The 1.12.2 stack for a 1.20.1 item id (wildcard meta where several 1.12 variants share the id), or
     * {@code null} if the item does not exist.
     */
    @Nullable
    public static ItemStack flattenItem(String id) {
        String mapped = ITEMS.getOrDefault(new ResourceLocation(id).toString(), id);
        return parseStack(mapped);
    }

    @Nullable
    private static ItemStack parseStack(String spec) {
        String[] parts = spec.split("@", 2);
        Item item = Item.REGISTRY.getObject(new ResourceLocation(parts[0]));
        if (item == null) {
            return null;
        }
        int meta = parts.length < 2 ? 0 : "*".equals(parts[1]) ? OreDictionary.WILDCARD_VALUE : Integer.parseInt(parts[1]);
        return new ItemStack(item, 1, meta);
    }

    public static Ingredient fromTag(String tag) {
        ResourceLocation id = new ResourceLocation(tag);
        String key = id.toString();
        String ore = TAG_ORES.get(key);
        if (ore == null) {
            ore = oreName(id);
        }
        if (ore != null && !OreDictionary.getOres(ore, false).isEmpty()) {
            return new OreIngredient(ore);
        }
        String[] items = TAG_ITEMS.get(key);
        if (items != null) {
            List<ItemStack> stacks = new ArrayList<>();
            for (String spec : items) {
                ItemStack stack = parseStack(spec);
                if (stack != null) {
                    stacks.add(stack);
                }
            }
            return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
        }
        if (ore == null) {
            throw new JsonSyntaxException("Unknown tag '" + tag + "'");
        }
        // an ore name nobody registers (yet): keep it, mods that add it to the ore dictionary make it craftable
        return new OreIngredient(ore);
    }

    /**
     * {@code forge:ingots/iron} → {@code ingotIron}, {@code forge:storage_blocks/raw_iron} → {@code blockRawIron}.
     */
    @Nullable
    private static String oreName(ResourceLocation tag) {
        String[] path = tag.getPath().split("/", 2);
        String prefix = TAG_PREFIXES.get(path[0]);
        if (prefix == null) {
            return null;
        }
        if (path.length < 2) {
            return prefix;
        }
        StringBuilder name = new StringBuilder(prefix);
        for (String word : path[1].split("_")) {
            if (!word.isEmpty()) {
                name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
            }
        }
        return name.toString();
    }
}
