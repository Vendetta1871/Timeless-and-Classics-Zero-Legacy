package com.tacz.guns.util;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.HoverNames;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

/**
 * 1.12.2: {@code InteractionResult.CONSUME} has no counterpart, {@link EnumActionResult#SUCCESS} stands in for it.
 */
public final class AmmoStorageInteractionHelper {
    private AmmoStorageInteractionHelper() {
    }

    public static EnumActionResult unloadAmmoBox(AmmoStorageBlockEntity storage, ItemStack boxStack, IAmmoBox ammoBox, EntityPlayer player) {
        if (ammoBox.isAllTypeCreative(boxStack) || ammoBox.isCreative(boxStack)) {
            return EnumActionResult.SUCCESS;
        }
        ResourceLocation ammoId = ammoBox.getAmmoId(boxStack);
        int count = ammoBox.getAmmoCount(boxStack);
        if (ammoId.equals(DefaultAssets.EMPTY_AMMO_ID) || count <= 0) {
            return EnumActionResult.SUCCESS;
        }
        ItemStack ammoStack = AmmoStorageHelper.createAmmoStack(ammoId, count);
        int inserted = storage.insertAmmo(ammoStack, false);
        if (inserted <= 0) {
            return EnumActionResult.SUCCESS;
        }
        int remaining = count - inserted;
        ammoBox.setAmmoCount(boxStack, remaining);
        if (remaining <= 0) {
            ammoBox.setAmmoId(boxStack, DefaultAssets.EMPTY_AMMO_ID);
        }
        player.sendStatusMessage(Components.translatable("message.tacz.ammo_storage.unloaded", HoverNames.getHoverName(ammoStack), inserted), true);
        // 1.12.2 has no SoundEvents.BUNDLE_REMOVE_ONE: the leather equip rustle is the closest vanilla sound
        player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F, 1.0F);
        return EnumActionResult.SUCCESS;
    }
}
