package com.tacz.guns.inventory.tooltip;

import com.tacz.guns.legacy.util.Components;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

// 1.12.2 has no TooltipComponent: this is a plain data holder, the tooltip image is drawn by legacy.util.TooltipComponentHandler
public class AmmoStorageTooltip {
    private final List<Entry> entries;
    private final int hidden;

    public AmmoStorageTooltip(List<Entry> entries, int hidden) {
        this.entries = entries;
        this.hidden = hidden;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public int getHidden() {
        return hidden;
    }

    public static final class Entry {
        private final ItemStack ammo;
        private final long count;

        public Entry(ItemStack ammo, long count) {
            this.ammo = ammo;
            this.count = count;
        }

        public ItemStack ammo() {
            return ammo;
        }

        public long count() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry that = (Entry) o;
            return java.util.Objects.equals(ammo, that.ammo) && java.util.Objects.equals(count, that.count);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ammo, count);
        }

        @Override
        public String toString() {
            return "Entry[" + "ammo=" + ammo + ", " + "count=" + count + "]";
        }

        public ITextComponent getAmmoName() {
            // ItemStack#getHoverName(): only used by the client tooltip, where getDisplayName() is localized
            return Components.literal(ammo.getDisplayName());
        }
    }
}
