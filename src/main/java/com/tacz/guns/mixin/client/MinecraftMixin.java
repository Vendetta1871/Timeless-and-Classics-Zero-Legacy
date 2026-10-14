package com.tacz.guns.mixin.client;

import com.tacz.guns.legacy.client.InputEvent;
import com.tacz.guns.legacy.resource.ClientGunPacks;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2 only:
 * <ul>
 *     <li>rescans the gun packs before the resource pack list is collected, which is when upstream's pack finder runs;</li>
 *     <li>posts {@link InputEvent.InteractionKeyMappingTriggered} for the attack, use and pick block keys, which
 *     1.12.2 Forge does not have.</li>
 * </ul>
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "refreshResources()V", at = @At("HEAD"))
    private void tacz$rescanGunPacks(CallbackInfo ci) {
        ClientGunPacks.rescan();
    }

    @Inject(method = "clickMouse()V", at = @At("HEAD"), cancellable = true)
    private void tacz$attackKey(CallbackInfo ci) {
        tacz$post(InputEvent.InteractionKeyMappingTriggered.Kind.ATTACK, ci);
    }

    @Inject(method = "rightClickMouse()V", at = @At("HEAD"), cancellable = true)
    private void tacz$useKey(CallbackInfo ci) {
        tacz$post(InputEvent.InteractionKeyMappingTriggered.Kind.USE_ITEM, ci);
    }

    @Inject(method = "middleClickMouse()V", at = @At("HEAD"), cancellable = true)
    private void tacz$pickKey(CallbackInfo ci) {
        tacz$post(InputEvent.InteractionKeyMappingTriggered.Kind.PICK_BLOCK, ci);
    }

    /**
     * Holding the attack key ({@code continueAttack} upstream, which posts the same event).
     */
    @Inject(method = "sendClickBlockToController(Z)V", at = @At("HEAD"), cancellable = true)
    private void tacz$continueAttack(boolean leftClick, CallbackInfo ci) {
        if (leftClick) {
            InputEvent.InteractionKeyMappingTriggered event = new InputEvent.InteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered.Kind.ATTACK, EnumHand.MAIN_HAND);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                Minecraft minecraft = (Minecraft) (Object) this;
                if (minecraft.playerController != null) {
                    minecraft.playerController.resetBlockRemoving();
                }
                ci.cancel();
            }
        }
    }

    private static void tacz$post(InputEvent.InteractionKeyMappingTriggered.Kind kind, CallbackInfo ci) {
        InputEvent.InteractionKeyMappingTriggered event = new InputEvent.InteractionKeyMappingTriggered(kind, EnumHand.MAIN_HAND);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            ci.cancel();
        }
    }
}
