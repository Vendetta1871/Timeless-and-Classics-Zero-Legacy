package com.tacz.guns.crafting;

import com.tacz.guns.item.GunPegboardItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Upstream's {@code CustomRecipe} ({@code tacz:gun_pegboard_dye}): 1.12.2 has no recipe serializers, the recipe is
 * registered as an {@link IRecipe} by {@code ModRecipe}. Dyes are any item {@link DyeUtils} knows (the vanilla dyes
 * and the ore dictionary's {@code dye*} entries); 1.12.2's vanilla armor dye recipe only takes leather armor.
 */
public class GunPegboardDyeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    public GunPegboardDyeRecipe() {
    }

    @Override
    public boolean matches(InventoryCrafting container, World level) {
        boolean pegboard = false;
        boolean dye = false;
        for (int slot = 0; slot < container.getSizeInventory(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof GunPegboardItem) {
                if (pegboard) return false;
                pegboard = true;
            } else if (DyeUtils.isDye(stack)) {
                dye = true;
            } else {
                return false;
            }
        }
        return pegboard && dye;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting container) {
        ItemStack pegboard = ItemStack.EMPTY;
        List<EnumDyeColor> dyes = new ArrayList<>();
        for (int slot = 0; slot < container.getSizeInventory(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Optional<EnumDyeColor> dye = DyeUtils.colorFromStack(stack);
            if (stack.getItem() instanceof GunPegboardItem && pegboard.isEmpty()) {
                pegboard = stack;
            } else if (dye.isPresent()) {
                dyes.add(dye.get());
            } else {
                return ItemStack.EMPTY;
            }
        }
        return pegboard.isEmpty() || dyes.isEmpty() ? ItemStack.EMPTY : GunPegboardItem.dyeArmor(pegboard, dyes);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
