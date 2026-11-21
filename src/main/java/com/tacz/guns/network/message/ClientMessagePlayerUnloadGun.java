package com.tacz.guns.network.message;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;
import com.tacz.guns.legacy.util.Components;

public class ClientMessagePlayerUnloadGun {
    private final int slot;
    private final ResourceLocation gunId;

    public ClientMessagePlayerUnloadGun(int slot, ResourceLocation gunId) {
        this.slot = slot;
        this.gunId = gunId;
    }

    public static void encode(ClientMessagePlayerUnloadGun message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.slot);
        buf.writeResourceLocation(message.gunId);
    }

    public static ClientMessagePlayerUnloadGun decode(FriendlyByteBuf buf) {
        return new ClientMessagePlayerUnloadGun(buf.readVarInt(), buf.readResourceLocation());
    }

    public static void handle(ClientMessagePlayerUnloadGun message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP player = context.getSender();
                if (player == null) {
                    return;
                }
                if (!player.isEntityAlive() || player.isSpectator()
                        || message.slot < 0 || message.slot >= 9 || player.inventory.currentItem != message.slot) {
                    showCannotUnload(player);
                    return;
                }
                var stack = player.getHeldItemMainhand();
                IGun gun = IGun.getIGunOrNull(stack);
                if (gun == null || !message.gunId.equals(gun.getGunId(stack))) {
                    showCannotUnload(player);
                    return;
                }
                var index = TimelessAPI.getCommonGunIndex(message.gunId).orElse(null);
                if (index == null) {
                    showCannotUnload(player);
                    return;
                }
                if (!index.canUnloadAmmo() || gun.useInventoryAmmo(stack)) {
                    player.sendStatusMessage(Components.translatable("message.tacz.unload.unsupported"), true);
                    return;
                }
                Bolt bolt = index.getGunData().getBolt();
                long ammoBefore = getLoadedAmmoCount(gun, stack, bolt);
                if (ammoBefore == 0) {
                    player.sendStatusMessage(Components.translatable("message.tacz.unload.no_ammo"), true);
                    return;
                }
                var gunDisplayId = gun.getGunDisplayId(stack);
                IGunOperator.fromLivingEntity(player).unloadAmmo();
                player.inventoryContainer.detectAndSendChanges();
                if (getLoadedAmmoCount(gun, stack, bolt) < ammoBefore) {
                    NetworkHandler.sendToClientPlayer(new ServerMessageGunUnload(message.gunId, gunDisplayId), player);
                } else {
                    showCannotUnload(player);
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static long getLoadedAmmoCount(IGun gun, ItemStack stack, Bolt bolt) {
        return Math.max(0L, gun.getCurrentAmmoCount(stack))
                + (bolt != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(stack) ? 1L : 0L);
    }

    private static void showCannotUnload(EntityPlayerMP player) {
        player.sendStatusMessage(Components.translatable("message.tacz.unload.cannot"), true);
    }
}
