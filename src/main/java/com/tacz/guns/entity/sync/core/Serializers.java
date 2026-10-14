package com.tacz.guns.entity.sync.core;

import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/**
 * Framework provided serializers used for creating a {@link SyncedDataKey}. This covers all
 * primitive types and common objects. You can create your custom serializer by implementing
 * {@link IDataSerializer}.
 * <p>
 * Author: MrCrayfish
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public class Serializers {
    public static final IDataSerializer<Boolean> BOOLEAN = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Boolean value) {
            buf.writeBoolean(value);
        }

        @Override
        public Boolean read(FriendlyByteBuf buf) {
            return buf.readBoolean();
        }

        @Override
        public NBTBase write(Boolean value) {
            return new NBTTagByte((byte) (value ? 1 : 0));
        }

        @Override
        public Boolean read(NBTBase tag) {
            return ((NBTTagByte) tag).getByte() != 0;
        }
    };

    public static final IDataSerializer<Byte> BYTE = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Byte value) {
            buf.writeByte(value);
        }

        @Override
        public Byte read(FriendlyByteBuf buf) {
            return buf.readByte();
        }

        @Override
        public NBTBase write(Byte value) {
            return new NBTTagByte(value);
        }

        @Override
        public Byte read(NBTBase tag) {
            return ((NBTTagByte) tag).getByte();
        }
    };

    public static final IDataSerializer<Short> SHORT = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Short value) {
            buf.writeShort(value);
        }

        @Override
        public Short read(FriendlyByteBuf buf) {
            return buf.readShort();
        }

        @Override
        public NBTBase write(Short value) {
            return new NBTTagShort(value);
        }

        @Override
        public Short read(NBTBase tag) {
            return ((NBTTagShort) tag).getShort();
        }
    };

    public static final IDataSerializer<Integer> INTEGER = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Integer value) {
            buf.writeVarInt(value);
        }

        @Override
        public Integer read(FriendlyByteBuf buf) {
            return buf.readVarInt();
        }

        @Override
        public NBTBase write(Integer value) {
            return new NBTTagInt(value);
        }

        @Override
        public Integer read(NBTBase tag) {
            return ((NBTTagInt) tag).getInt();
        }
    };

    public static final IDataSerializer<Long> LONG = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Long value) {
            buf.writeLong(value);
        }

        @Override
        public Long read(FriendlyByteBuf buf) {
            return buf.readLong();
        }

        @Override
        public NBTBase write(Long value) {
            return new NBTTagLong(value);
        }

        @Override
        public Long read(NBTBase tag) {
            return ((NBTTagLong) tag).getLong();
        }
    };

    public static final IDataSerializer<Float> FLOAT = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Float value) {
            buf.writeFloat(value);
        }

        @Override
        public Float read(FriendlyByteBuf buf) {
            return buf.readFloat();
        }

        @Override
        public NBTBase write(Float value) {
            return new NBTTagFloat(value);
        }

        @Override
        public Float read(NBTBase tag) {
            return ((NBTTagFloat) tag).getFloat();
        }
    };

    public static final IDataSerializer<Double> DOUBLE = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Double value) {
            buf.writeDouble(value);
        }

        @Override
        public Double read(FriendlyByteBuf buf) {
            return buf.readDouble();
        }

        @Override
        public NBTBase write(Double value) {
            return new NBTTagDouble(value);
        }

        @Override
        public Double read(NBTBase tag) {
            return ((NBTTagDouble) tag).getDouble();
        }
    };

    public static final IDataSerializer<Character> CHARACTER = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Character value) {
            buf.writeChar(value);
        }

        @Override
        public Character read(FriendlyByteBuf buf) {
            return buf.readChar();
        }

        @Override
        public NBTBase write(Character value) {
            return new NBTTagInt(value);
        }

        @Override
        public Character read(NBTBase tag) {
            return (char) ((NBTTagInt) tag).getInt();
        }
    };

    public static final IDataSerializer<String> STRING = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, String value) {
            buf.writeUtf(value);
        }

        @Override
        public String read(FriendlyByteBuf buf) {
            return buf.readUtf();
        }

        @Override
        public NBTBase write(String value) {
            return new NBTTagString(value);
        }

        @Override
        public String read(NBTBase tag) {
            return ((NBTTagString) tag).getString();
        }
    };

    public static final IDataSerializer<NBTTagCompound> TAG_COMPOUND = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, NBTTagCompound value) {
            buf.writeNbt(value);
        }

        @Override
        public NBTTagCompound read(FriendlyByteBuf buf) {
            return buf.readNbt();
        }

        @Override
        public NBTBase write(NBTTagCompound value) {
            return value;
        }

        @Override
        public NBTTagCompound read(NBTBase tag) {
            return (NBTTagCompound) tag;
        }
    };

    public static final IDataSerializer<BlockPos> BLOCK_POS = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, BlockPos value) {
            buf.writeBlockPos(value);
        }

        @Override
        public BlockPos read(FriendlyByteBuf buf) {
            return buf.readBlockPos();
        }

        @Override
        public NBTBase write(BlockPos value) {
            return new NBTTagLong(value.toLong());
        }

        @Override
        public BlockPos read(NBTBase tag) {
            return BlockPos.fromLong(((NBTTagLong) tag).getLong());
        }
    };

    public static final IDataSerializer<UUID> UUID = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, UUID value) {
            buf.writeUUID(value);
        }

        @Override
        public UUID read(FriendlyByteBuf buf) {
            return buf.readUUID();
        }

        @Override
        public NBTBase write(UUID value) {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setLong("Most", value.getMostSignificantBits());
            compound.setLong("Least", value.getLeastSignificantBits());
            return compound;
        }

        @Override
        public UUID read(NBTBase tag) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            return new UUID(compound.getLong("Most"), compound.getLong("Least"));
        }
    };

    public static final IDataSerializer<ItemStack> ITEM_STACK = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, ItemStack value) {
            buf.writeItem(value);
        }

        @Override
        public ItemStack read(FriendlyByteBuf buf) {
            return buf.readItem();
        }

        @Override
        public NBTBase write(ItemStack value) {
            return value.writeToNBT(new NBTTagCompound());
        }

        @Override
        public ItemStack read(NBTBase tag) {
            return new ItemStack((NBTTagCompound) tag);
        }
    };

    public static final IDataSerializer<ResourceLocation> RESOURCE_LOCATION = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, ResourceLocation value) {
            buf.writeResourceLocation(value);
        }

        @Override
        public ResourceLocation read(FriendlyByteBuf buf) {
            return buf.readResourceLocation();
        }

        @Override
        public NBTBase write(ResourceLocation value) {
            return new NBTTagString(value.toString());
        }

        @Override
        public ResourceLocation read(NBTBase tag) {
            return new ResourceLocation(((NBTTagString) tag).getString());
        }
    };
}
