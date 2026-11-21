package com.tacz.guns.compat.playeranimator.animation;

import net.minecraft.client.model.ModelPlayer;

/**
 * Upstream saves the model's emote supplier and the bend of each part so that a hand draw cannot leave another
 * player's animation on the shared model.
 * <p>
 * 1.12.2: the Player Animator port keeps the animation on the player ({@code IAnimatedPlayer}), not on the model,
 * and has no bends, so there is nothing to restore beyond the part angles, which
 * {@link com.tacz.guns.client.animation.third.PlayerModelRenderState} already restores.
 */
public final class PlayerAnimatorModelState {
    private PlayerAnimatorModelState() {
    }

    public static Runnable capture(ModelPlayer model) {
        return () -> {
        };
    }
}
