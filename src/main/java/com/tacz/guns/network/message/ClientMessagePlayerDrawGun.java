package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerDrawGun {
    public ClientMessagePlayerDrawGun() {
    }

    public static void encode(ClientMessagePlayerDrawGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerDrawGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerDrawGun();
    }

    public static void handle(ClientMessagePlayerDrawGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                InventoryPlayer inventory = entity.inventory;
                int selected = inventory.currentItem;
                IGunOperator.fromLivingEntity(entity).draw(() -> inventory.getItem(selected));
            });
        }
        context.setPacketHandled(true);
    }
}
