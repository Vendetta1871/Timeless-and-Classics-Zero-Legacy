package com.tacz.guns.compat.jei;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.compat.jei.category.AttachmentQueryCategory;
import com.tacz.guns.compat.jei.category.GunSmithTableCategory;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModRecipe;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 1.12.2: JEI integration ({@code mezz.jei} 4.x, HadEnoughItems keeps the same API package).
 * <p>
 * Ported from upstream's 1.20 JEI plugin: one recipe category per gun smith table block, the attachment
 * query category and the subtype interpreters. Upstream's {@code RecipeType}s are category uids here.
 */
@SideOnly(Side.CLIENT)
@JEIPlugin
public class GunModPlugin implements IModPlugin {
    private final Map<ResourceLocation, String> recipeCategoryUidMap = new HashMap<>();

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        recipeCategoryUidMap.clear();
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        TimelessAPI.getAllCommonBlockIndex().forEach(entry -> {
            ResourceLocation blockId = entry.getKey();
            ItemStack icon = BlockItemBuilder.create(entry.getValue().getBlock()).setId(blockId).build();
            String uid = GunMod.MOD_ID + ":gun_smith_table/" + blockId.toString().replace(':', '_');
            registration.addRecipeCategories(new GunSmithTableCategory(jeiHelpers.getGuiHelper(), icon, uid, icon.getDisplayName()));
            recipeCategoryUidMap.put(blockId, uid);
        });
        registration.addRecipeCategories(new AttachmentQueryCategory(jeiHelpers.getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registration) {
        List<GunSmithTableRecipe> recipes = new ArrayList<>();
        recipes.addAll(ClientGunPackData.recipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get()));

        recipeCategoryUidMap.forEach((blockId, uid) -> TimelessAPI.getCommonBlockIndex(blockId).ifPresent(blockIndex -> {
            List<GunSmithTableRecipe> recipeList = blockIndex.getFilter().filter(recipes, GunSmithTableRecipe::getId);
            recipeList.removeIf(recipe -> blockIndex.getData().getTabs().stream()
                    .noneMatch(tab -> Objects.equals(tab.id(), recipe.getResult().getGroup())));
            registration.handleRecipes(GunSmithTableRecipe.class, GunSmithTableCategory.Wrapper::new, uid);
            registration.addRecipes(recipeList, uid);
            ItemStack catalyst = BlockItemBuilder.create(blockIndex.getBlock()).setId(blockId).build();
            registration.addRecipeCatalyst(catalyst, uid);
        }));

        registration.handleRecipes(AttachmentQueryEntry.class, AttachmentQueryCategory.Wrapper::new, AttachmentQueryCategory.UID);
        List<AttachmentQueryEntry> entries = AttachmentQueryEntry.getAllAttachmentQueryEntries();
        registration.addRecipes(entries, AttachmentQueryCategory.UID);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistry registration) {
        // 1.12.2: the client has no gun pack data before it joins a world. JEI builds its item list (the creative tabs
        // are filled from the gun pack index) right after this callback, so the local packs are read here, see
        // ClientGunPackData.
        ClientGunPackData.ensureLoaded();
        registration.registerSubtypeInterpreter(ModItems.AMMO.get(), GunModSubtype.getAmmoSubtype());
        registration.registerSubtypeInterpreter(ModItems.ATTACHMENT.get(), GunModSubtype.getAttachmentSubtype());
        registration.registerSubtypeInterpreter(ModItems.AMMO_BOX.get(), GunModSubtype.getAmmoBoxSubtype());
        registration.registerSubtypeInterpreter(ModItems.WORKBENCH_111.get(), GunModSubtype.getTableSubType());
        registration.registerSubtypeInterpreter(ModItems.WORKBENCH_121.get(), GunModSubtype.getTableSubType());
        registration.registerSubtypeInterpreter(ModItems.WORKBENCH_211.get(), GunModSubtype.getTableSubType());
        GunItemManager.getAllGunItems().forEach(item -> registration.registerSubtypeInterpreter(item.get(), GunModSubtype.getGunSubtype()));
    }
}
