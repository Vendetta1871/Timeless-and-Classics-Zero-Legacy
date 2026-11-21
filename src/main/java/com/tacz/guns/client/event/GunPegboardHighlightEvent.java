package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 1.12.2: {@code RenderHighlightEvent.Block} is {@link DrawBlockHighlightEvent}, see
 * {@link GunDisplayCaseHighlightEvent#drawLineBox}.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class GunPegboardHighlightEvent {
    @SubscribeEvent
    public static void onRenderBlockHighlight(DrawBlockHighlightEvent event) {
        RayTraceResult target = event.getTarget();
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = target.getBlockPos();
        EntityPlayer cameraEntity = event.getPlayer();
        World level = cameraEntity.world;
        IBlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunPegboardBlock)) {
            return;
        }
        // 1.12.2: SHAPE is only in the actual state
        state = state.getActualState(level, pos);
        TileEntity blockEntity = level.getTileEntity(pos);
        GunPegboardBlockEntity pegboard = blockEntity instanceof GunPegboardBlockEntity value ? value : null;
        if (cameraEntity.isSneaking()) {
            renderOccupiedSlots(event, state, pos, pegboard);
            return;
        }
        renderAimedSlot(event, state, pos, target, pegboard);
    }

    private static void renderAimedSlot(DrawBlockHighlightEvent event, IBlockState state, BlockPos pos, RayTraceResult target, GunPegboardBlockEntity pegboard) {
        int slot = GunPegboardSlotHelper.getSlot(state, pos, target.hitVec);
        if (slot < 0) {
            return;
        }
        boolean occupied = pegboard != null && !pegboard.getItem(slot).isEmpty();
        renderSlotBox(event, state, pos, slot, occupied ? 1.0F : 1.0F, occupied ? 0.0F : 1.0F, 0.0F);
    }

    private static void renderOccupiedSlots(DrawBlockHighlightEvent event, IBlockState state, BlockPos pos, GunPegboardBlockEntity pegboard) {
        if (pegboard == null) {
            return;
        }
        for (int slot = 0; slot < GunPegboardBlockEntity.SLOT_COUNT; slot++) {
            if (!pegboard.getItem(slot).isEmpty()) {
                renderSlotBox(event, state, pos, slot, 1.0F, 0.0F, 0.0F);
            }
        }
    }

    private static void renderSlotBox(DrawBlockHighlightEvent event, IBlockState state, BlockPos pos, int slot, float red, float green, float blue) {
        AxisAlignedBB aabb = GunPegboardSlotHelper.getSlotAabb(state, pos, slot);
        GunDisplayCaseHighlightEvent.drawLineBox(event, aabb, red, green, blue);
    }
}
