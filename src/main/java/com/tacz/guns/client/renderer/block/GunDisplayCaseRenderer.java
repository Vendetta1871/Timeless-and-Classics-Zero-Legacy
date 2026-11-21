package com.tacz.guns.client.renderer.block;

import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.client.renderer.item.ItemPreviewRenderContext;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.RenderSystem;
import com.tacz.guns.legacy.util.BlockEntityRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * TODO(port): upstream first tries {@code DisplayGunMeshGroup.tryRender} (SimpleBedrockModel v2 VBO mesh groups shared
 *  by all display slots, with LOD); SBM v2 is not ported, every slot is drawn through the item renderer
 *  ({@code ItemRenderer.renderStatic} with {@code FIXED}, upstream's fallback path).
 */
public class GunDisplayCaseRenderer extends BlockEntityRenderer<GunDisplayCaseBlockEntity> {
    private static final double ITEM_OFFSET = 0.4375D - 3.0D / 16.0D;
    private static final float PREVIEW_ALPHA = 0.65F;

    public GunDisplayCaseRenderer() {
    }

    @Override
    public void render(GunDisplayCaseBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        IBlockState state = blockEntity.getWorld().getBlockState(blockEntity.getPos());
        if (!(state.getBlock() instanceof GunDisplayCaseBlock)) {
            return;
        }
        EnumFacing facing = state.getValue(GunDisplayCaseBlock.FACING);
        for (int slot = 0; slot < GunDisplayCaseBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int row = slot / GunDisplayCaseBlockEntity.GRID_SIZE;
            int col = slot % GunDisplayCaseBlockEntity.GRID_SIZE;
            double x = (col + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
            double y = (row + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
            renderCaseItem(stack, blockEntity.getWorld(), facing, x, y, blockEntity.getRotation(slot), poseStack, buffer, displayItemLight(), packedOverlay, 1.0F);
        }
        renderPlacementPreview(blockEntity, state, poseStack, buffer, packedLight, packedOverlay);
    }

    private static void renderPlacementPreview(GunDisplayCaseBlockEntity blockEntity, IBlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (state.getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        World level = blockEntity.getWorld();
        RayTraceResult hit = minecraft.objectMouseOver;
        if (player == null || level == null || player.isSneaking() || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockEntity.getPos();
        if (!hit.getBlockPos().equals(pos)) {
            return;
        }
        ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
        if (held.isEmpty() || !GunPegboardItemConfigRead.canPlace(held)) {
            return;
        }
        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, hit.hitVec);
        if (slot < 0 || !blockEntity.getItem(slot).isEmpty()) {
            return;
        }
        int row = slot / GunDisplayCaseBlockEntity.GRID_SIZE;
        int col = slot % GunDisplayCaseBlockEntity.GRID_SIZE;
        double x = (col + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
        double y = (row + 0.5D) / GunDisplayCaseBlockEntity.GRID_SIZE - 0.5D;
        EnumFacing facing = state.getValue(GunDisplayCaseBlock.FACING);
        ItemStack preview = held.copy();
        preview.setCount(1);
        renderCaseItem(preview, level, facing, x, y, 0, poseStack, buffer, displayItemLight(), packedOverlay, PREVIEW_ALPHA);
    }

    private static void renderCaseItem(ItemStack stack, World level, EnumFacing facing, double x, double y, int rotation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float alpha) {
        poseStack.pushPose();
        DisplayItemTransforms.apply(poseStack, facing, x, y, ITEM_OFFSET, rotation);
        if (alpha < 1.0F) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            ItemPreviewRenderContext.setAlpha(alpha);
        }
        GunPegboardRenderer.renderStatic(stack, poseStack, buffer, packedLight);
        if (alpha < 1.0F) {
            ItemPreviewRenderContext.setAlpha(1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    private static int displayItemLight() {
        return LightTexture.pack(15, 15);
    }

    // 1.12.2: the view distance is GunDisplayCaseBlockEntity#getMaxRenderDistanceSquared
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(GunDisplayCaseBlockEntity blockEntity) {
        return true;
    }
}
