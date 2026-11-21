package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerUnloadGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.legacy.client.LegacyClientHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.item.ItemStack;
import com.tacz.guns.legacy.util.Components;

public class LocalPlayerUnload {
    private final LocalPlayerDataHolder data;
    private final EntityPlayerSP player;

    public LocalPlayerUnload(LocalPlayerDataHolder data, EntityPlayerSP player) {
        this.data = data;
        this.player = player;
    }

    public void unloadAmmo() {
        if (isLocalIdle()) {
            showCannotUnload();
            return;
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof AbstractGunItem gun)) {
            showCannotUnload();
            return;
        }
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (isSyncIdle(operator)) {
            showCannotUnload();
            return;
        }
        var gunId = gun.getGunId(stack);
        var index = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (index == null) {
            showCannotUnload();
            return;
        }
        if (!index.canUnloadAmmo() || gun.useInventoryAmmo(stack)) {
            player.sendStatusMessage(Components.translatable("message.tacz.unload.unsupported"), true);
            return;
        }
        if (gun.getCurrentAmmoCount(stack) <= 0
                && (index.getGunData().getBolt() == Bolt.OPEN_BOLT || !gun.hasBulletInBarrel(stack))) {
            player.sendStatusMessage(Components.translatable("message.tacz.unload.no_ammo"), true);
            return;
        }

        data.lockState();
        data.autoReloadSuppressed = true;
        data.chargeProgress = 0;
        TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
            SoundPlayManager.stopPlayGunSound();
            var stateMachine = display.getAnimationStateMachine();
            if (stateMachine != null) {
                stateMachine.trigger(GunAnimationConstant.INPUT_INSPECT_BREAK_OUT);
            }
        });
        if (Minecraft.getMinecraft().playerController != null) {
            LegacyClientHooks.syncCurrentPlayItem(Minecraft.getMinecraft().playerController);
        }
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerUnloadGun(player.inventory.currentItem, gunId));
    }

    private void showCannotUnload() {
        player.sendStatusMessage(Components.translatable("message.tacz.unload.cannot"), true);
    }

    private boolean isSyncIdle(IGunOperator operator) {
        return operator.getSynReloadState().getStateType().isReloading() || operator.getSynIsBolting()
                || operator.getSynShootCoolDown() != 0 || operator.getSynDrawCoolDown() != 0
                || operator.getSynMeleeCoolDown() != 0 || operator.getSynSprintTime() > 0
                || IClientPlayerGunOperator.fromLocalPlayer(player).getClientShootCoolDown() > 0;
    }

    private boolean isLocalIdle() {
        return !player.isEntityAlive() || player.isSpectator() || data.clientStateLock
                || data.isCharging || data.isBolting || data.clientReloadStarting
                || !data.isShootRecorded || data.hasPendingShoot()
                || data.clientIsAiming || data.clientAimingProgress > 0 || player.isSprinting();
    }
}
