package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunDrawEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.common.MinecraftForge;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageGunDraw {
    private final int entityId;
    private final ItemStack previousGunItem;
    private final ItemStack currentGunItem;

    public ServerMessageGunDraw(int entityId, ItemStack previousGunItem, ItemStack currentGunItem) {
        this.entityId = entityId;
        this.previousGunItem = previousGunItem;
        this.currentGunItem = currentGunItem;
    }

    public static void encode(ServerMessageGunDraw message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeItem(message.previousGunItem);
        buf.writeItem(message.currentGunItem);
    }

    public static ServerMessageGunDraw decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ItemStack previousGunItem = buf.readItem();
        ItemStack currentGunItem = buf.readItem();
        return new ServerMessageGunDraw(entityId, previousGunItem, currentGunItem);
    }

    public static void handle(ServerMessageGunDraw message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void doClientEvent(ServerMessageGunDraw message) {
        WorldClient level = Minecraft.getMinecraft().world;
        if (level == null) {
            return;
        }
        if (level.getEntityByID(message.entityId) instanceof EntityLivingBase livingEntity) {
            GunDrawEvent gunDrawEvent = new GunDrawEvent(livingEntity, message.previousGunItem, message.currentGunItem, Side.CLIENT);
            MinecraftForge.EVENT_BUS.post(gunDrawEvent);
        }
    }
}
