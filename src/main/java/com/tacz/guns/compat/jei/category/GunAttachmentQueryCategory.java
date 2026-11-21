package com.tacz.guns.compat.jei.category;

import com.tacz.guns.GunMod;
import com.tacz.guns.compat.jei.entry.GunAttachmentQueryEntry;
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
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 1.12.2 JEI category showing the attachments of one type that fit a gun. Ported from upstream's
 * {@code GunAttachmentQueryCategory} (the recipe type is a category uid here, the entry is wrapped in {@link Wrapper}).
 */
@SideOnly(Side.CLIENT)
public class GunAttachmentQueryCategory implements IRecipeCategory<IRecipeWrapper> {
    public static final String GUN_ATTACHMENT_QUERY = GunMod.MOD_ID + ":gun_attachment_query";
    private static final String TITLE = "jei.tacz.gun_attachment_query.title";
    private static final int WIDTH = 162;
    private static final int COLUMNS = 9;
    private final IDrawableStatic bgDraw;
    private final IDrawable slotDraw;
    private final IDrawable iconDraw;

    public GunAttachmentQueryCategory(IGuiHelper guiHelper) {
        this.bgDraw = guiHelper.createBlankDrawable(WIDTH, 156);
        this.slotDraw = guiHelper.getSlotDrawable();
        this.iconDraw = guiHelper.createDrawableIngredient(ModCreativeTabs.GUN_RIFLE_TAB.createIcon());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper, IIngredients ingredients) {
        if (!(recipeWrapper instanceof Wrapper)) {
            return;
        }
        GunAttachmentQueryEntry entry = ((Wrapper) recipeWrapper).getEntry();
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        // 枪械作为输出，可从其 JEI 配方页查看全部兼容配件。
        // 1.12.2: JEI 4 slot positions include the 1 px slot border (upstream offsets the background by -1 instead)
        itemStacks.init(0, false, 72, 0);
        itemStacks.setBackground(0, slotDraw);
        for (int i = 0; i < entry.getAttachmentStacks().size(); i++) {
            int x = 1 + i % COLUMNS * 18;
            int y = 35 + i / COLUMNS * 18;
            itemStacks.init(i + 1, true, x - 1, y - 1);
            itemStacks.setBackground(i + 1, slotDraw);
        }
        itemStacks.set(ingredients);
    }

    @Override
    public String getUid() {
        return GUN_ATTACHMENT_QUERY;
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
        private final GunAttachmentQueryEntry entry;
        private final List<List<ItemStack>> inputs;

        public Wrapper(GunAttachmentQueryEntry entry) {
            this.entry = entry;
            this.inputs = new ArrayList<>();
            for (ItemStack attachment : entry.getAttachmentStacks()) {
                this.inputs.add(Collections.singletonList(attachment));
            }
        }

        public GunAttachmentQueryEntry getEntry() {
            return entry;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutput(VanillaTypes.ITEM, entry.getGunStack());
        }

        @Override
        public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            FontRenderer font = minecraft.fontRenderer;
            String typeName = I18n.format("tacz.type." + entry.getAttachmentType().name().toLowerCase(Locale.ROOT) + ".name");
            String groupTitle = I18n.format("jei.tacz.gun_attachment_query.group", typeName, entry.getTotalAttachmentCount());
            font.drawString(groupTitle, (WIDTH - font.getStringWidth(groupTitle)) / 2, 22, 0x555555);
            if (entry.getPageCount() > 1) {
                String page = I18n.format("jei.tacz.gun_attachment_query.page", entry.getPageNumber(), entry.getPageCount());
                font.drawString(page, (WIDTH - font.getStringWidth(page)) / 2, 145, 0x555555);
            }
        }
    }
}
