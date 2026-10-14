package com.tacz.guns.crafting;

import com.google.gson.JsonObject;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 工作台配方序列化器
 * <p>
 * 1.12.2: not a {@code RecipeSerializer} (no such registry); {@code legacy.crafting.RecipeManager} calls
 * {@link #fromJson}. Recipes reach the client as JSON with the gun pack network cache, so upstream's
 * {@code fromNetwork}/{@code toNetwork} are not ported.
 */
public class GunSmithTableSerializer {
    @Nullable
    public GunSmithTableRecipe fromJson(ResourceLocation id, JsonObject jsonObject) {
        TableRecipe tableRecipe = CommonAssetsManager.GSON.fromJson(jsonObject, TableRecipe.class);
        if (tableRecipe != null) {
            return new GunSmithTableRecipe(id, tableRecipe);
        }
        return null;
    }
}
