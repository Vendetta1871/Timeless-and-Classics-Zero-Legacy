package com.tacz.guns.item;

import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.12.2 has no {@code DyeableLeatherItem}: its methods are implemented here on the same {@code display.color} tag.
 * Upstream's {@code CauldronInteraction.WATER} entry ({@code DYED_ITEM}) is {@link #onItemUseFirst}.
 */
public class GunPegboardItem extends ItemBlock {
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_COLOR = "color";
    private static final int DEFAULT_LEATHER_COLOR = 10511680;

    public GunPegboardItem(Block block) {
        super(block);
    }

    /**
     * {@code DyeableLeatherItem#hasCustomColor}
     */
    public boolean hasCustomColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound(TAG_DISPLAY);
        return display != null && display.hasKey(TAG_COLOR, 99);
    }

    /**
     * {@code DyeableLeatherItem#getColor}
     */
    public int getColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound(TAG_DISPLAY);
        return display != null && display.hasKey(TAG_COLOR, 99) ? display.getInteger(TAG_COLOR) : DEFAULT_LEATHER_COLOR;
    }

    /**
     * {@code DyeableLeatherItem#clearColor}
     */
    public void clearColor(ItemStack stack) {
        NBTTagCompound display = stack.getSubCompound(TAG_DISPLAY);
        if (display != null && display.hasKey(TAG_COLOR)) {
            display.removeTag(TAG_COLOR);
        }
    }

    /**
     * {@code DyeableLeatherItem#setColor}
     */
    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound(TAG_DISPLAY).setInteger(TAG_COLOR, color);
    }

    /**
     * {@code DyeableLeatherItem.dyeArmor}: mixes the dyes into the item's color, empty if the item is not dyeable.
     */
    public static ItemStack dyeArmor(ItemStack stack, List<EnumDyeColor> dyes) {
        if (!(stack.getItem() instanceof GunPegboardItem dyeable)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copy();
        result.setCount(1);
        int[] totals = new int[3];
        int brightness = 0;
        int count = 0;
        if (dyeable.hasCustomColor(stack)) {
            int color = dyeable.getColor(result);
            int r = color >> 16 & 255;
            int g = color >> 8 & 255;
            int b = color & 255;
            brightness += Math.max(r, Math.max(g, b));
            totals[0] += r;
            totals[1] += g;
            totals[2] += b;
            ++count;
        }
        for (EnumDyeColor dye : dyes) {
            float[] components = dye.getColorComponentValues();
            int r = (int) (components[0] * 255.0F);
            int g = (int) (components[1] * 255.0F);
            int b = (int) (components[2] * 255.0F);
            brightness += Math.max(r, Math.max(g, b));
            totals[0] += r;
            totals[1] += g;
            totals[2] += b;
            ++count;
        }
        if (count == 0) {
            return ItemStack.EMPTY;
        }
        int r = totals[0] / count;
        int g = totals[1] / count;
        int b = totals[2] / count;
        float averageBrightness = (float) brightness / (float) count;
        float maxComponent = (float) Math.max(r, Math.max(g, b));
        r = (int) ((float) r * averageBrightness / maxComponent);
        g = (int) ((float) g * averageBrightness / maxComponent);
        b = (int) ((float) b * averageBrightness / maxComponent);
        int mixed = (r << 8) + g;
        mixed = (mixed << 8) + b;
        dyeable.setColor(result, mixed);
        return result;
    }

    /**
     * Upstream {@code place}: an undyed pegboard placed against a dyed one (without sneaking) takes its color.
     */
    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        int color = hasCustomColor(stack) ? getColor(stack) : -1;
        boolean inheritedColor = false;
        BlockPos targetPos = pos.offset(side.getOpposite());
        boolean linking = !(!world.isAirBlock(pos) && world.getBlockState(pos).getBlock().isReplaceable(world, pos))
                && player != null && !player.isSneaking()
                && world.getBlockState(targetPos).getBlock() instanceof GunPegboardBlock;
        if (color < 0 && linking && world.getTileEntity(targetPos) instanceof GunPegboardBlockEntity target) {
            color = target.getColor();
            inheritedColor = color >= 0;
        }
        boolean result = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
        if (result && world.getTileEntity(pos) instanceof GunPegboardBlockEntity pegboard
                && world.getBlockState(pos).getBlock() == getBlock()) {
            pegboard.setColor(color, inheritedColor);
        }
        return result;
    }

    /**
     * Upstream's {@code CauldronInteraction.DYED_ITEM}: a water cauldron washes the color off.
     */
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        IBlockState state = world.getBlockState(pos);
        ItemStack stack = player.getHeldItem(hand);
        if (state.getBlock() != Blocks.CAULDRON || !hasCustomColor(stack)) {
            return EnumActionResult.PASS;
        }
        int level = state.getValue(BlockCauldron.LEVEL);
        if (level <= 0) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote) {
            clearColor(stack);
            player.addStat(StatList.ARMOR_CLEANED);
            Blocks.CAULDRON.setWaterLevel(world, pos, state, level - 1);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World level, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, level, tooltip, flag);
        tooltip.add(Components.translatable("tooltip.tacz.gun_pegboard.dye").withStyle(TextFormatting.GRAY).getFormattedText());
        tooltip.add(Components.translatable("tooltip.tacz.gun_pegboard.link_color").withStyle(TextFormatting.GRAY).getFormattedText());
    }
}
