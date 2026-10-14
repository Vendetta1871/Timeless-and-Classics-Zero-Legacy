package com.tacz.guns.entity.sync.core;

import net.minecraft.util.EnumFacing;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DataHolderCapabilityProvider implements ICapabilitySerializable<NBTTagList> {
    public static final Capability<DataHolder> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    private final DataHolder holder = new DataHolder();
    private final LazyOptional<DataHolder> optional = LazyOptional.of(() -> this.holder);

    public void invalidate() {
        this.optional.invalidate();
    }

    @Override
    public NBTTagList serializeNBT() {
        NBTTagList list = new NBTTagList();
        this.holder.dataMap.forEach((key, entry) -> {
            if (key.save()) {
                NBTTagCompound keyTag = new NBTTagCompound();
                keyTag.setString("ClassKey", key.classKey().id().toString());
                keyTag.setString("DataKey", key.id().toString());
                keyTag.put("Value", entry.writeValue());
                list.add(keyTag);
            }
        });
        return list;
    }

    @Override
    public void deserializeNBT(NBTTagList listTag) {
        this.holder.dataMap.clear();
        listTag.forEach(entryTag -> {
            NBTTagCompound keyTag = (NBTTagCompound) entryTag;
            ResourceLocation classKey = ResourceLocation.tryParse(keyTag.getString("ClassKey"));
            ResourceLocation dataKey = ResourceLocation.tryParse(keyTag.getString("DataKey"));
            NBTBase value = keyTag.get("Value");
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

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing side) {
        return CAPABILITY.orEmpty(cap, this.optional);
    }
}
