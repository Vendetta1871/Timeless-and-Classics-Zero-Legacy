package com.tacz.guns.mixin.common;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMP.class)
public class ServerPlayerMixin {
    // 1.12.2: restoreFrom is copyFrom
    @Inject(method = "copyFrom(Lnet/minecraft/entity/player/EntityPlayerMP;Z)V", at = @At("RETURN"))
    public void initialGunOperateData(EntityPlayerMP pThat, boolean pKeepEverything, CallbackInfo ci) {
        EntityPlayerMP player = (EntityPlayerMP) (Object) this;
        IGunOperator.fromLivingEntity(player).initialData();
    }
}
