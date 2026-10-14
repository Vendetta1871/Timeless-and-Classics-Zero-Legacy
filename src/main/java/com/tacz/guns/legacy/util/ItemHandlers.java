package com.tacz.guns.legacy.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

import java.util.Optional;

/**
 * Stand-in for {@code entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)} of 1.20.1: in 1.12.2 players do
 * not expose an item handler capability, so the player inventory is wrapped explicitly.
 */
public final class ItemHandlers {
    private ItemHandlers() {
    }

    public static Optional<IItemHandler> of(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return Optional.of(new PlayerInvWrapper(((EntityPlayer) entity).inventory));
        }
        if (entity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return Optional.ofNullable(entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null));
        }
        return Optional.empty();
    }
}
