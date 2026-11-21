package com.tacz.guns.item;

import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.inventory.AmmoStorageInventory;
import com.tacz.guns.inventory.tooltip.AmmoStorageTooltip;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AmmoStorageBlockItem extends ItemBlock {
    private static final int MAX_TOOLTIP_ITEMS = 5;
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    private static final String ENTRIES_TAG = "Entries";
    private static final String STACK_TAG = "Stack";
    private static final String COUNT_TAG = "Count";

    public AmmoStorageBlockItem(Block block) {
        super(block);
        this.setMaxStackSize(1);
    }

    // TODO(port): canFitInsideContainerItems() == false. 1.12.2 has no such hook (only the shulker box slot checks
    //  for shulker boxes), so a filled storage can be put into a shulker box.

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = super.getItemStackDisplayName(stack);
        if (getBlock() == ModBlocks.AMMO_STORAGE_GOLD.get()) {
            return TextFormatting.YELLOW + name;
        }
        if (getBlock() == ModBlocks.AMMO_STORAGE_DIAMOND.get()) {
            return TextFormatting.AQUA + name;
        }
        return name;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World level, List<String> components, ITooltipFlag flag) {
        super.addInformation(stack, level, components, flag);
        components.add(Components.translatable("tooltip.tacz.ammo_storage.description").withStyle(TextFormatting.GRAY).getFormattedText());
        components.add(Components.translatable("tooltip.tacz.ammo_storage.description.extra").withStyle(TextFormatting.GRAY).getFormattedText());
    }

    // 1.12.2 has no TooltipComponent / Item#getTooltipImage: AmmoStorageTooltip is a plain data class, the tooltip is drawn by legacy.util.TooltipComponentHandler
    public Optional<AmmoStorageTooltip> getTooltipImage(ItemStack stack) {
        // BlockItem.getBlockEntityData(stack)
        NBTTagCompound blockEntityTag = stack.getSubCompound(BLOCK_ENTITY_TAG);
        if (blockEntityTag == null || !blockEntityTag.hasKey(ENTRIES_TAG, Constants.NBT.TAG_LIST)) {
            return Optional.empty();
        }
        NBTTagList entries = blockEntityTag.getTagList(ENTRIES_TAG, Constants.NBT.TAG_COMPOUND);
        List<AmmoStorageTooltip.Entry> mergedEntries = new ArrayList<>();
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entryTag = entries.getCompoundTagAt(i);
            if (!entryTag.hasKey(STACK_TAG, Constants.NBT.TAG_COMPOUND)) {
                continue;
            }
            ItemStack entryStack = new ItemStack(entryTag.getCompoundTag(STACK_TAG));
            long count = entryTag.getLong(COUNT_TAG);
            if (entryStack.isEmpty() || count <= 0) {
                continue;
            }
            mergeTooltipEntry(mergedEntries, entryStack, count);
        }
        if (mergedEntries.isEmpty()) {
            return Optional.empty();
        }
        int displayed = Math.min(MAX_TOOLTIP_ITEMS, mergedEntries.size());
        List<AmmoStorageTooltip.Entry> tooltipEntries = new ArrayList<>(mergedEntries.subList(0, displayed));
        return Optional.of(new AmmoStorageTooltip(tooltipEntries, mergedEntries.size() - displayed));
    }

    private static void mergeTooltipEntry(List<AmmoStorageTooltip.Entry> entries, ItemStack stack, long count) {
        for (int i = 0; i < entries.size(); i++) {
            AmmoStorageTooltip.Entry entry = entries.get(i);
            if (AmmoStorageInventory.isSameItemSameTags(entry.ammo(), stack)) {
                long mergedCount = saturatedAdd(entry.count(), count);
                entries.set(i, new AmmoStorageTooltip.Entry(entry.ammo(), mergedCount));
                return;
            }
        }
        ItemStack sample = stack.copy();
        sample.setCount(1);
        entries.add(new AmmoStorageTooltip.Entry(sample, count));
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
