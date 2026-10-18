package com.tacz.guns.compat.jei.category;

import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.2 JEI category for gun smith table recipes; one instance per gun smith table block.
 * Ported from upstream's 1.20 {@code GunSmithTableCategory} (the recipe type is a category uid here).
 */
@SideOnly(Side.CLIENT)
public class GunSmithTableCategory implements IRecipeCategory<IRecipeWrapper> {
    private final String uid;
    private final String title;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public GunSmithTableCategory(IGuiHelper guiHelper, ItemStack icon, String uid, String title) {
        this.bgDraw = guiHelper.createBlankDrawable(160, 40);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(icon);
        this.uid = uid;
        this.title = title;
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getModName() {
        return "TACZ";
    }

    @Override
    public IDrawable getBackground() {
        return bgDraw;
    }

    @Override
    public IDrawable getIcon() {
        return iconDraw;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper, IIngredients ingredients) {
        if (!(recipeWrapper instanceof Wrapper)) {
            return;
        }
        GunSmithTableRecipe recipe = ((Wrapper) recipeWrapper).getRecipe();
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();

        itemStacks.init(0, false, 3, 12);
        itemStacks.setBackground(0, slotDraw);

        List<GunSmithTableIngredient> inputs = recipe.getInputs();
        int size = inputs.size();
        // 单行排布
        if (size < 7) {
            for (int i = 0; i < size; i++) {
                int xOffset = 35 + 20 * i;
                int yOffset = 12;
                itemStacks.init(i + 1, true, xOffset, yOffset);
                itemStacks.setBackground(i + 1, slotDraw);
            }
        }
        // 双行排布
        else {
            for (int i = 0; i < 6; i++) {
                int xOffset = 35 + 20 * i;
                int yOffset = 2;
                itemStacks.init(i + 1, true, xOffset, yOffset);
                itemStacks.setBackground(i + 1, slotDraw);
            }
            for (int i = 6; i < size; i++) {
                int xOffset = 35 + 20 * (i - 6);
                int yOffset = 22;
                itemStacks.init(i + 1, true, xOffset, yOffset);
                itemStacks.setBackground(i + 1, slotDraw);
            }
        }
        itemStacks.set(ingredients);
    }

    /**
     * 1.12.2 JEI wraps recipes in {@link IRecipeWrapper}; upstream passes the recipe object to the category.
     */
    public static class Wrapper implements IRecipeWrapper {
        private final GunSmithTableRecipe recipe;
        private final List<List<ItemStack>> inputs;
        private final ItemStack output;

        public Wrapper(GunSmithTableRecipe recipe) {
            this.recipe = recipe;
            this.inputs = new ArrayList<>();
            for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack stack : ingredient.getIngredient().getMatchingStacks()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(ingredient.getCount());
                    stacks.add(copy);
                }
                this.inputs.add(stacks);
            }
            this.output = recipe.getOutput().copy();
        }

        public GunSmithTableRecipe getRecipe() {
            return recipe;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutput(VanillaTypes.ITEM, output);
        }
    }
}
