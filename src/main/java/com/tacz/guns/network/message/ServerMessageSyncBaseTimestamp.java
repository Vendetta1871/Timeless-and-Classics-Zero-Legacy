package com.tacz.guns.network.message;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Objects;
import java.util.function.Supplier;

public class ServerMessageSyncBaseTimestamp {
    private static final Marker MARKER = MarkerManager.getMarker("SYNC_BASE_TIMESTAMP");

    public ServerMessageSyncBaseTimestamp() { }

    public static void encode(ServerMessageSyncBaseTimestamp message, FriendlyByteBuf buf) { }

    public static ServerMessageSyncBaseTimestamp decode(FriendlyByteBuf buf) {
        return new ServerMessageSyncBaseTimestamp();
    }

    public static void handle(ServerMessageSyncBaseTimestamp message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            long timestamp = System.currentTimeMillis();
            context.enqueueWork(() -> updateBaseTimestamp(timestamp));
        }
        context.setPacketHandled(true);
        NetworkHandler.CHANNEL.reply(new ClientMessageSyncBaseTimestamp(), context);
    }

    @SideOnly(Side.CLIENT)
    private static void updateBaseTimestamp(long timestamp) {
        EntityPlayerSP player = Objects.requireNonNull(Minecraft.getMinecraft().player);
        LocalPlayerDataHolder dataHolder = IClientPlayerGunOperator.fromLocalPlayer(player).getDataHolder();
        dataHolder.clientBaseTimestamp = timestamp;
        GunMod.LOGGER.debug(MARKER, "Update client base timestamp: {}", dataHolder.clientBaseTimestamp);
    }
}
