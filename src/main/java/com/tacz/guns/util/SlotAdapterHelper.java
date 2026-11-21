package com.tacz.guns.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonSlotAdapterIndex;
import net.minecraft.nbt.NBTTagCompound;
import com.tacz.guns.legacy.util.ResourceLocations;
import com.tacz.guns.legacy.util.StackNbt;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;

public final class SlotAdapterHelper {
    private static final String SLOT_ADAPTERS_TAG = "SlotAdapters";
    private static final String TAG_PREFIX = "#";
    private static final String ADAPTER_TAG_PATH_PREFIX = "adapter/";

    private SlotAdapterHelper() {
    }

    public static void setSlotAdapter(ItemStack holder, AttachmentType type, ResourceLocation adapterId) {
        if (holder.isEmpty() || type == null || adapterId == null) {
            return;
        }
        NBTTagCompound root = StackNbt.getOrCreateTag(holder);
        NBTTagCompound adapters = root.getCompoundTag(SLOT_ADAPTERS_TAG);
        adapters.setString(slotKey(type), adapterId.toString());
        root.setTag(SLOT_ADAPTERS_TAG, adapters);
    }

    @Nullable
    public static ResourceLocation getSlotAdapter(ItemStack holder, AttachmentType type) {
        if (holder.isEmpty() || type == null) {
            return null;
        }
        NBTTagCompound root = holder.getTagCompound();
        if (root == null || !root.hasKey(SLOT_ADAPTERS_TAG, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        NBTTagCompound adapters = root.getCompoundTag(SLOT_ADAPTERS_TAG);
        String key = slotKey(type);
        if (!adapters.hasKey(key, Constants.NBT.TAG_STRING)) {
            return null;
        }
        return ResourceLocations.tryParse(adapters.getString(key));
    }

    public static void removeSlotAdapter(ItemStack holder, AttachmentType type) {
        if (holder.isEmpty() || type == null) {
            return;
        }
        NBTTagCompound root = holder.getTagCompound();
        if (root == null || !root.hasKey(SLOT_ADAPTERS_TAG, Constants.NBT.TAG_COMPOUND)) {
            return;
        }
        NBTTagCompound adapters = root.getCompoundTag(SLOT_ADAPTERS_TAG);
        adapters.removeTag(slotKey(type));
        if (adapters.isEmpty()) {
            root.removeTag(SLOT_ADAPTERS_TAG);
        } else {
            root.setTag(SLOT_ADAPTERS_TAG, adapters);
        }
    }

    public static boolean allowsDirectAttachment(ItemStack gun, ResourceLocation attachmentId) {
        ResourceLocation hostGunId = getHostGunId(gun);
        return hostGunId != null && attachmentId != null && AllowAttachmentTagMatcher.match(hostGunId, attachmentId);
    }

    public static List<Map.Entry<ResourceLocation, CommonSlotAdapterIndex>> getAvailableSlotAdapters(ItemStack gun, AttachmentType attachmentType) {
        ResourceLocation hostGunId = getHostGunId(gun);
        if (hostGunId == null || attachmentType == null || attachmentType == AttachmentType.NONE) {
            return ImmutableList.of();
        }
        Set<String> allowList = CommonAssetsManager.get().getAllowAttachmentTags(hostGunId);
        if (allowList == null || allowList.isEmpty()) {
            return ImmutableList.of();
        }

        LinkedHashMap<ResourceLocation, CommonSlotAdapterIndex> candidates = new LinkedHashMap<>();
        Set<ResourceLocation> visitedTags = new HashSet<>();
        for (String value : allowList) {
            ResourceLocation tagId = parseTag(value);
            if (tagId == null || !tagId.getPath().startsWith(ADAPTER_TAG_PATH_PREFIX)) {
                continue;
            }
            expandAdapterTag(tagId, attachmentType, visitedTags, candidates);
        }
        return candidates.entrySet().stream().map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    public static boolean isSlotAdapterAvailable(ItemStack gun, AttachmentType attachmentType, ResourceLocation adapterId) {
        if (adapterId == null) {
            return false;
        }
        return getAvailableSlotAdapters(gun, attachmentType).stream().anyMatch(entry -> entry.getKey().equals(adapterId));
    }

    @Nullable
    public static ResourceLocation getSelectedAvailableSlotAdapter(ItemStack gun, AttachmentType attachmentType) {
        ResourceLocation selected = getSlotAdapter(gun, attachmentType);
        if (selected == null) {
            return null;
        }
        return isSlotAdapterAvailable(gun, attachmentType, selected) ? selected : null;
    }

    public static boolean allowsAttachment(ItemStack gun, AttachmentType attachmentType, ResourceLocation attachmentId) {
        if (attachmentType == null || attachmentType == AttachmentType.NONE || attachmentId == null) {
            return false;
        }
        ResourceLocation selected = getSelectedAvailableSlotAdapter(gun, attachmentType);
        if (selected == null) {
            return allowsDirectAttachment(gun, attachmentId);
        }
        return TimelessAPI.getCommonSlotAdapterIndex(selected)
                .map(index -> index.allowsAttachment(attachmentId))
                .orElse(false);
    }

    @Nullable
    public static ResourceLocation getEffectiveSlotAdapter(ItemStack gun, AttachmentType attachmentType, ResourceLocation attachmentId) {
        ResourceLocation selected = getSelectedAvailableSlotAdapter(gun, attachmentType);
        if (selected == null || attachmentId == null) {
            return null;
        }
        return TimelessAPI.getCommonSlotAdapterIndex(selected)
                .filter(index -> index.allowsAttachment(attachmentId))
                .map(index -> selected)
                .orElse(null);
    }

    public static boolean allowsSlotAdapters(ItemStack holder) {
        return getHostGunId(holder) != null;
    }

    private static void expandAdapterTag(ResourceLocation tagId, AttachmentType attachmentType,
                                         Set<ResourceLocation> visitedTags,
                                         LinkedHashMap<ResourceLocation, CommonSlotAdapterIndex> candidates) {
        if (!visitedTags.add(tagId)) {
            return;
        }
        Set<String> content = CommonAssetsManager.get().getAttachmentTags(tagId);
        if (content == null || content.isEmpty()) {
            return;
        }
        for (String value : content) {
            ResourceLocation nestedTag = parseTag(value);
            if (nestedTag != null) {
                expandAdapterTag(nestedTag, attachmentType, visitedTags, candidates);
                continue;
            }
            ResourceLocation adapterId = ResourceLocations.tryParse(value);
            if (adapterId == null || candidates.containsKey(adapterId)) {
                continue;
            }
            TimelessAPI.getCommonSlotAdapterIndex(adapterId)
                    .filter(index -> index.getSlot() == attachmentType)
                    .ifPresent(index -> candidates.put(adapterId, index));
        }
    }

    @Nullable
    private static ResourceLocation parseTag(String value) {
        if (value == null || !value.startsWith(TAG_PREFIX)) {
            return null;
        }
        return ResourceLocations.tryParse(value.substring(TAG_PREFIX.length()));
    }

    private static String slotKey(AttachmentType type) {
        return type.name();
    }

    @Nullable
    private static ResourceLocation getHostGunId(ItemStack holder) {
        IGun gun = IGun.getIGunOrNull(holder);
        return gun == null ? null : gun.getGunId(holder);
    }
}
