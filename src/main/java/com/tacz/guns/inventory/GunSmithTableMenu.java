package com.tacz.guns.inventory;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.legacy.crafting.RecipeManager;
import com.tacz.guns.legacy.util.ItemHandlers;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonBlockIndex;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class GunSmithTableMenu extends Container {
    // 1.12.2: there is no MenuType; the menu is opened through ModContainer's IGuiHandler

    private final ResourceLocation blockId;
    private final RecipeFilter filter;
    /**
     * 1.12.2: the client applies window 0 ({@code inventoryContainer}) slot updates outside the hotbar only while that
     * container is open, and the server only syncs the open container every tick. While this menu is open the player
     * inventory (craft inputs taken, crafted item picked up) is synced by {@link #detectAndSendChanges()} instead.
     */
    private final InventoryPlayer playerInventory;
    private final NonNullList<ItemStack> syncedInventory;

    public GunSmithTableMenu(int id, InventoryPlayer inventory, @Nullable ResourceLocation resourceLocation) {
        super();
        this.windowId = id;
        this.blockId = resourceLocation;
        this.filter = TimelessAPI.getCommonBlockIndex(getBlockId()).map(CommonBlockIndex::getFilter).orElse(null);
        this.playerInventory = inventory;
        this.syncedInventory = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            this.syncedInventory.set(i, inventory.getStackInSlot(i).copy());
        }
    }

    @Nullable
    public ResourceLocation getBlockId() {
        return blockId;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.isEntityAlive();
    }

    /**
     * 1.12.2: sends the changed player inventory slots as window -2 updates (slot = {@link InventoryPlayer} index),
     * which the client applies whatever container is open.
     */
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(this.playerInventory.player instanceof EntityPlayerMP player)) {
            return;
        }
        for (int i = 0; i < this.syncedInventory.size(); i++) {
            ItemStack stack = this.playerInventory.getStackInSlot(i);
            if (!ItemStack.areItemStacksEqual(this.syncedInventory.get(i), stack)) {
                ItemStack copy = stack.copy();
                this.syncedInventory.set(i, copy);
                player.connection.sendPacket(new SPacketSetSlot(-2, i, copy));
            }
        }
    }

    @Nullable
    private GunSmithTableRecipe getRecipe(ResourceLocation recipeId, RecipeManager recipeManager) {
        if (!DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) || SyncConfig.ENABLE_TABLE_FILTER.get()) {
            if (filter != null && !filter.contains(recipeId)) {
                return null;
            }
        }

        GunSmithTableRecipe recipe = recipeManager.byKey(recipeId).orElse(null);
        if (recipe != null) {
            GunSmithTableRecipe gunSmithTableRecipe = recipe;
            boolean flag = TimelessAPI.getCommonBlockIndex(getBlockId()).map(blockIndex -> {
                return blockIndex.getData().getTabs().stream().noneMatch(tab -> tab.id().equals(gunSmithTableRecipe.getTab()));
            }).orElse(true);
            if (DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
                flag = false;
            }
            if (flag) {
                return null;
            }
            return gunSmithTableRecipe;
        }
        return null;
    }

    public void doCraft(ResourceLocation recipeId, EntityPlayer player) {
        GunSmithTableRecipe recipe = getRecipe(recipeId, RecipeManager.of(player.world));
        if (recipe == null) {
            return;
        }
        ItemHandlers.of(player).ifPresent(handler -> {
            // 是创造模式，就不扣材料
            if (!player.isCreative()) {
                Int2IntArrayMap recordCount = new Int2IntArrayMap();
                List<GunSmithTableIngredient> ingredients = recipe.getInputs();

                for (GunSmithTableIngredient ingredient : ingredients) {
                    int count = 0;
                    for (int slotIndex = 0; slotIndex < handler.getSlots(); slotIndex++) {
                        ItemStack stack = handler.getStackInSlot(slotIndex);
                        int stackCount = stack.getCount();
                        if (!stack.isEmpty() && ingredient.getIngredient().apply(stack)) {
                            count = count + stackCount;
                            // 记录扣除的 slot 和数量
                            if (count <= ingredient.getCount()) {
                                // 如果数量不足，全扣
                                recordCount.put(slotIndex, stackCount);
                            } else {
                                //  数量够了，只扣需要的数量
                                int remaining = count - ingredient.getCount();
                                recordCount.put(slotIndex, stackCount - remaining);
                                break;
                            }
                        }
                    }
                    // 数量不够，不执行后续逻辑，合成失败
                    if (count < ingredient.getCount()) {
                        return;
                    }
                }

                // 开始扣材料
                for (int slotIndex : recordCount.keySet()) {
                    handler.extractItem(slotIndex, recordCount.get(slotIndex), false);
                }
            }

            // 给玩家对应的物品
            World level = player.world;
            if (!level.isRemote) {
                EntityItem itemEntity = new EntityItem(level, player.posX, player.posY + 0.5, player.posZ, recipe.getResultItem().copy());
                itemEntity.setPickupDelay(0);
                level.spawnEntity(itemEntity);
            }
            // 更新，否则客户端显示不正确
            player.inventoryContainer.detectAndSendChanges();
            // 1.12.2: window 0 updates of the main inventory are dropped by the client while this menu is open
            this.detectAndSendChanges();
            NetworkHandler.sendToClientPlayer(new ServerMessageCraft(this.windowId), player);
        });
    }
}
