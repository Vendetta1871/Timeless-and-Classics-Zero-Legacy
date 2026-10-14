package com.tacz.guns.mixin.client;

import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.12.2: {@code AbstractButton#onClick} is {@link GuiButton}; a click is when the screen plays its press sound.
 */
@Mixin(GuiButton.class)
public class AbstractButtonMixin {
    /**
     * 记录点击按钮的时间，后续方便给予射击冷却，防止点击按钮后误触开火
     */
    @Inject(method = "playPressSound(Lnet/minecraft/client/audio/SoundHandler;)V", at = @At("HEAD"))
    public void onClickHead(SoundHandler soundHandler, CallbackInfo ci) {
        LocalPlayerDataHolder.clientClickButtonTimestamp = System.currentTimeMillis();
    }
}
