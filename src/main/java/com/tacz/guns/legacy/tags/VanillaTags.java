package com.tacz.guns.legacy.tags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The 1.12.2 contents of the vanilla 1.20.1 tags that TACZ's tag files refer to, and the 1.20.1 block ids that
 * became several 1.12.2 blocks.
 */
final class VanillaTags {
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private static final Map<String, List<String>> BLOCK_TAGS = ImmutableMap.<String, List<String>>builder()
            .put("anvil", ImmutableList.of("anvil"))
            .put("beds", ImmutableList.of("bed"))
            .put("buttons", ImmutableList.of("stone_button", "wooden_button"))
            .put("wooden_buttons", ImmutableList.of("wooden_button"))
            .put("wooden_doors", ImmutableList.of("wooden_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door"))
            .put("doors", ImmutableList.of("wooden_door", "spruce_door", "birch_door", "jungle_door", "acacia_door", "dark_oak_door", "iron_door"))
            .put("fence_gates", ImmutableList.of("fence_gate", "spruce_fence_gate", "birch_fence_gate", "jungle_fence_gate", "dark_oak_fence_gate", "acacia_fence_gate"))
            .put("fences", ImmutableList.of("fence", "spruce_fence", "birch_fence", "jungle_fence", "dark_oak_fence", "acacia_fence", "nether_brick_fence"))
            .put("wooden_fences", ImmutableList.of("fence", "spruce_fence", "birch_fence", "jungle_fence", "dark_oak_fence", "acacia_fence"))
            .put("leaves", ImmutableList.of("leaves", "leaves2"))
            .put("signs", ImmutableList.of("standing_sign", "wall_sign"))
            .put("standing_signs", ImmutableList.of("standing_sign"))
            .put("wall_signs", ImmutableList.of("wall_sign"))
            .put("all_hanging_signs", ImmutableList.of())
            .put("wooden_trapdoors", ImmutableList.of("trapdoor"))
            .put("trapdoors", ImmutableList.of("trapdoor", "iron_trapdoor"))
            .put("rails", ImmutableList.of("rail", "golden_rail", "detector_rail", "activator_rail"))
            .put("shulker_boxes", shulkerBoxes())
            .build();

    /**
     * 1.20.1 block ids that are several blocks in 1.12.2 (lit variants etc.).
     */
    private static final Map<String, List<String>> BLOCK_IDS = ImmutableMap.<String, List<String>>builder()
            .put("furnace", ImmutableList.of("furnace", "lit_furnace"))
            .put("redstone_lamp", ImmutableList.of("redstone_lamp", "lit_redstone_lamp"))
            .put("repeater", ImmutableList.of("unpowered_repeater", "powered_repeater"))
            .put("comparator", ImmutableList.of("unpowered_comparator", "powered_comparator"))
            .put("oak_door", ImmutableList.of("wooden_door"))
            .put("oak_fence", ImmutableList.of("fence"))
            .put("oak_fence_gate", ImmutableList.of("fence_gate"))
            .put("oak_trapdoor", ImmutableList.of("trapdoor"))
            .build();

    private VanillaTags() {
    }

    private static List<String> shulkerBoxes() {
        List<String> boxes = new ArrayList<>();
        for (String color : COLORS) {
            boxes.add(color + "_shulker_box");
        }
        return boxes;
    }

    static List<ResourceLocation> get(TagType<?> type, ResourceLocation tag) {
        if (type != TagType.BLOCK || !"minecraft".equals(tag.getNamespace())) {
            return Collections.emptyList();
        }
        return toIds(BLOCK_TAGS.getOrDefault(tag.getPath(), Collections.emptyList()));
    }

    static List<ResourceLocation> flatten(TagType<?> type, ResourceLocation id) {
        if (type == TagType.BLOCK && "minecraft".equals(id.getNamespace()) && BLOCK_IDS.containsKey(id.getPath())) {
            return toIds(BLOCK_IDS.get(id.getPath()));
        }
        return Collections.singletonList(id);
    }

    private static List<ResourceLocation> toIds(List<String> paths) {
        List<ResourceLocation> ids = new ArrayList<>(paths.size());
        for (String path : paths) {
            ids.add(new ResourceLocation("minecraft", path));
        }
        return ids;
    }
}
