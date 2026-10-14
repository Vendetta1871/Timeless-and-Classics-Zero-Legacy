package com.tacz.guns.network.message;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessagePlayerAim {
    private final boolean isAim;

    public ClientMessagePlayerAim(boolean isAim) {
        this.isAim = isAim;
    }

    public static void encode(ClientMessagePlayerAim message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isAim);
    }

    public static ClientMessagePlayerAim decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerAim(buf.readBoolean());
    }

    public static void handle(ClientMessagePlayerAim message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP entity = context.getSender();
                if (entity == null) {
                    return;
                }
                IGunOperator.fromLivingEntity(entity).aim(message.isAim);
            });
        }
        context.setPacketHandled(true);
    }
}
