package com.tacz.guns.network.message;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.util.AmmoStorageHelper;
import net.minecraft.util.math.BlockPos;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.HoverNames;

public class ClientMessageAmmoStorageInteract {
    private static final String LAST_PICKUP_TAG = "TacAmmoStorageLastPickup";
    private static final String LAST_ACCUMULATE_TAG = "TacAmmoStorageLastAccumulate";
    private static final String ACCUMULATE_AMMO_TAG = "TacAmmoStorageAccumulateAmmo";
    private static final String ACCUMULATE_COUNT_TAG = "TacAmmoStorageAccumulateCount";
    private static final long PICKUP_COOLDOWN_TICKS = 5L;
    private static final long ACCUMULATE_WINDOW_TICKS = 20L;

    private final BlockPos pos;

    public ClientMessageAmmoStorageInteract(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ClientMessageAmmoStorageInteract message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
    }

    public static ClientMessageAmmoStorageInteract decode(FriendlyByteBuf buf) {
        return new ClientMessageAmmoStorageInteract(buf.readBlockPos());
    }

    public static void handle(ClientMessageAmmoStorageInteract message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP player = context.getSender();
                if (player == null || player.isSpectator()) {
                    return;
                }
                long gameTime = player.world.getTotalWorldTime();
                long lastPickup = player.getEntityData().getLong(LAST_PICKUP_TAG);
                if (lastPickup > 0L && gameTime - lastPickup < PICKUP_COOLDOWN_TICKS) {
                    return;
                }
                if (player.getDistanceSq(message.pos.getX() + 0.5D, message.pos.getY() + 0.5D, message.pos.getZ() + 0.5D) > 36.0D) {
                    return;
                }
                ItemStack gunStack = player.getHeldItemMainhand();
                if (!(gunStack.getItem() instanceof IGun gun)) {
                    return;
                }
                ResourceLocation ammoId = TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack))
                        .map(index -> index.getGunData().getAmmoId())
                        .orElse(null);
                if (ammoId == null) {
                    return;
                }
                TileEntity blockEntity = player.world.getTileEntity(message.pos);
                if (!(blockEntity instanceof AmmoStorageBlockEntity storage)) {
                    return;
                }
                int amount = AmmoStorageHelper.getStackSize(AmmoStorageHelper.createAmmoStack(ammoId, 1));
                ItemStack extracted = storage.extractAmmo(ammoId, amount, false);
                if (extracted.isEmpty()) {
                    player.getEntityData().setLong(LAST_PICKUP_TAG, gameTime);
                    player.sendStatusMessage(Components.translatable("message.tacz.ammo_storage.no_gun_stock"), true);
                    return;
                }
                player.getEntityData().setLong(LAST_PICKUP_TAG, gameTime);
                int count = extracted.getCount();
                if (!player.inventory.addItemStackToInventory(extracted)) {
                    player.dropItem(extracted, false);
                }
                int accumulatedCount = getAccumulatedCount(player, ammoId, count, gameTime);
                player.sendStatusMessage(Components.translatable("message.tacz.ammo_storage.picked", HoverNames.getHoverName(AmmoStorageHelper.createAmmoStack(ammoId, 1)), accumulatedCount), true);
                player.world.playSound(null, player.getPosition(), SoundEvents.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 0.8F, 1.0F);
            });
        }
        context.setPacketHandled(true);
    }

    private static int getAccumulatedCount(EntityPlayerMP player, ResourceLocation ammoId, int count, long gameTime) {
        String ammoKey = ammoId.toString();
        long lastAccumulate = player.getEntityData().getLong(LAST_ACCUMULATE_TAG);
        String lastAmmoKey = player.getEntityData().getString(ACCUMULATE_AMMO_TAG);
        int accumulated = count;
        if (lastAccumulate > 0L && gameTime - lastAccumulate <= ACCUMULATE_WINDOW_TICKS && ammoKey.equals(lastAmmoKey)) {
            accumulated += player.getEntityData().getInteger(ACCUMULATE_COUNT_TAG);
        }
        player.getEntityData().setLong(LAST_ACCUMULATE_TAG, gameTime);
        player.getEntityData().setString(ACCUMULATE_AMMO_TAG, ammoKey);
        player.getEntityData().setInteger(ACCUMULATE_COUNT_TAG, accumulated);
        return accumulated;
    }
}
