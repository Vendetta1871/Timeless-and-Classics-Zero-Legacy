package com.tacz.guns.network.message.event;

import com.tacz.guns.api.event.common.GunMeleeEvent;
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

public class ServerMessageGunMelee {
    private final int shooterId;
    private final ItemStack gunItemStack;

    public ServerMessageGunMelee(int shooterId, ItemStack gunItemStack) {
        this.shooterId = shooterId;
        this.gunItemStack = gunItemStack;
    }

    public static void encode(ServerMessageGunMelee message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.shooterId);
        buf.writeItem(message.gunItemStack);
    }

    public static ServerMessageGunMelee decode(FriendlyByteBuf buf) {
        int shooterId = buf.readVarInt();
        ItemStack gunItemStack = buf.readItem();
        return new ServerMessageGunMelee(shooterId, gunItemStack);
    }

    public static void handle(ServerMessageGunMelee message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> doClientEvent(message));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void doClientEvent(ServerMessageGunMelee message) {
        WorldClient level = Minecraft.getMinecraft().world;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.shooterId) instanceof EntityLivingBase shooter) {
            GunMeleeEvent gunMeleeEvent = new GunMeleeEvent(shooter, message.gunItemStack, Side.CLIENT);
            MinecraftForge.EVENT_BUS.post(gunMeleeEvent);
        }
    }
}
