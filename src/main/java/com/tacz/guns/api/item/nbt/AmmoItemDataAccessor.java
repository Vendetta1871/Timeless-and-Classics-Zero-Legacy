package com.tacz.guns.api.item.nbt;

import com.tacz.guns.legacy.util.StackNbt;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.util.Constants;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.util.ResourceLocations;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AmmoItemDataAccessor extends IAmmo {
    String AMMO_ID_TAG = "AmmoId";

    @Override
    @Nonnull
    default ResourceLocation getAmmoId(ItemStack ammo) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(ammo);
        if (nbt.hasKey(AMMO_ID_TAG, Constants.NBT.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocations.tryParse(nbt.getString(AMMO_ID_TAG));
            return gunId != null ? gunId : DefaultAssets.EMPTY_AMMO_ID;
        }
        return DefaultAssets.EMPTY_AMMO_ID;
    }

    @Override
    default void setAmmoId(ItemStack ammo, @Nullable ResourceLocation ammoId) {
        NBTTagCompound nbt = StackNbt.getOrCreateTag(ammo);
        if (ammoId != null) {
            nbt.setString(AMMO_ID_TAG, ammoId.toString());
            return;
        }
        nbt.setString(AMMO_ID_TAG, DefaultAssets.DEFAULT_AMMO_ID.toString());
    }

    @Override
    default boolean isAmmoOfGun(ItemStack gun, ItemStack ammo) {
        if (gun.getItem() instanceof IGun iGun && ammo.getItem() instanceof IAmmo iAmmo) {
            ResourceLocation gunId = iGun.getGunId(gun);
            ResourceLocation ammoId = iAmmo.getAmmoId(ammo);
            return TimelessAPI.getCommonGunIndex(gunId).map(gunIndex -> gunIndex.getGunData().getAmmoId().equals(ammoId)).orElse(false);
        }
        return false;
    }
}
