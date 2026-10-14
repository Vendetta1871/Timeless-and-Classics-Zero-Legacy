package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerMelee {
    public static void encode(ClientMessagePlayerMelee message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerMelee decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerMelee();
    }

    public static void handle(ClientMessagePlayerMelee message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).melee();
            });
        }
        context.setPacketHandled(true);
    }
}
