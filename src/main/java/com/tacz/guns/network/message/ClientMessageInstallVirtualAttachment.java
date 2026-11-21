package com.tacz.guns.network.message;

import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.util.VirtualOemAttachment;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMessageInstallVirtualAttachment {
    private final int gunSlotIndex;
    private final AttachmentType attachmentType;
    private final ResourceLocation attachmentId;

    public ClientMessageInstallVirtualAttachment(int gunSlotIndex, AttachmentType attachmentType, ResourceLocation attachmentId) {
        this.gunSlotIndex = gunSlotIndex;
        this.attachmentType = attachmentType;
        this.attachmentId = attachmentId;
    }

    public static void encode(ClientMessageInstallVirtualAttachment message, FriendlyByteBuf buf) {
        buf.writeInt(message.gunSlotIndex);
        buf.writeEnum(message.attachmentType);
        buf.writeResourceLocation(message.attachmentId);
    }

    public static ClientMessageInstallVirtualAttachment decode(FriendlyByteBuf buf) {
        return new ClientMessageInstallVirtualAttachment(buf.readInt(), buf.readEnum(AttachmentType.class), buf.readResourceLocation());
    }

    public static void handle(ClientMessageInstallVirtualAttachment message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                EntityPlayerMP player = context.getSender();
                if (player == null) {
                    return;
                }
                InventoryPlayer inventory = player.inventory;
                ItemStack gunItem = inventory.getStackInSlot(message.gunSlotIndex);
                IGun iGun = IGun.getIGunOrNull(gunItem);
                if (iGun == null || iGun.hasAttachmentLock(gunItem)) {
                    return;
                }
                if (!VirtualOemAttachment.isAllowedVirtualOem(iGun.getGunId(gunItem), message.attachmentId, message.attachmentType)) {
                    return;
                }
                ItemStack virtualAttachment = AttachmentItemBuilder.create().setId(message.attachmentId).build();
                IAttachment iAttachment = IAttachment.getIAttachmentOrNull(virtualAttachment);
                if (iAttachment == null || iAttachment.getType(virtualAttachment) != message.attachmentType || !iGun.allowAttachment(gunItem, virtualAttachment)) {
                    return;
                }
                ItemStack oldAttachmentItem = iGun.getAttachment(gunItem, message.attachmentType);
                if (!oldAttachmentItem.isEmpty() && !VirtualOemAttachment.isMarked(oldAttachmentItem) && inventory.getFirstEmptyStack() == -1) {
                    return;
                }
                VirtualOemAttachment.mark(virtualAttachment);
                ItemStack beforeRefit = gunItem.copy();
                iGun.installAttachment(gunItem, virtualAttachment);
                GunRefitTrigger.INSTANCE.trigger(player, beforeRefit, gunItem);
                if (!oldAttachmentItem.isEmpty() && !VirtualOemAttachment.isMarked(oldAttachmentItem)) {
                    inventory.addItemStackToInventory(oldAttachmentItem);
                }
                AttachmentPropertyManager.postChangeEvent(player, gunItem);
                player.inventoryContainer.detectAndSendChanges();
                NetworkHandler.sendToClientPlayer(new ServerMessageRefreshRefitScreen(), player);
            });
        }
        context.setPacketHandled(true);
    }
}
