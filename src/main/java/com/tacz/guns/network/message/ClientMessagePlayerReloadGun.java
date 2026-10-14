package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerReloadGun {
    public ClientMessagePlayerReloadGun() {
    }

    public static void encode(ClientMessagePlayerReloadGun message, FriendlyByteBuf buf) {
    }

    public static ClientMessagePlayerReloadGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerReloadGun();
    }

    public static void handle(ClientMessagePlayerReloadGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).reload();
            });
        }
        context.setPacketHandled(true);
    }
}
