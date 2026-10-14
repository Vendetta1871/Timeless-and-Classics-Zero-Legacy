package com.tacz.guns.legacy.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.legacy.resource.FileToIdConverter;
import com.tacz.guns.legacy.resource.ProfilerFiller;
import com.tacz.guns.legacy.resource.ResourceManager;
import com.tacz.guns.legacy.resource.SimplePreparableReloadListener;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.manager.INetworkCacheReloadListener;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.util.ResourceScanner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The part of 1.20.1 {@code RecipeManager} that TACZ uses, holding only gun smith table recipes.
 * <p>
 * Upstream these are vanilla datapack recipes ({@code recipes/*.json} with type {@code tacz:gun_smith_table_crafting})
 * that vanilla loads and syncs. On 1.12.2 the server loads them from the gun packs' data like the rest of TACZ's
 * data ({@code CommonAssetsManager}) and sends the raw JSON with the network cache ({@link DataType#RECIPES}); the
 * client rebuilds its copy in {@link CommonNetworkCache}. Other recipe types in the gun packs are ignored.
 */
public class RecipeManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> implements INetworkCacheReloadListener {
    private static final FileToIdConverter RECIPES = FileToIdConverter.json("recipes");

    private final Map<ResourceLocation, GunSmithTableRecipe> recipes = new LinkedHashMap<>();
    private Map<ResourceLocation, String> networkCache = ImmutableMap.of();

    /**
     * {@code level.getRecipeManager()}: the server's recipes on the logical server, the synced ones on the client.
     */
    public static RecipeManager of(World level) {
        if (!level.isRemote && CommonAssetsManager.getInstance() != null && CommonAssetsManager.getInstance().recipeManager != null) {
            return CommonAssetsManager.getInstance().recipeManager;
        }
        return CommonNetworkCache.INSTANCE.recipeManager;
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return ResourceScanner.scanDirectory(resourceManager, RECIPES, CommonAssetsManager.GSON);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        recipes.clear();
        ImmutableMap.Builder<ResourceLocation, String> cache = ImmutableMap.builder();
        String type = ModRecipe.GUN_SMITH_TABLE_CRAFTING.get().toString();
        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            JsonElement element = entry.getValue();
            if (!element.isJsonObject() || !element.getAsJsonObject().has("type")
                    || !type.equals(element.getAsJsonObject().get("type").getAsString())) {
                continue;
            }
            if (load(entry.getKey(), element.getAsJsonObject())) {
                cache.put(entry.getKey(), element.toString());
            }
        }
        networkCache = cache.build();
    }

    /**
     * Client side: replaces the recipes with the ones received from the server.
     */
    public void fromNetwork(Map<ResourceLocation, String> data) {
        recipes.clear();
        for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
            try {
                load(entry.getKey(), CommonAssetsManager.GSON.fromJson(entry.getValue(), JsonObject.class));
            } catch (JsonParseException exception) {
                GunMod.LOGGER.warn("Failed to parse recipe from network with id {}", entry.getKey(), exception);
            }
        }
        recipes.values().forEach(GunSmithTableRecipe::init);
    }

    private boolean load(ResourceLocation id, JsonObject json) {
        try {
            GunSmithTableRecipe recipe = ModRecipe.GUN_SMITH_TABLE_RECIPE_SERIALIZER.get().fromJson(id, json);
            if (recipe != null) {
                recipes.put(id, recipe);
                return true;
            }
        } catch (IllegalArgumentException | JsonParseException exception) {
            GunMod.LOGGER.error("Parsing error loading recipe {}", id, exception);
        }
        return false;
    }

    public void clear() {
        recipes.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAllRecipesFor(RecipeType<T> type) {
        if (type != ModRecipe.GUN_SMITH_TABLE_CRAFTING.get()) {
            return Collections.emptyList();
        }
        return (List<T>) new ArrayList<>(recipes.values());
    }

    public Optional<GunSmithTableRecipe> byKey(ResourceLocation id) {
        return Optional.ofNullable(recipes.get(id));
    }

    @Override
    public Map<ResourceLocation, String> getNetworkCache() {
        return networkCache;
    }

    @Override
    public DataType getType() {
        return DataType.RECIPES;
    }
}
