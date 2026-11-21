package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class GunPegboardInteractionEvent {
    /**
     * 1.12.2 has no {@code LeftClickBlock.Action.START}, see {@link GunDisplayCaseInteractionEvent}.
     */
    private static final Map<UUID, Long> LAST_REMOVAL = new HashMap<>();

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // TODO(port): upstream ignores the click with Items.DEBUG_STICK, which 1.12.2 does not have
        World level = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunPegboardBlock)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        // 1.12.2: Entity#pick is client only; the ray trace hits the plates of corner pegboards one by one
        RayTraceResult hit = ForgeHooks.rayTraceEyes(player, 6.0D);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) {
            return;
        }
        state = state.getActualState(level, pos);
        int slot = GunPegboardSlotHelper.getSlot(state, pos, hit.hitVec);
        if (slot < 0) {
            return;
        }
        TileEntity blockEntity = level.getTileEntity(pos);
        if (!(blockEntity instanceof GunPegboardBlockEntity pegboard) || pegboard.getItem(slot).isEmpty()) {
            return;
        }
        event.setCanceled(true);
        // 1.12.2: the client ignores the cancellation in survival, denying the item use stops it from digging
        event.setUseItem(Event.Result.DENY);
        event.setUseBlock(Event.Result.DENY);
        boolean shouldDrop = !player.isCreative() || player.isSneaking();
        if (!level.isRemote && GunDisplayCaseInteractionEvent.DisplayInteractionCooldown.tryRemove(LAST_REMOVAL, player)
                && (shouldDrop ? pegboard.dropItem(slot) : pegboard.removeItem(slot))) {
            level.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}
