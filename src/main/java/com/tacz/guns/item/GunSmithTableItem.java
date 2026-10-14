package com.tacz.guns.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.BlockItemBuilder;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.client.renderer.item.GunSmithTableItemRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

public class GunSmithTableItem extends ItemBlock implements BlockItemDataAccessor {
    public GunSmithTableItem(Block block) {
        super(block);
        this.setMaxStackSize(1);
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.initializeClient();
        }
    }

    /**
     * 1.12.2: {@code IClientItemExtensions#getCustomRenderer}
     */
    @SideOnly(Side.CLIENT)
    private void initializeClient() {
        this.setTileEntityItemStackRenderer(new GunSmithTableItemRenderer());
    }

    /**
     * 1.12.2: upstream's {@code getStateForPlacement} returns {@code null} (no placement) when the second half of a
     * two-block table has no room.
     */
    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        if (newState.getBlock() instanceof AbstractGunSmithTableBlock table && !table.canPlaceAt(world, pos, newState)) {
            return false;
        }
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }

    public static NonNullList<ItemStack> fillItemCategory() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonBlockIndex().forEach((blockIndex) -> {
            ItemStack stack = BlockItemBuilder.create(blockIndex.getValue().getBlock()).setId(blockIndex.getKey()).build();
            stacks.add(stack);
        });
        return stacks;
    }

    @Override
    @Nonnull
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        ResourceLocation blockId = this.getBlockId(stack);
        Optional<ClientBlockIndex> blockIndex = TimelessAPI.getClientBlockIndex(blockId);
        if (blockIndex.isPresent()) {
            return I18n.format(blockIndex.get().getName());
        }
        return super.getItemStackDisplayName(stack);
    }

//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
//        ResourceLocation blockId = this.getBlockId(stack);
//        TimelessAPI.getClientBlockIndex(blockId).ifPresent(index -> {
//            String tooltipKey = index.getTooltipKey();
//            if (tooltipKey != null) {
//                components.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
//            }
//        });
//
//        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(blockId);
//        if (packInfoObject != null) {
//            MutableComponent component = Component.translatable(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
//            components.add(component);
//        }
//    }

    // TODO(port): 1.12.2 has no TooltipComponent / Item#getTooltipImage; BlockItemTooltip is a plain class here and needs a tooltip event hook
    @NotNull
    public Optional<BlockItemTooltip> getTooltipImage(ItemStack pStack) {
        return Optional.of(new BlockItemTooltip(this.getBlockId(pStack)));
    }
}
