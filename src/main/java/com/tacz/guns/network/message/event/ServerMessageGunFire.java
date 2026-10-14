package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunFireEvent;
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

public class ServerMessageGunFire {
    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunFire(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunFire message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeItem(message.gunItemStack);
    }

    public static ServerMessageGunFire decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = buf.readItem();
        return new ServerMessageGunFire(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunFire message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void doClientEvent(ServerMessageGunFire message) {
        WorldClient level = Minecraft.getMinecraft().world;
        if (level == null) {
            return;
        }
        if (level.getEntityByID(message.shooterId) instanceof EntityLivingBase shooter) {
            GunFireEvent gunFireEvent = new GunFireEvent(shooter, message.gunItemStack, Side.CLIENT);
            MinecraftForge.EVENT_BUS.post(gunFireEvent);
        }
    }
}
