package com.tacz.guns.init;

import com.tacz.guns.entity.sync.core.DataHolder;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

/**
 * 1.12.2: the capability is registered with the {@link CapabilityManager} in pre-init (called from {@code GunMod}).
 */
public class CapabilityRegistry {
    public static void onRegisterCapabilities() {
        // the data is saved by DataHolderCapabilityProvider itself (it is an ICapabilitySerializable), so the
        // capability storage has nothing to do
        CapabilityManager.INSTANCE.register(DataHolder.class, new Capability.IStorage<DataHolder>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<DataHolder> capability, DataHolder instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<DataHolder> capability, DataHolder instance, EnumFacing side, NBTBase nbt) {
            }
        }, DataHolder::new);
    }
}
