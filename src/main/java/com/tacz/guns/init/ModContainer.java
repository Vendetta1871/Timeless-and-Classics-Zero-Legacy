package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import net.minecraft.entity.player.EntityPlayer;
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

    public static final IGuiHandler GUI_HANDLER = new IGuiHandler() {
        @Nullable
        @Override
        public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            if (ID == GUN_SMITH_TABLE_MENU && world.getTileEntity(new BlockPos(x, y, z)) instanceof GunSmithTableBlockEntity gunSmithTable) {
                return gunSmithTable.createMenu(0, player.inventory, player);
            }
            return null;
        }

        @Nullable
        @Override
        public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            // TODO(port): client screens (batch 3)
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
        player.openGui(Loader.instance().getIndexedModList().get(GunMod.MOD_ID).getMod(), GUN_SMITH_TABLE_MENU,
                world, rootPos.getX(), rootPos.getY(), rootPos.getZ());
    }
}
