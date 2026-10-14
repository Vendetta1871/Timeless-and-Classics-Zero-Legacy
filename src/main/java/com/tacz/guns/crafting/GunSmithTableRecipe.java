package com.tacz.guns.crafting;

import com.tacz.guns.crafting.result.GunSmithTableResult;
import com.tacz.guns.resource.pojo.data.recipe.TableRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.List;

// TODO(port): upstream implements net.minecraft.world.item.crafting.Recipe<Inventory> (matches/assemble/getSerializer/getType,
//  backed by ModRecipe's serializer and RecipeType). 1.12.2 has a different recipe system (IRecipe registry entries),
//  so this is a plain data holder until the recipe registration is ported (needs ModRecipe).
public class GunSmithTableRecipe {
    private final ResourceLocation id;
    private final GunSmithTableResult result;
    private final List<GunSmithTableIngredient> inputs;

    public GunSmithTableRecipe(ResourceLocation id, GunSmithTableResult result, List<GunSmithTableIngredient> inputs) {
        this.id = id;
        this.result = result;
        this.inputs = inputs;
    }

    public GunSmithTableRecipe(ResourceLocation id, TableRecipe tableRecipe) {
        this(id, tableRecipe.getResult(), tableRecipe.getMaterials());
    }

    // TODO(port): needs ModRecipe (Recipe#matches, Recipe#assemble, Recipe#getSerializer, Recipe#getType)

    public ItemStack getResultItem() {
        return this.result.getResult().copy();
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public ItemStack getOutput() {
        return result.getResult();
    }

    public List<GunSmithTableIngredient> getInputs() {
        return inputs;
    }

    public GunSmithTableResult getResult() {
        return result;
    }

    public void init() {
        result.init();
    }

    public ResourceLocation getTab() {
        return result.getGroup();
    }
}
