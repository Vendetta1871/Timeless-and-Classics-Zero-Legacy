package com.tacz.guns.entity.sync.core;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DataHolderCapabilityProvider implements ICapabilitySerializable<NBTTagList> {
    // 1.12.2: injected when CapabilityRegistry registers DataHolder
    @CapabilityInject(DataHolder.class)
    public static Capability<DataHolder> CAPABILITY = null;
    private final DataHolder holder = new DataHolder();
    // 1.12.2 capabilities have no LazyOptional; invalidate() only marks the provider as dead
    private boolean valid = true;

    public void invalidate() {
        this.valid = false;
    }

    @Override
    public NBTTagList serializeNBT() {
        NBTTagList list = new NBTTagList();
        this.holder.dataMap.forEach((key, entry) -> {
            if (key.save()) {
                NBTTagCompound keyTag = new NBTTagCompound();
                keyTag.setString("ClassKey", key.classKey().id().toString());
                keyTag.setString("DataKey", key.id().toString());
                keyTag.setTag("Value", entry.writeValue());
                list.appendTag(keyTag);
            }
        });
        return list;
    }

    @Override
    public void deserializeNBT(NBTTagList listTag) {
        this.holder.dataMap.clear();
        listTag.forEach(entryTag -> {
            NBTTagCompound keyTag = (NBTTagCompound) entryTag;
            ResourceLocation classKey = new ResourceLocation(keyTag.getString("ClassKey"));
            ResourceLocation dataKey = new ResourceLocation(keyTag.getString("DataKey"));
            NBTBase value = keyTag.getTag("Value");
            SyncedClassKey<?> syncedClassKey = SyncedEntityData.instance().getClassKey(classKey);
            if (syncedClassKey == null) {
                return;
            }
            SyncedDataKey<?, ?> syncedDataKey = SyncedEntityData.instance().getKey(syncedClassKey, dataKey);
            if (syncedDataKey == null || !syncedDataKey.save()) {
                return;
            }
            DataEntry<?, ?> entry = new DataEntry<>(syncedDataKey);
            entry.readValue(value);
            this.holder.dataMap.put(syncedDataKey, entry);
        });
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return valid && capability == CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return hasCapability(capability, facing) ? CAPABILITY.cast(holder) : null;
    }
}
