package com.tacz.guns.network.message;

import com.tacz.guns.inventory.AmmoStorageEntry;
import com.tacz.guns.inventory.AmmoStorageMenu;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;

public final class ServerMessageAmmoStorageContents {
    private final int containerId;
    private final int capacityStacks;
    private final long usedStacks;
    private final List<AmmoStorageEntry> entries;

    public ServerMessageAmmoStorageContents(int containerId, int capacityStacks, long usedStacks, List<AmmoStorageEntry> entries) {
        entries = ImmutableList.copyOf(entries);
        this.containerId = containerId;
        this.capacityStacks = capacityStacks;
        this.usedStacks = usedStacks;
        this.entries = entries;
    }

    public int containerId() {
        return containerId;
    }

    public int capacityStacks() {
        return capacityStacks;
    }

    public long usedStacks() {
        return usedStacks;
    }

    public List<AmmoStorageEntry> entries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerMessageAmmoStorageContents)) return false;
        ServerMessageAmmoStorageContents that = (ServerMessageAmmoStorageContents) o;
        return java.util.Objects.equals(containerId, that.containerId) && java.util.Objects.equals(capacityStacks, that.capacityStacks) && java.util.Objects.equals(usedStacks, that.usedStacks) && java.util.Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, capacityStacks, usedStacks, entries);
    }

    @Override
    public String toString() {
        return "ServerMessageAmmoStorageContents[" + "containerId=" + containerId + ", " + "capacityStacks=" + capacityStacks + ", " + "usedStacks=" + usedStacks + ", " + "entries=" + entries + "]";
    }


    public static void encode(ServerMessageAmmoStorageContents message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.containerId);
        buf.writeVarInt(message.capacityStacks);
        buf.writeVarLong(message.usedStacks);
        buf.writeVarInt(message.entries.size());
        for (AmmoStorageEntry entry : message.entries) {
            buf.writeVarInt(entry.id());
            buf.writeItem(entry.sample());
            buf.writeVarLong(entry.count());
            buf.writeVarLong(entry.remainingCapacity());
        }
    }

    public static ServerMessageAmmoStorageContents decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int capacityStacks = buf.readVarInt();
        long usedStacks = buf.readVarLong();
        int size = buf.readVarInt();
        if (capacityStacks < 0 || usedStacks < 0 || size < 0 || size > AmmoStorageMenu.STORAGE_SLOT_COUNT) {
            throw new DecoderException("Invalid ammo storage entry count: " + size);
        }
        List<AmmoStorageEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int id = buf.readVarInt();
            ItemStack sample = buf.readItem();
            long count = buf.readVarLong();
            long remaining = buf.readVarLong();
            if (id < 0 || sample.isEmpty() || count <= 0 || remaining < 0) {
                throw new DecoderException("Invalid ammo storage entry");
            }
            entries.add(new AmmoStorageEntry(id, sample, count, remaining));
        }
        return new ServerMessageAmmoStorageContents(containerId, capacityStacks, usedStacks, entries);
    }

    public static void handle(ServerMessageAmmoStorageContents message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> updateContents(message));
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void updateContents(ServerMessageAmmoStorageContents message) {
        var player = Minecraft.getMinecraft().player;
        if (player != null && player.openContainer instanceof AmmoStorageMenu menu
                && menu.windowId == message.containerId) {
            menu.setContents(message.capacityStacks, message.usedStacks, message.entries);
        }
    }
}
