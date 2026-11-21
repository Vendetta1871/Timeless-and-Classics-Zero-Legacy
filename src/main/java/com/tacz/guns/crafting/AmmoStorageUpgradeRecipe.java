package com.tacz.guns.crafting;

import com.tacz.guns.init.ModItems;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * 1.12.2: there is no {@code CustomRecipe} / {@code RecipeSerializer}; the recipe is a plain {@link IRecipe} that
 * {@code ModRecipe} registers under the ids of upstream's {@code ammo_storage_gold.json} / {@code ammo_storage_diamond.json}
 * (the registry name is set there).
 */
public class AmmoStorageUpgradeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IShapedRecipe {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private final Item from;
    private final Item material;
    private final Item to;

    public AmmoStorageUpgradeRecipe(ResourceLocation id) {
        if (id.getPath().contains("diamond")) {
            from = ModItems.AMMO_STORAGE_GOLD.get();
            material = Items.DIAMOND;
            to = ModItems.AMMO_STORAGE_DIAMOND.get();
        } else {
            from = ModItems.AMMO_STORAGE.get();
            material = Items.GOLD_INGOT;
            to = ModItems.AMMO_STORAGE_GOLD.get();
        }
    }

    @Override
    public boolean matches(InventoryCrafting container, World level) {
        if (container.getWidth() != 3 || container.getHeight() != 3) {
            return false;
        }
        for (int slot = 0; slot < container.getSizeInventory(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (slot == 4) {
                if (stack.getItem() != from) {
                    return false;
                }
            } else if (stack.getItem() != material) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting container) {
        ItemStack result = getRecipeOutput();
        // BlockItem.getBlockEntityData / setBlockEntityData
        NBTTagCompound source = container.getStackInSlot(4).getTagCompound();
        if (source != null && source.hasKey(BLOCK_ENTITY_TAG, 10)) {
            result.setTagInfo(BLOCK_ENTITY_TAG, source.getCompoundTag(BLOCK_ENTITY_TAG).copy());
        }
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        Ingredient materialIngredient = Ingredient.fromItems(material);
        for (int slot = 0; slot < ingredients.size(); slot++) {
            ingredients.set(slot, slot == 4 ? Ingredient.fromItems(from) : materialIngredient);
        }
        return ingredients;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(to);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public int getRecipeWidth() {
        return 3;
    }

    @Override
    public int getRecipeHeight() {
        return 3;
    }

    /**
     * {@code CustomRecipe#isSpecial()}: not shown in the recipe book.
     */
    @Override
    public boolean isDynamic() {
        return true;
    }
}
