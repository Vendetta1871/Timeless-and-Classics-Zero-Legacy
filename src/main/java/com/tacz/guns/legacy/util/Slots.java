package com.tacz.guns.legacy.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 1.20 {@code Slot} methods missing on 1.12.2's {@link Slot}, with the same logic.
 */
public final class Slots {
    private Slots() {
    }

    /**
     * {@code Slot#safeInsert(ItemStack)}: moves as much of {@code stack} as fits into the slot.
     *
     * @return {@code stack}, shrunk by the inserted count
     */
    public static ItemStack safeInsert(Slot slot, ItemStack stack) {
        return safeInsert(slot, stack, stack.getCount());
    }

    /**
     * {@code Slot#safeInsert(ItemStack, int)}
     */
    public static ItemStack safeInsert(Slot slot, ItemStack stack, int increment) {
        if (!stack.isEmpty() && slot.isItemValid(stack)) {
            ItemStack itemstack = slot.getStack();
            int i = Math.min(Math.min(increment, stack.getCount()), slot.getItemStackLimit(stack) - itemstack.getCount());
            if (itemstack.isEmpty()) {
                slot.putStack(stack.splitStack(i));
            } else if (ItemStack.areItemsEqual(itemstack, stack) && ItemStack.areItemStackTagsEqual(itemstack, stack)) {
                stack.shrink(i);
                itemstack.grow(i);
                slot.putStack(itemstack);
            }
        }
        return stack;
    }

    /**
     * {@code Slot#safeTake(int, int, Player)}: takes {@code min(count, decrement)} items out of the slot.
     *
     * @return the taken items, or {@link ItemStack#EMPTY}
     */
    public static ItemStack safeTake(Slot slot, int count, int decrement, EntityPlayer player) {
        // Slot#tryRemove
        if (!slot.canTakeStack(player)) {
            return ItemStack.EMPTY;
        }
        // Slot#allowModification
        boolean allowModification = slot.canTakeStack(player) && slot.isItemValid(slot.getStack());
        if (!allowModification && decrement < slot.getStack().getCount()) {
            return ItemStack.EMPTY;
        }
        ItemStack itemstack = slot.decrStackSize(Math.min(count, decrement));
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (slot.getStack().isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        }
        slot.onTake(player, itemstack);
        return itemstack;
    }
}
