package com.tacz.guns.network.message;

import com.tacz.guns.inventory.AmmoStorageMenu;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

/** A virtual entry interaction, not a vanilla inventory-slot click. */
public final class ClientMessageAmmoStorageClick {
    private final int containerId;
    private final int entryId;
    private final AmmoStorageMenu.Action action;

    public ClientMessageAmmoStorageClick(int containerId, int entryId, AmmoStorageMenu.Action action) {
        this.containerId = containerId;
        this.entryId = entryId;
        this.action = action;
    }

    public int containerId() {
        return containerId;
    }

    public int entryId() {
        return entryId;
    }

    public AmmoStorageMenu.Action action() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientMessageAmmoStorageClick)) return false;
        ClientMessageAmmoStorageClick that = (ClientMessageAmmoStorageClick) o;
        return java.util.Objects.equals(containerId, that.containerId) && java.util.Objects.equals(entryId, that.entryId) && java.util.Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(containerId, entryId, action);
    }

    @Override
    public String toString() {
        return "ClientMessageAmmoStorageClick[" + "containerId=" + containerId + ", " + "entryId=" + entryId + ", " + "action=" + action + "]";
    }

    public static void encode(ClientMessageAmmoStorageClick message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.containerId);
        buf.writeVarInt(message.entryId);
        buf.writeEnum(message.action);
    }

    public static ClientMessageAmmoStorageClick decode(FriendlyByteBuf buf) {
        return new ClientMessageAmmoStorageClick(buf.readVarInt(), buf.readVarInt(), buf.readEnum(AmmoStorageMenu.Action.class));
    }

    public static void handle(ClientMessageAmmoStorageClick message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            EntityPlayerMP player = context.getSender();
            if (player != null && player.openContainer instanceof AmmoStorageMenu menu
                    && menu.windowId == message.containerId) {
                menu.handleInteraction(player, message.entryId, message.action);
            }
        });
        context.setPacketHandled(true);
    }
}
