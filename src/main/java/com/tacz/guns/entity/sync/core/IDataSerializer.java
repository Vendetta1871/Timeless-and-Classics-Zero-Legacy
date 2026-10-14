package com.tacz.guns.entity.sync.core;

import net.minecraft.nbt.NBTBase;
import com.tacz.guns.legacy.network.FriendlyByteBuf;

/**
 * Author: MrCrayfish.
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public interface IDataSerializer<T> {
    void write(FriendlyByteBuf buf, T value);

    T read(FriendlyByteBuf buf);

    NBTBase write(T value);

    T read(NBTBase nbt);
}
