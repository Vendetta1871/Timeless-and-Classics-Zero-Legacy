package com.tacz.guns.legacy.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

/**
 * Wraps a built-in-renderer item model to record the display context of the item being rendered. 1.12.2 item
 * renderers ({@code TileEntityItemStackRenderer}) receive no context, but every vanilla item render path calls
 * {@link #handlePerspective} right before invoking the renderer; {@link BlockEntityWithoutLevelRenderer} reads it.
 */
@SideOnly(Side.CLIENT)
public class PerspectiveCaptureModel implements IBakedModel {
    private static ItemCameraTransforms.TransformType current = ItemCameraTransforms.TransformType.NONE;

    private final IBakedModel base;

    public PerspectiveCaptureModel(IBakedModel base) {
        this.base = base;
    }

    public static ItemCameraTransforms.TransformType current() {
        return current;
    }

    public static void wrap(ModelBakeEvent event, ModelResourceLocation location) {
        IBakedModel model = event.getModelRegistry().getObject(location);
        if (model != null && !(model instanceof PerspectiveCaptureModel)) {
            event.getModelRegistry().putObject(location, new PerspectiveCaptureModel(model));
        }
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        current = cameraTransformType;
        Pair<? extends IBakedModel, Matrix4f> pair = base.handlePerspective(cameraTransformType);
        return Pair.of(this, pair.getRight());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return base.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return base.getParticleTexture();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return base.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return base.getOverrides();
    }
}
