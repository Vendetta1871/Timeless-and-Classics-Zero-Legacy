package com.tacz.guns.client.renderer.block;

import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.client.renderer.item.DisplayItemRenderContext;
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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
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
public class GunPegboardRenderer extends BlockEntityRenderer<GunPegboardBlockEntity> {
    private static final double ITEM_FRAME_ITEM_OFFSET = 0.4375D - 3.0D / 16.0D;
    private static final float PREVIEW_ALPHA = 0.65F;

    public GunPegboardRenderer() {
    }

    @Override
    public void render(GunPegboardBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        IBlockState state = blockEntity.getBlockState();
        for (int slot = 0; slot < GunPegboardBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            EnumFacing facing = GunPegboardSlotHelper.getSlotFacing(state, slot);
            int row = (slot % GunPegboardBlockEntity.SLOTS_PER_FACE) / GunPegboardBlockEntity.GRID_SIZE;
            int col = slot % GunPegboardBlockEntity.GRID_SIZE;
            double x = (col + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
            double y = (row + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;

            int itemLight = isGlowing(blockEntity) ? LightTexture.pack(15, 15) : packedLight;
            renderPegboardItem(stack, blockEntity.getWorld(), facing, x, y, blockEntity.getRotation(slot), poseStack, buffer, itemLight, packedOverlay, 1.0F);
        }
        renderPlacementPreview(blockEntity, poseStack, buffer, packedLight, packedOverlay);
    }

    private static void renderPlacementPreview(GunPegboardBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
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
        IBlockState state = blockEntity.getBlockState();
        int slot = GunPegboardSlotHelper.getSlot(state, pos, hit.hitVec);
        if (slot < 0 || !blockEntity.getItem(slot).isEmpty()) {
            return;
        }
        int row = (slot % GunPegboardBlockEntity.SLOTS_PER_FACE) / GunPegboardBlockEntity.GRID_SIZE;
        int col = slot % GunPegboardBlockEntity.GRID_SIZE;
        double x = (col + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
        double y = (row + 0.5D) / GunPegboardBlockEntity.GRID_SIZE - 0.5D;
        EnumFacing facing = GunPegboardSlotHelper.getSlotFacing(state, slot);
        int itemLight = isGlowing(blockEntity) ? LightTexture.pack(15, 15) : packedLight;
        ItemStack preview = held.copy();
        preview.setCount(1);
        renderPegboardItem(preview, level, facing, x, y, 0, poseStack, buffer, itemLight, packedOverlay, PREVIEW_ALPHA);
    }

    private static void renderPegboardItem(ItemStack stack, World level, EnumFacing facing, double x, double y, int rotation, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, float alpha) {
        poseStack.pushPose();
        DisplayItemTransforms.apply(poseStack, facing, x, y, ITEM_FRAME_ITEM_OFFSET, rotation);
        if (alpha < 1.0F) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            ItemPreviewRenderContext.setAlpha(alpha);
        }
        renderStatic(stack, poseStack, buffer, packedLight);
        if (alpha < 1.0F) {
            ItemPreviewRenderContext.setAlpha(1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    /**
     * {@code ItemRenderer.renderStatic(stack, FIXED, packedLight, ...)}: 1.12.2 draws items through GL with the pose
     * stack's matrix (as {@code StatueRenderer} does) and the light from the lightmap coordinates.
     */
    static void renderStatic(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // draw what is batched first
        if (buffer instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, packedLight & 0xFFFF, packedLight >> 16 & 0xFFFF);
        GlStateManager.pushMatrix();
        RenderSystem.multMatrix(poseStack.last().pose());
        DisplayItemRenderContext.render(() -> Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED));
        GlStateManager.popMatrix();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
    }

    private static boolean isGlowing(GunPegboardBlockEntity blockEntity) {
        return blockEntity.getBlockState().getBlock() instanceof GunPegboardBlock pegboard && pegboard.isGlowing();
    }

    // 1.12.2: the view distance is GunPegboardBlockEntity#getMaxRenderDistanceSquared
    public int getViewDistance() {
        return RenderConfig.TARGET_RENDER_DISTANCE.get();
    }

    @Override
    public boolean shouldRenderOffScreen(GunPegboardBlockEntity blockEntity) {
        return true;
    }
}
