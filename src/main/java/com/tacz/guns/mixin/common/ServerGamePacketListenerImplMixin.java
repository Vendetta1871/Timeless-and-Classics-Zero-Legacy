package com.tacz.guns.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.12.2: {@code handlePlayerCommand} is {@link NetHandlerPlayServer#processEntityAction}.
 */
@Mixin(NetHandlerPlayServer.class)
public class ServerGamePacketListenerImplMixin {
    @WrapOperation(method = "processEntityAction(Lnet/minecraft/network/play/client/CPacketEntityAction;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;setSprinting(Z)V"))
    public void cancelSprintCommand(EntityPlayerMP player, boolean sprint, Operation<Void> original) {
        IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
        original.call(player, gunOperator.getProcessedSprintStatus(sprint));
    }
}
