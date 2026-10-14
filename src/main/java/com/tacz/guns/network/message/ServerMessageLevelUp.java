package com.tacz.guns.network.message;

import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;
import com.tacz.guns.legacy.util.Components;

public class ServerMessageLevelUp {
    private final ItemStack gun;
    private final int level;

    public ServerMessageLevelUp(ItemStack gun, int level) {
        this.gun = gun;
        this.level = level;
    }

    public static void encode(ServerMessageLevelUp message, FriendlyByteBuf buf) {
        buf.writeItemStack(message.gun, true);
        buf.writeInt(message.level);
    }

    public static ServerMessageLevelUp decode(FriendlyByteBuf buf) {
        ItemStack gun = buf.readItem();
        int level = buf.readInt();
        return new ServerMessageLevelUp(gun, level);
    }

    public static void handle(ServerMessageLevelUp message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onLevelUp(message));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void onLevelUp(ServerMessageLevelUp message) {
        int level = message.getLevel();
        ItemStack gun = message.getGun();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        // TODO 在完成了枪械升级逻辑后，解封下面的代码
                /*
                if (GunLevelManager.DAMAGE_UP_LEVELS.contains(level)) {
                    Minecraft.getMinecraft().getToasts().addToast(new GunLevelUpToast(gun,
                            Components.translatable("toast.tacz.level_up"),
                            Components.translatable("toast.tacz.sub.damage_up")));
                } else if (level >= GunLevelManager.MAX_LEVEL) {
                    Minecraft.getMinecraft().getToasts().addToast(new GunLevelUpToast(gun,
                            Components.translatable("toast.tacz.level_up"),
                            Components.translatable("toast.tacz.sub.final_level")));
                } else {
                    Minecraft.getMinecraft().getToasts().addToast(new GunLevelUpToast(gun,
                            Components.translatable("toast.tacz.level_up"),
                            Components.translatable("toast.tacz.sub.level_up")));
                }*/
    }

    public ItemStack getGun() {
        return this.gun;
    }

    public int getLevel() {
        return this.level;
    }
}
