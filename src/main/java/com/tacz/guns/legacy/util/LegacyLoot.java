package com.tacz.guns.legacy.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.item.ItemStack;

/**
 * Rewrites a 1.20.1 loot table JSON (as gun packs write their loot injections) into the 1.12.2 format: entry types
 * without the namespace ({@code minecraft:item} → {@code item}), uniform number providers as plain {@code min/max}
 * ranges (binomial ones become {@code 0..n}) and flattened item ids mapped back with a {@code set_data} function.
 * The input is not modified.
 */
public final class LegacyLoot {
    private LegacyLoot() {
    }

    public static JsonObject toLegacy(JsonObject table) {
        // Gson 2.8.0: deepCopy() is not public
        JsonObject result = new com.google.gson.JsonParser().parse(table.toString()).getAsJsonObject();
        if (result.has("pools")) {
            for (JsonElement pool : result.getAsJsonArray("pools")) {
                convertPool(pool.getAsJsonObject());
            }
        }
        return result;
    }

    private static void convertPool(JsonObject pool) {
        convertNumber(pool, "rolls");
        convertNumber(pool, "bonus_rolls");
        if (pool.has("entries")) {
            for (JsonElement entry : pool.getAsJsonArray("entries")) {
                convertEntry(entry.getAsJsonObject());
            }
        }
    }

    private static void convertEntry(JsonObject entry) {
        if (entry.has("type")) {
            String type = entry.get("type").getAsString();
            if (type.startsWith("minecraft:")) {
                entry.addProperty("type", type.substring("minecraft:".length()));
            }
        }
        if (entry.has("functions")) {
            for (JsonElement function : entry.getAsJsonArray("functions")) {
                JsonObject object = function.getAsJsonObject();
                convertNumber(object, "count");
                convertNumber(object, "damage");
            }
        }
        if ("item".equals(entry.has("type") ? entry.get("type").getAsString() : "") && entry.has("name")) {
            ItemStack stack = LegacyIngredients.flattenItem(entry.get("name").getAsString());
            if (stack != null) {
                entry.addProperty("name", stack.getItem().getRegistryName().toString());
                if (stack.getMetadata() != 0) {
                    JsonObject setData = new JsonObject();
                    setData.addProperty("function", "set_data");
                    setData.addProperty("data", stack.getMetadata());
                    JsonArray functions = entry.has("functions") ? entry.getAsJsonArray("functions") : new JsonArray();
                    functions.add(setData);
                    entry.add("functions", functions);
                }
            }
        }
    }

    private static void convertNumber(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonObject()) {
            return;
        }
        JsonObject provider = object.getAsJsonObject(key);
        String type = provider.has("type") ? provider.get("type").getAsString() : "minecraft:uniform";
        JsonObject range = new JsonObject();
        if (type.endsWith("binomial")) {
            range.add("min", new JsonPrimitive(0));
            range.add("max", provider.get("n"));
        } else if (type.endsWith("constant")) {
            object.add(key, provider.get("value"));
            return;
        } else {
            range.add("min", provider.get("min"));
            range.add("max", provider.get("max"));
        }
        object.add(key, range);
    }
}
