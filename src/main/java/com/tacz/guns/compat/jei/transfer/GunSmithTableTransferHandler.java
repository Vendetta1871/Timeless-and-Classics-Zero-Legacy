package com.tacz.guns.compat.jei.transfer;

import com.tacz.guns.compat.jei.category.GunSmithTableCategory;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.legacy.crafting.RecipeManager;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.12.2: JEI 4 transfer handlers are registered per category uid and get the recipe layout instead of the recipe;
 * the recipe shown in the layout is looked up through {@link GunSmithTableCategory#getRecipe(IRecipeLayout)}.
 */
@SideOnly(Side.CLIENT)
public class GunSmithTableTransferHandler implements IRecipeTransferHandler<GunSmithTableMenu> {
    private final String recipeType;
    private final IRecipeTransferHandlerHelper transferHelper;

    public GunSmithTableTransferHandler(String recipeType, IRecipeTransferHandlerHelper transferHelper) {
        this.recipeType = recipeType;
        this.transferHelper = transferHelper;
    }

    @Override
    public Class<GunSmithTableMenu> getContainerClass() {
        return GunSmithTableMenu.class;
    }

    public String getRecipeType() {
        return this.recipeType;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(GunSmithTableMenu container, IRecipeLayout recipeLayout,
                                               EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        GunSmithTableRecipe recipe = GunSmithTableCategory.getRecipe(recipeLayout);
        if (recipe == null) {
            return this.transferHelper.createInternalError();
        }
        if (!container.canSelectRecipe(recipe.getId(), RecipeManager.of(player.world))) {
            return this.transferHelper.createUserErrorWithTooltip(I18n.format("jei.tacz.gun_smith_table.recipe_unavailable"));
        }
        if (doTransfer) {
            // The table crafts from the player's inventory; transferring only selects the recipe.
            // JEI restores the parent screen after this call, which consumes the selection request.
            container.requestRecipeSelection(recipe.getId());
        }
        return null;
    }
}
