package com.tacz.guns.legacy.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 1.20.1 {@code FriendlyByteBuf}: 1.12.2's {@link PacketBuffer} with the 1.20.1 method names TACZ uses.
 */
public class FriendlyByteBuf extends PacketBuffer {
    /**
     * 1.20.1 limit of {@code readUtf()}.
     */
    public static final int MAX_STRING_LENGTH = 32767;

    public FriendlyByteBuf(ByteBuf wrapped) {
        super(wrapped);
    }

    public FriendlyByteBuf writeUtf(String string) {
        writeString(string);
        return this;
    }

    public String readUtf() {
        return readString(MAX_STRING_LENGTH);
    }

    public String readUtf(int maxLength) {
        return readString(maxLength);
    }

    public FriendlyByteBuf writeEnum(Enum<?> value) {
        writeEnumValue(value);
        return this;
    }

    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return readEnumValue(enumClass);
    }

    public FriendlyByteBuf writeItem(ItemStack stack) {
        writeItemStack(stack);
        return this;
    }

    /**
     * 1.20.1 {@code writeItemStack(stack, limitedTag)}; 1.12.2 always sends the share tag.
     */
    public FriendlyByteBuf writeItemStack(ItemStack stack, boolean limitedTag) {
        writeItemStack(stack);
        return this;
    }

    public ItemStack readItem() {
        try {
            return readItemStack();
        } catch (IOException e) {
            throw new DecoderException(e);
        }
    }

    public FriendlyByteBuf writeNbt(@Nullable NBTTagCompound nbt) {
        writeCompoundTag(nbt);
        return this;
    }

    @Nullable
    public NBTTagCompound readNbt() {
        try {
            return readCompoundTag();
        } catch (IOException e) {
            throw new DecoderException(e);
        }
    }

    public FriendlyByteBuf writeUUID(UUID uuid) {
        writeUniqueId(uuid);
        return this;
    }

    public UUID readUUID() {
        return readUniqueId();
    }

    public <K, V> void writeMap(Map<K, V> map, BiConsumer<FriendlyByteBuf, K> keyWriter, BiConsumer<FriendlyByteBuf, V> valueWriter) {
        writeVarInt(map.size());
        map.forEach((key, value) -> {
            keyWriter.accept(this, key);
            valueWriter.accept(this, value);
        });
    }

    public <K, V> Map<K, V> readMap(Function<FriendlyByteBuf, K> keyReader, Function<FriendlyByteBuf, V> valueReader) {
        int size = readVarInt();
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            K key = keyReader.apply(this);
            V value = valueReader.apply(this);
            map.put(key, value);
        }
        return map;
    }

    public <T> void writeCollection(Collection<T> collection, BiConsumer<FriendlyByteBuf, T> elementWriter) {
        writeVarInt(collection.size());
        for (T element : collection) {
            elementWriter.accept(this, element);
        }
    }

    public <T> List<T> readList(Function<FriendlyByteBuf, T> elementReader) {
        int size = readVarInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(elementReader.apply(this));
        }
        return list;
    }
}
