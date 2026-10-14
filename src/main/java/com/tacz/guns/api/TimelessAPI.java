package com.tacz.guns.api;

import com.tacz.guns.api.client.other.IThirdPersonAnimation;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.crafting.RecipeType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TimelessAPI {
    @SideOnly(Side.CLIENT)
    public static Optional<GunDisplayInstance> getGunDisplay(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            ResourceLocation gunId = iGun.getGunId(stack);
            if (!getCommonGunIndex(gunId).isPresent()) {
                return Optional.empty();
            }
            ResourceLocation displayId = iGun.getGunDisplayId(stack);
            if (displayId.equals(DefaultAssets.DEFAULT_GUN_DISPLAY_ID)) {
                return getClientGunIndex(gunId).map(ClientGunIndex::getDefaultDisplay);
            } else {
                return getGunDisplay(displayId, gunId);
            }
        }
        return Optional.empty();
    }

    @SideOnly(Side.CLIENT)
    public static Optional<ClientGunIndex> getClientGunIndex(ResourceLocation gunId) {
        return Optional.ofNullable(ClientIndexManager.GUN_INDEX.get(gunId));
    }

    @SideOnly(Side.CLIENT)
    public static Optional<GunDisplayInstance> getGunDisplay(ResourceLocation displayId, ResourceLocation fallbackGunId) {
        if (displayId == null || displayId.equals(DefaultAssets.DEFAULT_GUN_DISPLAY_ID)) {
            return getClientGunIndex(fallbackGunId).map(ClientGunIndex::getDefaultDisplay);
        }

        GunDisplayInstance instance = ClientIndexManager.getOrCreateGunDisplay(displayId);
        if (instance == null) {
            return getClientGunIndex(fallbackGunId).map(ClientGunIndex::getDefaultDisplay);
        }
        return Optional.of(instance);
    }

    @SideOnly(Side.CLIENT)
    public static Optional<ClientAttachmentIndex> getClientAttachmentIndex(ResourceLocation attachmentId) {
        return Optional.ofNullable(ClientIndexManager.ATTACHMENT_INDEX.get(attachmentId));
    }

    @SideOnly(Side.CLIENT)
    public static Optional<ClientAmmoIndex> getClientAmmoIndex(ResourceLocation ammoId) {
        return Optional.ofNullable(ClientIndexManager.AMMO_INDEX.get(ammoId));
    }

    @SideOnly(Side.CLIENT)
    public static Optional<ClientBlockIndex> getClientBlockIndex(ResourceLocation blockId) {
        return Optional.ofNullable(ClientIndexManager.BLOCK_INDEX.get(blockId));
    }

    @SideOnly(Side.CLIENT)
    public static Set<Map.Entry<ResourceLocation, ClientGunIndex>> getAllClientGunIndex() {
        return ClientIndexManager.getAllGuns();
    }

    @SideOnly(Side.CLIENT)
    public static Set<Map.Entry<ResourceLocation, ClientAmmoIndex>> getAllClientAmmoIndex() {
        return ClientIndexManager.getAllAmmo();
    }

    @SideOnly(Side.CLIENT)
    public static Set<Map.Entry<ResourceLocation, ClientAttachmentIndex>> getAllClientAttachmentIndex() {
        return ClientIndexManager.getAllAttachments();
    }

    public static Optional<CommonBlockIndex> getCommonBlockIndex(ResourceLocation blockId) {
        return Optional.ofNullable(CommonAssetsManager.get().getBlockIndex(blockId));
    }

    public static Optional<CommonGunIndex> getCommonGunIndex(ResourceLocation gunId) {
        return Optional.ofNullable(CommonAssetsManager.get().getGunIndex(gunId));
    }

    public static Optional<CommonAttachmentIndex> getCommonAttachmentIndex(ResourceLocation attachmentId) {
        return Optional.ofNullable(CommonAssetsManager.get().getAttachmentIndex(attachmentId));
    }

    public static Optional<CommonAmmoIndex> getCommonAmmoIndex(ResourceLocation ammoId) {
        return Optional.ofNullable(CommonAssetsManager.get().getAmmoIndex(ammoId));
    }

    /**
     * @deprecated
     * 不再使用独立的配方同步，而是使用原版的配方加载器<br/>
     * 请用 {@link com.tacz.guns.legacy.crafting.RecipeManager#byKey(ResourceLocation)}和{@link com.tacz.guns.legacy.crafting.RecipeManager#getAllRecipesFor(RecipeType)}获取配方
     */
    @Deprecated
    public static Optional<GunSmithTableRecipe> getRecipe(ResourceLocation recipeId) {
        return Optional.empty();
    }

    public static Set<Map.Entry<ResourceLocation, CommonBlockIndex>> getAllCommonBlockIndex() {
        return CommonAssetsManager.get().getAllBlocks();
    }

    public static Set<Map.Entry<ResourceLocation, CommonGunIndex>> getAllCommonGunIndex() {
        return CommonAssetsManager.get().getAllGuns();
    }

    public static Set<Map.Entry<ResourceLocation, CommonAmmoIndex>> getAllCommonAmmoIndex() {
        return CommonAssetsManager.get().getAllAmmos();
    }

    public static Set<Map.Entry<ResourceLocation, CommonAttachmentIndex>> getAllCommonAttachmentIndex() {
        return CommonAssetsManager.get().getAllAttachments();
    }

    /**
     * @deprecated
     * 不再使用独立的配方同步，而是使用原版的配方加载器<br/>
     * 请用 {@link com.tacz.guns.legacy.crafting.RecipeManager#byKey(ResourceLocation)}和{@link com.tacz.guns.legacy.crafting.RecipeManager#getAllRecipesFor(RecipeType)}获取配方
     */
    @Deprecated
    public static Map<ResourceLocation, GunSmithTableRecipe> getAllRecipes() {
        return Collections.emptyMap();
    }

    public static void registerThirdPersonAnimation(String name, IThirdPersonAnimation animation) {
        ThirdPersonManager.register(name, animation);
    }
}
