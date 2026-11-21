package com.tacz.guns.compat.jei;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.compat.jei.category.*;
import com.tacz.guns.compat.jei.entry.AmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
import com.tacz.guns.compat.jei.entry.AttachmentQueryData;
import com.tacz.guns.compat.jei.entry.GunAmmoQueryEntry;
import com.tacz.guns.compat.jei.entry.GunAttachmentQueryEntry;
import com.tacz.guns.compat.jei.transfer.GunSmithTableTransferHandler;
import com.tacz.guns.crafting.AmmoStorageUpgradeRecipe;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModRecipe;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 1.12.2: JEI integration ({@code mezz.jei} 4.x, HadEnoughItems keeps the same API package).
 * <p>
 * Ported from upstream's 1.20 JEI plugin: one recipe category per gun smith table block, the attachment / ammo
 * query categories, the gun smith table transfer handler and the subtype interpreters. Upstream's {@code RecipeType}s
 * are category uids here, and JEI 4 registers recipes, catalysts and transfer handlers all in {@link #register}.
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
        registration.addRecipeCategories(new GunAttachmentQueryCategory(jeiHelpers.getGuiHelper()));
        registration.addRecipeCategories(new GunAmmoQueryCategory(jeiHelpers.getGuiHelper()));
        registration.addRecipeCategories(new AmmoQueryCategory(jeiHelpers.getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registration) {
        List<GunSmithTableRecipe> recipes = new ArrayList<>();
        recipes.addAll(ClientGunPackData.recipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get()));

        long workbenchStart = System.nanoTime();
        recipeCategoryUidMap.forEach((blockId, uid) -> TimelessAPI.getCommonBlockIndex(blockId).ifPresent(blockIndex -> {
            List<GunSmithTableRecipe> recipeList = blockIndex.getFilter().filter(recipes, GunSmithTableRecipe::getId);
            recipeList.removeIf(recipe -> blockIndex.getData().getTabs().stream()
                    .noneMatch(tab -> Objects.equals(tab.id(), recipe.getResult().getGroup())));
            registration.handleRecipes(GunSmithTableRecipe.class, GunSmithTableCategory.Wrapper::new, uid);
            registration.addRecipes(recipeList, uid);
            // upstream: registerRecipeCatalysts
            ItemStack catalyst = BlockItemBuilder.create(blockIndex.getBlock()).setId(blockId).build();
            registration.addRecipeCatalyst(catalyst, uid);
        }));

        List<IRecipe> ammoStorageUpgradeRecipes = ForgeRegistries.RECIPES.getValuesCollection().stream()
                .filter(AmmoStorageUpgradeRecipe.class::isInstance)
                .map(recipe -> expandToShapedRecipe((AmmoStorageUpgradeRecipe) recipe))
                .collect(Collectors.toList());
        registration.addRecipes(ammoStorageUpgradeRecipes, VanillaRecipeCategoryUid.CRAFTING);
        long workbenchNanos = System.nanoTime() - workbenchStart;

        long attachmentStart = System.nanoTime();
        AttachmentQueryData attachmentQueries = AttachmentQueryData.load();
        long attachmentBuildNanos = System.nanoTime() - attachmentStart;
        registration.handleRecipes(AttachmentQueryEntry.class, AttachmentQueryCategory.Wrapper::new, AttachmentQueryCategory.UID);
        long attachmentRegisterNanos = registerTimed(registration, AttachmentQueryCategory.UID, attachmentQueries.attachmentQueries());
        registration.handleRecipes(GunAttachmentQueryEntry.class, GunAttachmentQueryCategory.Wrapper::new, GunAttachmentQueryCategory.GUN_ATTACHMENT_QUERY);
        long gunAttachmentRegisterNanos = registerTimed(registration, GunAttachmentQueryCategory.GUN_ATTACHMENT_QUERY, attachmentQueries.gunAttachmentQueries());

        long gunAmmoStart = System.nanoTime();
        List<GunAmmoQueryEntry> gunAmmoEntries = GunAmmoQueryEntry.getAllGunAmmoQueryEntries();
        long gunAmmoBuildNanos = System.nanoTime() - gunAmmoStart;
        registration.handleRecipes(GunAmmoQueryEntry.class, GunAmmoQueryCategory.Wrapper::new, GunAmmoQueryCategory.GUN_AMMO_QUERY);
        long gunAmmoRegisterNanos = registerTimed(registration, GunAmmoQueryCategory.GUN_AMMO_QUERY, gunAmmoEntries);
        long ammoStart = System.nanoTime();
        List<AmmoQueryEntry> ammoEntries = AmmoQueryEntry.getAllAmmoQueryEntries(gunAmmoEntries);
        long ammoBuildNanos = System.nanoTime() - ammoStart;
        registration.handleRecipes(AmmoQueryEntry.class, AmmoQueryCategory.Wrapper::new, AmmoQueryCategory.AMMO_QUERY);
        long ammoRegisterNanos = registerTimed(registration, AmmoQueryCategory.AMMO_QUERY, ammoEntries);

        GunMod.LOGGER.info("JEI query timings (ms): workbench/crafting={}, attachment build={} ({} guns, {} attachments, {} checks), " +
                        "attachment register={}, gun-attachment register={}, gun-ammo build={}, gun-ammo register={}, ammo build={}, ammo register={}",
                TimeUnit.NANOSECONDS.toMillis(workbenchNanos), TimeUnit.NANOSECONDS.toMillis(attachmentBuildNanos),
                attachmentQueries.gunCount(), attachmentQueries.attachmentCount(), attachmentQueries.compatibilityChecks(),
                TimeUnit.NANOSECONDS.toMillis(attachmentRegisterNanos), TimeUnit.NANOSECONDS.toMillis(gunAttachmentRegisterNanos),
                TimeUnit.NANOSECONDS.toMillis(gunAmmoBuildNanos), TimeUnit.NANOSECONDS.toMillis(gunAmmoRegisterNanos),
                TimeUnit.NANOSECONDS.toMillis(ammoBuildNanos), TimeUnit.NANOSECONDS.toMillis(ammoRegisterNanos));

        // upstream: registerRecipeTransferHandlers
        IRecipeTransferHandlerHelper transferHelper = registration.getJeiHelpers().recipeTransferHandlerHelper();
        for (String uid : recipeCategoryUidMap.values()) {
            registration.getRecipeTransferRegistry().addRecipeTransferHandler(new GunSmithTableTransferHandler(uid, transferHelper), uid);
        }
    }

    private static <T> long registerTimed(IModRegistry registration, String type, List<T> recipes) {
        long start = System.nanoTime();
        registration.addRecipes(recipes, type);
        return System.nanoTime() - start;
    }

    /**
     * {@link AmmoStorageUpgradeRecipe} 继承自 {@code CustomRecipe}，
     * 其 isSpecial() 返回 true。JEI 无法处理特殊配方（不会读取其布局与产物，导致无法通过物品检索到配方）。
     * 这里参照原版 JEI 内置插件对特殊配方的处理方式，将其展开为一个等价的、JEI 能够识别的 {@link ShapedRecipes}。
     * <p>
     * 1.12.2: JEI 4 only shows crafting recipes of the classes its vanilla plugin handles (custom {@link IRecipe}s are
     * skipped), so the recipe is expanded into a {@link ShapedRecipes} as upstream does.
     */
    private static IRecipe expandToShapedRecipe(AmmoStorageUpgradeRecipe recipe) {
        ShapedRecipes shapedRecipe = new ShapedRecipes(
                "",
                recipe.getRecipeWidth(),
                recipe.getRecipeHeight(),
                recipe.getIngredients(),
                recipe.getRecipeOutput()
        );
        shapedRecipe.setRegistryName(recipe.getRegistryName());
        return shapedRecipe;
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
