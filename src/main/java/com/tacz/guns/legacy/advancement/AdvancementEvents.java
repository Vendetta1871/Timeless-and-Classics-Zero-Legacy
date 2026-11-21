package com.tacz.guns.legacy.advancement;

import com.tacz.guns.GunMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires the ported 1.20.1 triggers from 1.12.2 events: {@link RecipeCraftedTrigger} where 1.20.1's result slot calls
 * {@code triggerRecipeCrafted}, {@link PlayerKilledEntityTrigger} where its {@code awardKillScore} does.
 * ({@link ItemUsedOnBlockTrigger} is fired by {@code PlayerInteractionManagerMixin}.)
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public final class AdvancementEvents {
    private AdvancementEvents() {
    }

    /**
     * Fired before the crafting grid is consumed, so the recipe still matches it.
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.player instanceof EntityPlayerMP player) || !(event.craftMatrix instanceof InventoryCrafting grid)) {
            return;
        }
        IRecipe recipe = CraftingManager.findMatchingRecipe(grid, player.world);
        if (recipe == null || recipe.getRegistryName() == null) {
            return;
        }
        RecipeCraftedTrigger.INSTANCE.trigger(player, recipe.getRegistryName(), stacks(grid));
    }

    private static List<ItemStack> stacks(IInventory inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /**
     * Lowest priority: a cancelled death is no kill. The killer is the one 1.12.2's {@code onDeath} credits.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        EntityLivingBase killed = event.getEntityLiving();
        if (killed.world.isRemote) {
            return;
        }
        Entity killer = killed.getAttackingEntity();
        if (killer == null) {
            killer = event.getSource().getTrueSource();
        }
        if (killer instanceof EntityPlayerMP player && killer != killed) {
            PlayerKilledEntityTrigger.INSTANCE.trigger(player, killed, event.getSource());
        }
    }
}
