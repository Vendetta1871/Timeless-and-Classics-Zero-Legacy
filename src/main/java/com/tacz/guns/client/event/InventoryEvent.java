package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.event.SwapItemWithOffHand;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAnimationItem;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.ClientIndexManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class InventoryEvent {
    private static final int HOTBAR_WARM_UP_INTERVAL_TICKS = 7;
    private static final int BACKPACK_WARM_UP_INTERVAL_TICKS = 41;

    // 用于切枪逻辑
    private static int oldHotbarSelected = -1;
    private static ItemStack oldHotbarSelectItem = ItemStack.EMPTY;

    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        InventoryPlayer inventory = player.inventory;
        // 玩家切换选中框的情况
        if (oldHotbarSelected != inventory.currentItem) {
            ClientIndexManager.warmUpItem(inventory.getStackInSlot(inventory.currentItem));
            if (oldHotbarSelected == -1) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(ItemStack.EMPTY);
            } else {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(inventory.getStackInSlot(oldHotbarSelected));
            }
            oldHotbarSelected = inventory.currentItem;
            oldHotbarSelectItem = inventory.getStackInSlot(inventory.currentItem).copy();
            return;
        }
        // 玩家选中的物品改变的情况
        ItemStack currentItem = inventory.getStackInSlot(inventory.currentItem);
        if (currentItem.getItem() instanceof IAnimationItem item ) {
            if (!item.isSame(oldHotbarSelectItem, currentItem)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(oldHotbarSelectItem);
            }
        } else {
            if (!ItemStack.areItemStacksEqual(oldHotbarSelectItem, currentItem)) {
                IClientPlayerGunOperator.fromLocalPlayer(player).draw(oldHotbarSelectItem);
            }
        }

        if (!ItemStack.areItemStacksEqual(oldHotbarSelectItem, currentItem)) {
            oldHotbarSelectItem = currentItem.copy();
        }
        if (event.phase == TickEvent.Phase.END) {
            if (player.ticksExisted % HOTBAR_WARM_UP_INTERVAL_TICKS == 0) {
                ClientIndexManager.warmUpEquippedAndHotbarModels();
            }
            if (player.ticksExisted % BACKPACK_WARM_UP_INTERVAL_TICKS == 0) {
                ClientIndexManager.warmUpBackpackModels();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerSwapMainHand(SwapItemWithOffHand event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        IClientPlayerGunOperator.fromLocalPlayer(player).draw(player.getHeldItemMainhand());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // 离开游戏时重置客户端 draw 状态
        oldHotbarSelected = -1;
        oldHotbarSelectItem = ItemStack.EMPTY;
    }

    private static boolean isSame(ItemStack i, ItemStack j) {
        IGun iGun1 = IGun.getIGunOrNull(i);
        IGun iGun2 = IGun.getIGunOrNull(j);
        if (iGun1 != null && iGun2 != null) {
            return iGun1.getGunId(i).equals(iGun2.getGunId(j));
        }
        if (i.isEmpty() || j.isEmpty()) {
            return i.isEmpty() && j.isEmpty();
        }
        return ItemStack.areItemStacksEqual(i, j);
    }
}
