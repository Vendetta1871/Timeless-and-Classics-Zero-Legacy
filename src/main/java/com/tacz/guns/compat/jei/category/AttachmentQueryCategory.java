package com.tacz.guns.compat.jei.category;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
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
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.12.2 JEI category showing which guns an attachment fits. Ported from upstream's
 * {@code AttachmentQueryCategory} (the recipe type is a category uid here).
 */
@SideOnly(Side.CLIENT)
public class AttachmentQueryCategory implements IRecipeCategory<IRecipeWrapper> {
    public static final String UID = GunMod.MOD_ID + ":attachment_query";
    public static final int MAX_GUN_SHOW_COUNT = 60;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public AttachmentQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(160, 145);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(ModCreativeTabs.ATTACHMENT_SCOPE_TAB.createIcon());
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("jei.tacz.attachment_query.title");
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
        AttachmentQueryEntry entry = ((Wrapper) recipeWrapper).getEntry();
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();

        // 先把配件放在正中央
        itemStacks.init(0, false, 72, 0);
        itemStacks.setBackground(0, slotDraw);

        // 逐行画枪械，每行 9 个
        List<ItemStack> allowGunStacks = entry.getAllowGunStacks();
        int xOffset = 0;
        int yOffset = 20;
        for (int i = 0; i < allowGunStacks.size(); i++) {
            int column = i % 9;
            int row = i / 9;
            xOffset = column * 18;
            yOffset = 20 + row * 18;
            itemStacks.init(i + 1, true, xOffset, yOffset);
            itemStacks.setBackground(i + 1, slotDraw);
        }

        // 如果超出上限，那么最后一格则为来回跳变的物品
        if (!entry.getExtraAllowGunStacks().isEmpty()) {
            itemStacks.init(allowGunStacks.size() + 1, true, xOffset + 18, yOffset);
            itemStacks.setBackground(allowGunStacks.size() + 1, slotDraw);
        }
        itemStacks.set(ingredients);
    }

    /**
     * 1.12.2 JEI wraps recipes in {@link IRecipeWrapper}; upstream passes the entry object to the category.
     */
    public static class Wrapper implements IRecipeWrapper {
        private final AttachmentQueryEntry entry;
        private final List<List<ItemStack>> inputs;
        private final List<ItemStack> outputs;

        public Wrapper(AttachmentQueryEntry entry) {
            this.entry = entry;
            this.inputs = new ArrayList<>();
            this.inputs.add(entry.getAllowGunStacks());
            if (!entry.getExtraAllowGunStacks().isEmpty()) {
                this.inputs.add(entry.getExtraAllowGunStacks());
            }
            this.outputs = Collections.singletonList(entry.getAttachmentStack());
        }

        public AttachmentQueryEntry getEntry() {
            return entry;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutputs(VanillaTypes.ITEM, outputs);
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            if (!entry.getExtraAllowGunStacks().isEmpty()) {
                minecraft.fontRenderer.drawString(I18n.format("jei.tacz.attachment_query.more"), 128, 134, 0x555555);
            }
        }
    }
}
