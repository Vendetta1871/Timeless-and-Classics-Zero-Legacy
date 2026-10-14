package com.tacz.guns.network.message;

import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageRefreshRefitScreen {
    public static void encode(ServerMessageRefreshRefitScreen message, FriendlyByteBuf buf) {
    }

    public static ServerMessageRefreshRefitScreen decode(FriendlyByteBuf buf) {
        return new ServerMessageRefreshRefitScreen();
    }

    public static void handle(ServerMessageRefreshRefitScreen message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(ServerMessageRefreshRefitScreen::updateScreen);
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void updateScreen() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && Minecraft.getMinecraft().currentScreen instanceof GunRefitScreen screen) {
            screen.init();
            // 刷新配件数据，客户端的
            AttachmentPropertyManager.postChangeEvent(player, player.getHeldItemMainhand());
        }
    }
}
