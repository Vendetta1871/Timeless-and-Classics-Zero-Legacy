package com.tacz.guns.legacy.client;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;

/**
 * Access to 1.12.2 client internals that upstream calls directly.
 */
@SideOnly(Side.CLIENT)
public final class LegacyClientHooks {
    private static final Method SYNC_CURRENT_PLAY_ITEM = ObfuscationReflectionHelper.findMethod(PlayerControllerMP.class, "func_78750_j", void.class);

    private LegacyClientHooks() {
    }

    /**
     * {@code MultiPlayerGameMode#ensureHasSentCarriedItem}.
     */
    public static void syncCurrentPlayItem(PlayerControllerMP controller) {
        try {
            SYNC_CURRENT_PLAY_ITEM.invoke(controller);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
