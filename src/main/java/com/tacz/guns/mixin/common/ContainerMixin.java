package com.tacz.guns.mixin.common;

import com.tacz.guns.legacy.util.ClickAction;
import com.tacz.guns.legacy.util.ItemClickBehaviour;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/**
 * 1.20's item click overrides ({@code AbstractContainerMenu#tryItemClickBehaviourOverride}) for items implementing
 * {@link ItemClickBehaviour}, such as the ammo box: a plain click on a slot with such an item on the cursor goes to
 * the item first. Runs on both sides like the rest of {@code slotClick}, so the client prediction matches the server.
 */
@Mixin(Container.class)
public abstract class ContainerMixin {
    @Shadow
    public List<Slot> inventorySlots;
    @Shadow
    private int dragMode;
    @Shadow
    private int dragEvent;
    @Shadow
    @Final
    private Set<Slot> dragSlots;

    @Shadow
    protected abstract void resetDrag();

    @Shadow
    public abstract ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player);

    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    private void tacz$itemClickBehaviour(int slotId, int dragType, ClickType clickType, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack carried = player.inventory.getItemStack();
        if (!(carried.getItem() instanceof ItemClickBehaviour behaviour)) {
            return;
        }
        // A click that moves the mouse a little inside the slot ends as a quick craft (drag) over that single slot.
        // 1.20 turns a single slot quick craft into a plain click (AbstractContainerMenu#doClick); 1.12 places the
        // carried item there instead, so a right click on an empty slot would put the ammo box down.
        if (clickType == ClickType.QUICK_CRAFT && Container.getDragEvent(dragType) == 2 && this.dragEvent == 1
                && this.dragSlots.size() == 1 && (this.dragMode == 0 || this.dragMode == 1)) {
            int index = this.dragSlots.iterator().next().slotNumber;
            int button = this.dragMode;
            this.resetDrag();
            cir.setReturnValue(this.slotClick(index, button, ClickType.PICKUP, player));
            return;
        }
        if (clickType != ClickType.PICKUP || (dragType != 0 && dragType != 1) || slotId < 0 || slotId >= this.inventorySlots.size()) {
            return;
        }
        Slot slot = this.inventorySlots.get(slotId);
        if (slot == null) {
            return;
        }
        // like vanilla, return the slot content from before the click: the server compares it with the client's
        ItemStack before = slot.getStack().copy();
        if (behaviour.overrideStackedOnOther(carried, slot, ClickAction.of(dragType), player)) {
            slot.onSlotChanged();
            cir.setReturnValue(before);
        }
    }
}
