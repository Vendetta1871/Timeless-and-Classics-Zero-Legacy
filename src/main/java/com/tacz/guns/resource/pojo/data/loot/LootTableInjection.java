package com.tacz.guns.resource.pojo.data.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.util.GsonHelper;
import com.tacz.guns.legacy.util.LegacyLoot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.common.ForgeHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LootTableInjection {
    private final List<ResourceLocation> lootTables;
    private final LootTable lootTable;

    public LootTableInjection(List<ResourceLocation> lootTables, LootTable lootTable) {
        this.lootTables = lootTables;
        this.lootTable = lootTable;
    }

    public List<ResourceLocation> lootTables() {
        return lootTables;
    }

    public LootTable lootTable() {
        return lootTable;
    }

    // 1.12.2: Deserializers.createLootTableSerializer() does not exist, this is a copy of LootTableManager.GSON_INSTANCE
    private static final Gson LOOT_TABLE_GSON = new GsonBuilder()
            .registerTypeAdapter(RandomValueRange.class, new RandomValueRange.Serializer())
            .registerTypeAdapter(LootPool.class, new LootPool.Serializer())
            .registerTypeAdapter(LootTable.class, new LootTable.Serializer())
            .registerTypeHierarchyAdapter(LootEntry.class, new LootEntry.Serializer())
            .registerTypeHierarchyAdapter(LootFunction.class, new LootFunctionManager.Serializer())
            .registerTypeHierarchyAdapter(LootCondition.class, new LootConditionManager.Serializer())
            .registerTypeHierarchyAdapter(LootContext.EntityTarget.class, new LootContext.EntityTarget.Serializer())
            .create();

    public static LootTableInjection fromJson(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "loot injection");
        List<ResourceLocation> lootTables = readLootTables(fileId, object);
        if (!object.has("pools")) {
            throw new JsonParseException("Loot injection " + fileId + " must define pools");
        }
        // 1.12.2: gun packs write 1.20 loot tables
        JsonObject legacy = LegacyLoot.toLegacy(object);
        return new LootTableInjection(lootTables, ForgeHooks.loadLootTable(LOOT_TABLE_GSON, fileId, legacy.toString(), true, null));
    }

    private static List<ResourceLocation> readLootTables(ResourceLocation fileId, JsonObject object) {
        List<ResourceLocation> lootTables = new ArrayList<>();
        if (object.has("loot_tables")) {
            for (JsonElement table : GsonHelper.getAsJsonArray(object, "loot_tables")) {
                lootTables.add(new ResourceLocation(GsonHelper.convertToString(table, "loot table")));
            }
        } else if (object.has("loot_table")) {
            lootTables.add(new ResourceLocation(GsonHelper.getAsString(object, "loot_table")));
        } else {
            throw new JsonParseException("Loot injection " + fileId + " must define loot_table or loot_tables");
        }
        return Collections.unmodifiableList(new ArrayList<>(lootTables));
    }

    public List<ItemStack> createStacks(LootContext context) {
        // 1.12.2: LootTable#generateLootForPools takes the random source explicitly
        return new ArrayList<>(lootTable.generateLootForPools(context.getWorld().rand, context));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LootTableInjection that = (LootTableInjection) o;
        return java.util.Objects.equals(lootTables, that.lootTables) && java.util.Objects.equals(lootTable, that.lootTable);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(lootTables, lootTable);
    }

    @Override
    public String toString() {
        return "LootTableInjection[" + "lootTables=" + lootTables + ", " + "lootTable=" + lootTable + "]";
    }
}
