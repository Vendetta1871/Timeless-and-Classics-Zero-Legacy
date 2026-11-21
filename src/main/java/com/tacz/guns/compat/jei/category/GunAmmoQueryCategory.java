package com.tacz.guns.compat.jei.category;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.jei.entry.GunAmmoQueryEntry;
import com.tacz.guns.init.ModCreativeTabs;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2 JEI category showing the ammo a gun uses. Ported from upstream's {@code GunAmmoQueryCategory}
 * (the recipe type is a category uid here, the entry is wrapped in {@link Wrapper}).
 */
@SideOnly(Side.CLIENT)
public class GunAmmoQueryCategory implements IRecipeCategory<IRecipeWrapper> {
    public static final String GUN_AMMO_QUERY = GunMod.MOD_ID + ":gun_ammo_query";
    private static final String TITLE = "jei.tacz.gun_ammo_query.title";
    private static final String AMMO_LABEL = "jei.tacz.gun_ammo_query.ammo";
    private static final int WIDTH = 162;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public GunAmmoQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(WIDTH, 58);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(ModCreativeTabs.GUN_RIFLE_TAB.createIcon());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper, IIngredients ingredients) {
        if (!(recipeWrapper instanceof Wrapper)) {
            return;
        }
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        // 枪械作为输出，从其 JEI 配方页查询所用弹药。
        // 1.12.2: JEI 4 slot positions include the 1 px slot border (upstream offsets the background by -1 instead)
        itemStacks.init(0, false, 72, 0);
        itemStacks.setBackground(0, slotDraw);
        itemStacks.init(1, true, 72, 38);
        itemStacks.setBackground(1, slotDraw);
        itemStacks.set(ingredients);
    }

    @Override
    public String getUid() {
        return GUN_AMMO_QUERY;
    }

    @Override
    public String getTitle() {
        return I18n.format(TITLE);
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

    /**
     * 1.12.2 JEI wraps recipes in {@link IRecipeWrapper}; upstream passes the entry object to the category.
     */
    public static class Wrapper implements IRecipeWrapper {
        private final GunAmmoQueryEntry entry;

        public Wrapper(GunAmmoQueryEntry entry) {
            this.entry = entry;
        }

        public GunAmmoQueryEntry getEntry() {
            return entry;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInput(VanillaTypes.ITEM, entry.getAmmoStack());
            ingredients.setOutput(VanillaTypes.ITEM, entry.getGunStack());
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            FontRenderer font = minecraft.fontRenderer;
            String ammoLabel = I18n.format(AMMO_LABEL);
            font.drawString(ammoLabel, (WIDTH - font.getStringWidth(ammoLabel)) / 2, 24, 0x555555);
        }
    }
}
