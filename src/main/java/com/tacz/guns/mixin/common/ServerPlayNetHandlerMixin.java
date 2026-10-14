package com.tacz.guns.mixin.common;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSwapItem;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2: {@code ServerGamePacketListenerImpl#handlePlayerAction} is {@link NetHandlerPlayServer#processPlayerDigging};
 * its swap-hands branch ends with the second {@code setHeldItem} (there is no {@code stopUsingItem} call there).
 */
@Mixin(NetHandlerPlayServer.class)
public class ServerPlayNetHandlerMixin {
    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "processPlayerDigging(Lnet/minecraft/network/play/client/CPacketPlayerDigging;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;setHeldItem(Lnet/minecraft/util/EnumHand;Lnet/minecraft/item/ItemStack;)V", ordinal = 1, shift = At.Shift.AFTER))
    public void applySwapOffhandDraw(CPacketPlayerDigging packetIn, CallbackInfo ci) {
        player.inventoryContainer.detectAndSendChanges();
        NetworkHandler.sendToClientPlayer(new ServerMessageSwapItem(), player);
    }
}
