package com.tacz.guns.legacy.advancement;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.resource.PreparableReloadListener;
import com.tacz.guns.legacy.resource.ResourceManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The gun packs' advancements ({@code data/<namespace>/advancements}). On 1.20.1 gun packs are data packs and vanilla
 * loads these; 1.12.2 reads advancements only from mod jars and the world folder, so they are read with the rest of
 * the pack data ({@link CommonAssetsManager#onReload()}) and added to every {@code AdvancementManager#reload} by
 * {@code AdvancementManagerMixin}.
 * <p>
 * The 1.20.1 JSON is converted first: criteria with triggers 1.12.2 lacks or cannot parse in their 1.20.1 form go
 * to this mod's ports ({@link RecipeCraftedTrigger}, {@link ItemUsedOnBlockTrigger},
 * {@link PlayerKilledEntityTrigger}), and 1.20.1 item predicates ({@code items} lists, {@code tag}) in vanilla
 * triggers' conditions become {@link LegacyItemPredicate}s. An advancement that still cannot be loaded (an entity
 * or item 1.12.2 does not have) is skipped with a warning.
 */
public final class GunPackAdvancements implements PreparableReloadListener {
    public static final GunPackAdvancements INSTANCE = new GunPackAdvancements();
    private static final Map<String, String> TRIGGERS = ImmutableMap.of(
            "minecraft:recipe_crafted", RecipeCraftedTrigger.ID.toString(),
            "minecraft:item_used_on_block", ItemUsedOnBlockTrigger.ID.toString(),
            "minecraft:player_killed_entity", PlayerKilledEntityTrigger.ID.toString());

    private Map<ResourceLocation, JsonObject> advancements = ImmutableMap.of();

    private GunPackAdvancements() {
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonObject> loaded = new LinkedHashMap<>();
        ResourceScanner.scanDirectory(resourceManager, "advancements", CommonAssetsManager.GSON).forEach((id, json) -> {
            if (json.isJsonObject()) {
                loaded.put(id, convert(json.getAsJsonObject()));
            }
        });
        advancements = loaded;
    }

    /**
     * Adds the gun pack advancements to the ones {@code AdvancementManager#reload} is about to load. Advancements of
     * the world folder and the mod jars keep precedence.
     */
    public void addTo(Map<ResourceLocation, Advancement.Builder> map) {
        int added = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : advancements.entrySet()) {
            if (map.containsKey(entry.getKey())) {
                continue;
            }
            try {
                map.put(entry.getKey(), JsonUtils.gsonDeserialize(AdvancementManager.GSON, entry.getValue().toString(), Advancement.Builder.class));
                added++;
            } catch (RuntimeException e) {
                GunMod.LOGGER.warn("Skipping gun pack advancement {}: {}", entry.getKey(), e.getMessage());
            }
        }
        GunMod.LOGGER.info("Added {} gun pack advancements", added);
    }

    static JsonObject convert(JsonObject advancement) {
        if (advancement.has("criteria") && advancement.get("criteria").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : advancement.getAsJsonObject("criteria").entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject criterion = entry.getValue().getAsJsonObject();
                String trigger = new ResourceLocation(JsonUtils.getString(criterion, "trigger", "")).toString();
                String ported = TRIGGERS.get(trigger);
                if (ported != null) {
                    criterion.addProperty("trigger", ported);
                } else if (trigger.startsWith("minecraft:") && criterion.has("conditions")) {
                    markItemPredicates(criterion.get("conditions"));
                }
            }
        }
        return advancement;
    }

    /**
     * Selects {@link LegacyItemPredicate} for every object that reads as a 1.20.1 item predicate: a list of item ids
     * in {@code items}, or a {@code tag}.
     */
    private static void markItemPredicates(JsonElement element) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(GunPackAdvancements::markItemPredicates);
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("type") && (object.has("tag") || isIdList(object.get("items")))) {
            object.add("type", new JsonPrimitive(LegacyItemPredicate.TYPE.toString()));
            return;
        }
        object.entrySet().forEach(entry -> markItemPredicates(entry.getValue()));
    }

    private static boolean isIdList(JsonElement element) {
        if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() == 0) {
            return false;
        }
        for (JsonElement id : element.getAsJsonArray()) {
            if (!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
                return false;
            }
        }
        return true;
    }
}
