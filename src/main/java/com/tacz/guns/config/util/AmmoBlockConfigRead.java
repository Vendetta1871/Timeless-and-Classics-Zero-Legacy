package com.tacz.guns.config.util;

import com.tacz.guns.GunMod;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.legacy.tags.BlockTags;
import com.tacz.guns.legacy.tags.TagKey;
import com.tacz.guns.legacy.util.OreDictTags;
import com.tacz.guns.legacy.util.ResourceLocations;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public final class AmmoBlockConfigRead {
    private static volatile BlockList passThroughBlocks = new BlockList(ImmutableSet.of(), ImmutableList.of(), ImmutableList.of());
    private static volatile BlockList destroyBlocks = new BlockList(ImmutableSet.of(), ImmutableList.of(), ImmutableList.of());

    private AmmoBlockConfigRead() {
    }

    public static void init() {
        passThroughBlocks = readBlockList(AmmoConfig.PASS_THROUGH_BLOCKS.get(), "PassThroughBlocks");
        destroyBlocks = readBlockList(AmmoConfig.DESTROY_BLOCKS.get(), "DestroyBlocks");
    }

    public static boolean canPassThrough(IBlockState state) {
        return passThroughBlocks.matches(state);
    }

    public static boolean canDestroy(IBlockState state) {
        return destroyBlocks.matches(state);
    }

    private static BlockList readBlockList(List<? extends String> entries, String configName) {
        Set<Block> blocks = new HashSet<>();
        Set<TagKey<Block>> tags = new LinkedHashSet<>();
        Set<String> oreNames = new LinkedHashSet<>();
        for (String entry : entries) {
            String value = entry.trim();
            boolean isTag = value.startsWith("#");
            String name = isTag ? value.substring(1) : value;
            ResourceLocation id = name.isEmpty() ? null : ResourceLocations.tryParse(name);
            if (id == null) {
                GunMod.LOGGER.warn("Ignoring invalid block ID or tag '{}' in ammo.{}", entry, configName);
                continue;
            }
            if (isTag) {
                // 配置加载时 tag 尚未绑定，缓存 TagKey 可随数据包重载自动匹配新内容。
                tags.add(BlockTags.create(id));
                // 1.12.2: forge tags (the default #forge:glass, #forge:glass_panes) are ore dictionary names
                String oreName = OreDictTags.toOreName(id);
                if (oreName != null) {
                    oreNames.add(oreName);
                }
            } else if (ForgeRegistries.BLOCKS.containsKey(id)) {
                blocks.add(ForgeRegistries.BLOCKS.getValue(id));
            } else {
                GunMod.LOGGER.warn("Ignoring unknown block ID '{}' in ammo.{}", entry, configName);
            }
        }
        return new BlockList(ImmutableSet.copyOf(blocks), ImmutableList.copyOf(tags), ImmutableList.copyOf(oreNames));
    }

    private static final class BlockList {
        private final Set<Block> blocks;
        private final List<TagKey<Block>> tags;
        /**
         * 1.12.2 only: the ore dictionary names of the forge tags.
         */
        private final List<String> oreNames;

        public BlockList(Set<Block> blocks, List<TagKey<Block>> tags, List<String> oreNames) {
            this.blocks = blocks;
            this.tags = tags;
            this.oreNames = oreNames;
        }

        public Set<Block> blocks() {
            return blocks;
        }

        public List<TagKey<Block>> tags() {
            return tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockList)) return false;
            BlockList that = (BlockList) o;
            return java.util.Objects.equals(blocks, that.blocks) && java.util.Objects.equals(tags, that.tags) && java.util.Objects.equals(oreNames, that.oreNames);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(blocks, tags, oreNames);
        }

        @Override
        public String toString() {
            return "BlockList[" + "blocks=" + blocks + ", " + "tags=" + tags + ", " + "oreNames=" + oreNames + "]";
        }

        private boolean matches(IBlockState state) {
            if (blocks.contains(state.getBlock())) {
                return true;
            }
            for (TagKey<Block> tag : tags) {
                if (tag.contains(state.getBlock())) {
                    return true;
                }
            }
            for (String oreName : oreNames) {
                if (OreDictTags.matches(state, oreName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
