package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerFireSelect {
    public static void encode(ClientMessagePlayerFireSelect message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerFireSelect decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerFireSelect();
    }

    public static void handle(ClientMessagePlayerFireSelect message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).fireSelect();
            });
        }
        context.setPacketHandled(true);
    }
}
