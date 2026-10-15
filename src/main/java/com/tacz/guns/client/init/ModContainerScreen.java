package com.tacz.guns.client.init;

import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.12.2: there is no {@code MenuScreens.register}; {@code ModContainer}'s gui handler asks for the screen here
 * ({@link com.tacz.guns.init.ModContainer#GUI_HANDLER}).
 */
@SideOnly(Side.CLIENT)
public class ModContainerScreen {
    /**
     * The client side of {@code ModContainer.GUN_SMITH_TABLE_MENU}: upstream reads the block id from the menu's extra
     * data buffer, here it comes from the block entity at the position.
     */
    @Nullable
    public static Object createGunSmithTableScreen(EntityPlayer player, World world, int x, int y, int z) {
        if (world.getTileEntity(new BlockPos(x, y, z)) instanceof GunSmithTableBlockEntity) {
            GunSmithTableBlockEntity blockEntity = (GunSmithTableBlockEntity) world.getTileEntity(new BlockPos(x, y, z));
            GunSmithTableMenu menu = new GunSmithTableMenu(0, player.inventory, blockEntity.getId());
            return new GunSmithTableScreen(menu, player.inventory, Components.literal(""));
        }
        return null;
    }
}
