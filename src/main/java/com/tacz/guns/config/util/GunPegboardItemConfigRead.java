package com.tacz.guns.config.util;

import com.google.common.collect.Lists;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class GunPegboardItemConfigRead {
    private static final List<ResourceLocation> WHITELIST = Lists.newArrayList();
    private static final List<ResourceLocation> BLACKLIST = Lists.newArrayList();
    // TODO(port): needs an item TagType in legacy.tags (only blocks and entity types exist); upstream also reads the
    //  item tags tacz:gun_pegboard/whitelist and tacz:gun_pegboard/blacklist (empty in the mod's own data)
    // private static final TagKey<Item> WHITELIST_ITEMS = ItemTags.create(new ResourceLocation(GunMod.MOD_ID, "gun_pegboard/whitelist"));
    // private static final TagKey<Item> BLACKLIST_ITEMS = ItemTags.create(new ResourceLocation(GunMod.MOD_ID, "gun_pegboard/blacklist"));

    private GunPegboardItemConfigRead() {
    }

    public static void init() {
        WHITELIST.clear();
        BLACKLIST.clear();
        handleConfigData(SyncConfig.GUN_PEGBOARD_WHITELIST_ITEMS.get(), WHITELIST);
        handleConfigData(SyncConfig.GUN_PEGBOARD_BLACKLIST_ITEMS.get(), BLACKLIST);
    }

    public static boolean canPlace(ItemStack stack) {
        ResourceLocation itemId = stack.getItem().getRegistryName();
        if (itemId == null || stack.isEmpty()) {
            return false;
        }
        if (BLACKLIST.contains(itemId)) {
            return false;
        }
        if (WHITELIST.contains(itemId)) {
            return true;
        }
        Item item = stack.getItem();
        return item instanceof IGun || item instanceof IAttachment || item instanceof IAmmo || item instanceof IAmmoBox;
    }

    private static void handleConfigData(List<String> configData, List<ResourceLocation> storeList) {
        configData.forEach(data -> {
            if (StringUtils.isBlank(data)) {
                return;
            }
            storeList.add(new ResourceLocation(data));
        });
    }
}
