package com.tacz.guns.entity.sync;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.sync.core.IDataSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import com.tacz.guns.legacy.network.FriendlyByteBuf;

public class ModSerializers {
    public static final IDataSerializer<ReloadState> RELOAD_STATE = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, ReloadState value) {
            buf.writeInt(value.getStateType().ordinal());
            buf.writeLong(value.getCountDown());
        }

        @Override
        public ReloadState read(FriendlyByteBuf buf) {
            ReloadState reloadState = new ReloadState();
            reloadState.setStateType(ReloadState.StateType.values()[buf.readInt()]);
            reloadState.setCountDown(buf.readLong());
            return reloadState;
        }

        @Override
        public NBTBase write(ReloadState value) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("StateType", value.getStateType().toString());
            compound.setLong("CountDown", value.getCountDown());
            return compound;
        }

        @Override
        public ReloadState read(NBTBase nbt) {
            NBTTagCompound compound = (NBTTagCompound) nbt;
            try {
                ReloadState.StateType stateType = ReloadState.StateType.valueOf(compound.getString("StateType"));
                long countDown = compound.getLong("CountDown");
                ReloadState reloadState = new ReloadState();
                reloadState.setStateType(stateType);
                reloadState.setCountDown(countDown);
                return reloadState;
            } catch (IllegalArgumentException ignore) {
            }
            return new ReloadState();
        }
    };
}
