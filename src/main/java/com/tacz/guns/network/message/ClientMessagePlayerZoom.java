package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerZoom {
    public static void encode(ClientMessagePlayerZoom message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerZoom decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerZoom();
    }

    public static void handle(ClientMessagePlayerZoom message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).zoom();
            });
        }
        context.setPacketHandled(true);
    }
}
