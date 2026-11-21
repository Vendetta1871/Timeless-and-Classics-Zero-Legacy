package com.tacz.guns.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.legacy.advancement.ItemUsedOnBlockTrigger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.12.2 has no {@code minecraft:item_used_on_block}: as 1.20.1's {@code ServerPlayerGameMode#useItemOn}, a block
 * that accepted the right click, or an item used on a block with success, fires {@link ItemUsedOnBlockTrigger} with
 * the stack as it was before. The calls themselves are watched, not the method's result: 1.12.2 returns PASS after a
 * block took the last item of the stack (a gun put into a display case).
 */
@Mixin(PlayerInteractionManager.class)
public class PlayerInteractionManagerMixin {
    @Unique
    private ItemStack tacz$usedStack = ItemStack.EMPTY;
    @Unique
    private EntityPlayer tacz$player;
    @Unique
    private BlockPos tacz$pos;

    @Inject(method = "processRightClickBlock", at = @At("HEAD"))
    private void tacz$rememberUse(EntityPlayer player, World world, ItemStack stack, EnumHand hand, BlockPos pos,
                                  EnumFacing facing, float hitX, float hitY, float hitZ,
                                  CallbackInfoReturnable<EnumActionResult> cir) {
        tacz$usedStack = stack.copy();
        tacz$player = player;
        tacz$pos = pos;
    }

    @ModifyExpressionValue(method = "processRightClickBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;onBlockActivated(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)Z"))
    private boolean tacz$blockUsed(boolean used) {
        if (used) {
            tacz$trigger();
        }
        return used;
    }

    @ModifyExpressionValue(method = "processRightClickBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;onItemUse(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)Lnet/minecraft/util/EnumActionResult;"))
    private EnumActionResult tacz$itemUsed(EnumActionResult result) {
        if (result == EnumActionResult.SUCCESS) {
            tacz$trigger();
        }
        return result;
    }

    @Unique
    private void tacz$trigger() {
        if (tacz$player instanceof EntityPlayerMP player && tacz$pos != null) {
            ItemUsedOnBlockTrigger.INSTANCE.trigger(player, tacz$pos, tacz$usedStack);
        }
    }

    @Inject(method = "processRightClickBlock", at = @At("RETURN"))
    private void tacz$forgetUse(CallbackInfoReturnable<EnumActionResult> cir) {
        tacz$usedStack = ItemStack.EMPTY;
        tacz$player = null;
        tacz$pos = null;
    }
}
