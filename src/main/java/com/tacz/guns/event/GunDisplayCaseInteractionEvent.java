package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class GunDisplayCaseInteractionEvent {
    /**
     * 1.12.2 has no {@code LeftClickBlock.Action.START}: a survival player holding the attack button sends a new start
     * of digging (and fires the event again) every tick, so one item is taken per {@link DisplayInteractionCooldown}.
     */
    private static final Map<UUID, Long> LAST_REMOVAL = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking() || player.getHeldItemMainhand().getItem() instanceof IGun) {
            return;
        }
        if (!(player.getHeldItemOffhand().getItem() instanceof IGun)) {
            return;
        }
        World level = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunDisplayCaseBlock displayCaseBlock)) {
            return;
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        displayCaseBlock.toggleLockFromInteraction(level, pos, state);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        World level = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        // 1.12.2: Entity#pick is client only
        RayTraceResult hit = ForgeHooks.rayTraceEyes(player, 6.0D);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) {
            return;
        }
        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, hit.hitVec);
        if (slot < 0) {
            return;
        }
        TileEntity blockEntity = level.getTileEntity(pos);
        if (!(blockEntity instanceof GunDisplayCaseBlockEntity displayCase) || displayCase.getItem(slot).isEmpty()) {
            return;
        }
        event.setCanceled(true);
        // 1.12.2: the client ignores the cancellation in survival, denying the item use stops it from digging
        event.setUseItem(Event.Result.DENY);
        event.setUseBlock(Event.Result.DENY);
        boolean shouldDrop = !player.isCreative() || player.isSneaking();
        if (!level.isRemote && DisplayInteractionCooldown.tryRemove(LAST_REMOVAL, player)
                && (shouldDrop ? displayCase.dropItem(slot) : displayCase.removeItem(slot))) {
            level.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * 1.12.2 stand-in for {@code LeftClickBlock.Action.START}, shared with {@link GunPegboardInteractionEvent}.
     */
    static final class DisplayInteractionCooldown {
        /**
         * The creative digging repeat ({@code PlayerControllerMP#blockHitDelay}), which 1.20.1 has too.
         */
        private static final long TICKS = 5;

        private DisplayInteractionCooldown() {
        }

        static boolean tryRemove(Map<UUID, Long> lastRemoval, EntityPlayer player) {
            long now = player.world.getTotalWorldTime();
            Long last = lastRemoval.get(player.getUniqueID());
            if (last != null && now >= last && now - last < TICKS) {
                return false;
            }
            lastRemoval.put(player.getUniqueID(), now);
            return true;
        }
    }
}
