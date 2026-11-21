package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.init.ModContainerScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;

/**
 * 1.12.2: there is no {@code MenuType} registry; menus are opened through an {@link IGuiHandler}. The gui id replaces
 * the menu type, the block position replaces the extra data the menu type reads from the buffer.
 */
public class ModContainer {
    public static final int GUN_SMITH_TABLE_MENU = 0;
    /**
     * Upstream {@code AMMO_STORAGE_MENU} ({@code IForgeMenuType.create((windowId, inv, data) -> new AmmoStorageMenu(windowId, inv))}):
     * the client menu needs no block entity, the position only finds the server one.
     */
    public static final int AMMO_STORAGE_MENU = 1;

    public static final IGuiHandler GUI_HANDLER = new IGuiHandler() {
        @Nullable
        @Override
        public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            TileEntity blockEntity = world.getTileEntity(new BlockPos(x, y, z));
            if (ID == GUN_SMITH_TABLE_MENU && blockEntity instanceof GunSmithTableBlockEntity gunSmithTable) {
                return gunSmithTable.createMenu(0, player.inventory, player);
            }
            if (ID == AMMO_STORAGE_MENU && blockEntity instanceof AmmoStorageBlockEntity storage) {
                return storage.createMenu(0, player.inventory, player);
            }
            return null;
        }

        @Nullable
        @Override
        public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            if (ID == GUN_SMITH_TABLE_MENU) {
                return ModContainerScreen.createGunSmithTableScreen(player, world, x, y, z);
            }
            if (ID == AMMO_STORAGE_MENU) {
                return ModContainerScreen.createAmmoStorageScreen(player, world, x, y, z);
            }
            return null;
        }
    };

    public static void register(Object mod) {
        NetworkRegistry.INSTANCE.registerGuiHandler(mod, GUI_HANDLER);
    }

    /**
     * {@code NetworkHooks.openScreen(serverPlayer, menuProvider, buf -> buf.writeResourceLocation(blockId))}
     */
    public static void openGunSmithTable(EntityPlayer player, World world, BlockPos rootPos) {
        openMenu(player, GUN_SMITH_TABLE_MENU, world, rootPos);
    }

    /**
     * {@code NetworkHooks.openScreen(serverPlayer, ammoStorageBlockEntity)}
     */
    public static void openAmmoStorage(EntityPlayer player, World world, BlockPos pos) {
        openMenu(player, AMMO_STORAGE_MENU, world, pos);
    }

    private static void openMenu(EntityPlayer player, int id, World world, BlockPos pos) {
        player.openGui(Loader.instance().getIndexedModList().get(GunMod.MOD_ID).getMod(), id,
                world, pos.getX(), pos.getY(), pos.getZ());
    }
}
