package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
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
 * 1.12.2: {@code RenderHighlightEvent.Block} is {@link DrawBlockHighlightEvent}; the slot boxes are drawn with
 * {@link RenderGlobal#drawSelectionBoundingBox} in the state of upstream's {@code tacz_display_case_highlight} render
 * type (lines, no depth test, no depth write).
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class GunDisplayCaseHighlightEvent {
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
        if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        TileEntity blockEntity = level.getTileEntity(pos);
        GunDisplayCaseBlockEntity displayCase = blockEntity instanceof GunDisplayCaseBlockEntity value ? value : null;
        if (cameraEntity.isSneaking()) {
            renderOccupiedSlots(event, state, pos, displayCase);
            return;
        }
        renderAimedSlot(event, state, pos, target, displayCase);
    }

    private static void renderAimedSlot(DrawBlockHighlightEvent event, IBlockState state, BlockPos pos, RayTraceResult target, GunDisplayCaseBlockEntity displayCase) {
        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, target.hitVec);
        if (slot < 0) {
            return;
        }
        boolean occupied = displayCase != null && !displayCase.getItem(slot).isEmpty();
        renderSlotBox(event, state, pos, slot, occupied ? 1.0F : 1.0F, occupied ? 0.0F : 1.0F, 0.0F);
    }

    private static void renderOccupiedSlots(DrawBlockHighlightEvent event, IBlockState state, BlockPos pos, GunDisplayCaseBlockEntity displayCase) {
        if (displayCase == null) {
            return;
        }
        for (int slot = 0; slot < GunDisplayCaseBlockEntity.SLOT_COUNT; slot++) {
            if (!displayCase.getItem(slot).isEmpty()) {
                renderSlotBox(event, state, pos, slot, 1.0F, 0.0F, 0.0F);
            }
        }
    }

    private static void renderSlotBox(DrawBlockHighlightEvent event, IBlockState state, BlockPos pos, int slot, float red, float green, float blue) {
        AxisAlignedBB aabb = GunDisplayCaseSlotHelper.getSlotAabb(state, pos, slot);
        drawLineBox(event, aabb, red, green, blue);
    }

    /**
     * {@code LevelRenderer.renderLineBox} relative to the camera, through the (no depth) highlight line state.
     */
    static void drawLineBox(DrawBlockHighlightEvent event, AxisAlignedBB aabb, float red, float green, float blue) {
        EntityPlayer player = event.getPlayer();
        float partialTicks = event.getPartialTicks();
        double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        RenderGlobal.drawSelectionBoundingBox(aabb.offset(-cameraX, -cameraY, -cameraZ), red, green, blue, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
