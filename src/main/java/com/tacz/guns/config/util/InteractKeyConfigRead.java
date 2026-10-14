package com.tacz.guns.config.util;

import com.google.common.collect.Lists;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.tags.BlockTags;
import com.tacz.guns.legacy.tags.TagKey;
import com.tacz.guns.legacy.tags.TagType;
import net.minecraft.entity.EntityList;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;

public class InteractKeyConfigRead {
    private static final EnumMap<Type, List<ResourceLocation>> WHITELIST = new EnumMap<>(Type.class);
    private static final EnumMap<Type, List<ResourceLocation>> BLACKLIST = new EnumMap<>(Type.class);
    private static final TagKey<Block> WHITELIST_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "interact_key/whitelist"));
    private static final TagKey<Block> BLACKLIST_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "interact_key/blacklist"));
    private static final TagKey<Entity> WHITELIST_ENTITIES = TagKey.create(TagType.ENTITY_TYPE, new ResourceLocation(GunMod.MOD_ID, "interact_key/whitelist"));
    private static final TagKey<Entity> BLACKLIST_ENTITIES = TagKey.create(TagType.ENTITY_TYPE, new ResourceLocation(GunMod.MOD_ID, "interact_key/blacklist"));

    public static void init() {
        WHITELIST.clear();
        BLACKLIST.clear();
        handleConfigData(SyncConfig.INTERACT_KEY_WHITELIST_BLOCKS.get(), WHITELIST, Type.BLOCK);
        handleConfigData(SyncConfig.INTERACT_KEY_WHITELIST_ENTITIES.get(), WHITELIST, Type.ENTITY);
        handleConfigData(SyncConfig.INTERACT_KEY_BLACKLIST_BLOCKS.get(), BLACKLIST, Type.BLOCK);
        handleConfigData(SyncConfig.INTERACT_KEY_BLACKLIST_ENTITIES.get(), BLACKLIST, Type.ENTITY);
    }

    public static boolean canInteractBlock(IBlockState block) {
        ResourceLocation blockId = block.getBlock().getRegistryName();
        if (blockId == null) {
            return false;
        }
        // 先检查黑名单
        if (BLACKLIST.containsKey(Type.BLOCK) && BLACKLIST.get(Type.BLOCK).contains(blockId)) {
            return false;
        }
        if (BLACKLIST_BLOCKS.contains(block.getBlock())) {
            return false;
        }
        // 再检查白名单
        if (WHITELIST.containsKey(Type.BLOCK) && WHITELIST.get(Type.BLOCK).contains(blockId)) {
            return true;
        }
        return WHITELIST_BLOCKS.contains(block.getBlock());
    }

    public static boolean canInteractEntity(Entity entity) {
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return false;
        }
        // 先检查黑名单
        if (BLACKLIST.containsKey(Type.ENTITY) && BLACKLIST.get(Type.ENTITY).contains(entityId)) {
            return false;
        }
        if (BLACKLIST_ENTITIES.contains(entity)) {
            return false;
        }
        // 再检查白名单
        if (WHITELIST.containsKey(Type.ENTITY) && WHITELIST.get(Type.ENTITY).contains(entityId)) {
            return true;
        }
        return WHITELIST_ENTITIES.contains(entity);
    }

    private static void handleConfigData(List<String> configData, EnumMap<Type, List<ResourceLocation>> storeList, Type type) {
        configData.forEach(data -> {
            if (data.isEmpty()) {
                return;
            }
            if (StringUtils.isBlank(data)) {
                return;
            }
            ResourceLocation id = new ResourceLocation(data);
            storeList.computeIfAbsent(type, t -> Lists.newArrayList()).add(id);
        });
    }

    public enum Type {
        BLOCK, ENTITY;
    }
}
