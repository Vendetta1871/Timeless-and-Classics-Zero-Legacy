package com.tacz.guns.network.message;

import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;


public class ServerMessageSyncGunPack {
    private final Map<DataType, Map<ResourceLocation, String>> cache;

    public ServerMessageSyncGunPack(Map<DataType, Map<ResourceLocation, String>> cache) {
        this.cache = cache;
    }

    public static void encode(ServerMessageSyncGunPack message, FriendlyByteBuf buf) {
        buf.writeMap(message.getCache(), FriendlyByteBuf::writeEnum, (buf1, map) -> {
            buf1.writeMap(map, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeUtf);
        });
    }

    public static ServerMessageSyncGunPack decode(FriendlyByteBuf buf) {
        var map = buf.readMap(buf1 -> buf1.readEnum(DataType.class), buf2 -> {
            return buf2.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readUtf);
        });
        return new ServerMessageSyncGunPack(map);
    }

    public static void handle(ServerMessageSyncGunPack message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            boolean remoteConnection = context.getNetworkManager() != null && !context.getNetworkManager().isLocalChannel();
            context.enqueueWork(() -> doSync(message, remoteConnection));
        }
        context.setPacketHandled(true);
    }


    public Map<DataType, Map<ResourceLocation, String>> getCache() {
        return cache;
    }

    @SideOnly(Side.CLIENT)
    private static void doSync(ServerMessageSyncGunPack message, boolean remoteConnection) {
        if (remoteConnection) {
            CommonAssetsManager.clearInstance();
        }
        CommonNetworkCache.INSTANCE.fromNetwork(message.cache);
        // 通知客户端重新构建ClientIndex
        ClientIndexManager.reload();
    }
}
