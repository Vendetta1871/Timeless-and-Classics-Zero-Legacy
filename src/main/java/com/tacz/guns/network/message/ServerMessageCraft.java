package com.tacz.guns.network.message;

// TODO(port): needs GunSmithTableScreen
// import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageCraft {
    private final int menuId;

    public ServerMessageCraft(int menuId) {
        this.menuId = menuId;
    }

    public static void encode(ServerMessageCraft message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.menuId);
    }

    public static ServerMessageCraft decode(FriendlyByteBuf buf) {
        return new ServerMessageCraft(buf.readVarInt());
    }

    public static void handle(ServerMessageCraft message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> updateScreen(message.menuId));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void updateScreen(int containerId) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        // TODO(port): needs GunSmithTableScreen
        // if (player != null && player.openContainer.windowId == containerId && Minecraft.getMinecraft().currentScreen instanceof GunSmithTableScreen screen) {
        //     screen.updateIngredientCount();
        // }
    }
}
