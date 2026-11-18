package com.tacz.guns.legacy.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 1.20 {@code Item#overrideStackedOnOther}. 1.12.2 has no such hook, so items implement this interface and
 * {@code mixin.common.ContainerMixin} calls it from {@code Container#slotClick}, at the place where 1.20's
 * {@code AbstractContainerMenu#tryItemClickBehaviourOverride} calls the item method: a plain click on a slot with
 * this item on the cursor.
 */
public interface ItemClickBehaviour {
    /**
     * @param stack the item on the cursor
     * @param slot  the clicked slot
     * @return true when the click was handled, so the vanilla pick up/place logic is skipped
     */
    boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, EntityPlayer player);
}
