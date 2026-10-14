package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerCancelReload {
    public ClientMessagePlayerCancelReload() {
    }

    public static void encode(ClientMessagePlayerCancelReload message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerCancelReload decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerCancelReload();
    }

    public static void handle(ClientMessagePlayerCancelReload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).cancelReload();
            });
        }
        context.setPacketHandled(true);
    }
}
