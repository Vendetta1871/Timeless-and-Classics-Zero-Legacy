package com.tacz.guns.client.model.functional;

import com.tacz.guns.legacy.render.PoseStack;
import com.tacz.guns.legacy.render.VertexConsumer;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.compat.ar.ARCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.LaserColorUtil;
import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.render.LightTexture;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.RenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import com.tacz.guns.legacy.render.DisplayContext;
import com.tacz.guns.legacy.render.RenderBuffers;

public class BeamRenderer  {
    public static final ResourceLocation LASER_BEAM_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/entity/beam.png");
    private static final LaserConfig DEFAULT_LASER_CONFIG = new LaserConfig();

    public static void renderLaserBeam(ItemStack stack, PoseStack poseStack, ItemCameraTransforms.TransformType transformType, @Nonnull List<BedrockPart> path) {
        if (stack == null || !DisplayContext.firstPerson(transformType) && !(transformType == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }

		if (ARCompat.shouldAccelerate() && renderLaserBeamAccelerated(stack, poseStack, transformType, path)) {
			return;
		}

        MultiBufferSource.BufferSource bufferSource = RenderBuffers.bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(LaserBeamRenderState.getLaserBeam());
        poseStack.pushPose();
        {
            for (int i = 0; i < path.size(); ++i) {
                path.get(i).translateAndRotateAndScale(poseStack);
            }

            LaserConfig laserConfig = getLaserConfig(stack);

            int color = LaserColorUtil.getLaserColor(stack, laserConfig);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            stringVertex(DisplayContext.firstPerson(transformType) ? -laserConfig.getLength() : -laserConfig.getLengthThird(),
                    DisplayContext.firstPerson(transformType) ? laserConfig.getWidth() : laserConfig.getWidthThird(),
                    builder, poseStack.last(), r, g, b, RenderConfig.ENABLE_LASER_FADE_OUT.get());
        }
        poseStack.popPose();
    }

	public static boolean renderLaserBeamAccelerated(ItemStack stack, PoseStack poseStack, ItemCameraTransforms.TransformType transformType, @Nonnull List<BedrockPart> path) {
		// 获取VertexConsumer拓展方法
		var builder = RenderBuffers.bufferSource().getBuffer(LaserBeamRenderState.getLaserBeamEntity()); //替换为LASER_BEAM_ENTITY确保镭射和枪体在同一个format的同一层

		// 加速渲染的实现机制是每个VertexFormat一个bufferSource, 每个bufferSource维护一个Map<层编号, 层>
		// 因此如果镭射和枪体不在一个format, 则可能出现镭射无法正确被模板缓冲剔除掉
		// 因为枪体渲染完成之后已经擦除了模板缓冲区, 再到下一个format镭射开始渲染时就读取不到模板缓冲了

		// 如果不支持则回退
		if (!ARCompat.isAccelerated(builder)) {
			return false;
		}

		poseStack.pushPose();
		{
			for (int i = 0; i < path.size(); ++i) {
				path.get(i).translateAndRotateAndScale(poseStack);
			}

			var laserConfig = getLaserConfig(stack);

			// 这里直接调用及其容易触发类加载(编译期常量?), 所以封装到ARCompat中
			ARCompat.renderLaser(
					builder,
					DisplayContext.firstPerson(transformType) ? -laserConfig.getLength() : -laserConfig.getLengthThird(), //z
					DisplayContext.firstPerson(transformType) ? laserConfig.getWidth() : laserConfig.getWidthThird(), //width
					RenderConfig.ENABLE_LASER_FADE_OUT.get(), //fadeOut
					poseStack,
					//不透明的镭射颜色
					(255 << 24) | (LaserColorUtil.getLaserColor(stack, laserConfig) & 0xFF_FF_FF)
			);
		}
		poseStack.popPose();

		// 渲染完成, 阻止后续原始方法的渲染
		return true;
	}

    private static LaserConfig getLaserConfig(ItemStack stack) {
        if (stack == null) {
            return DEFAULT_LASER_CONFIG;
        }

        if (stack.getItem() instanceof IAttachment iAttachment) {
            return TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(stack))
                    .map(ClientAttachmentIndex::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        if (stack.getItem() instanceof IGun) {
            return TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        return DEFAULT_LASER_CONFIG;
    }

    private static void stringVertex(float z, float width, VertexConsumer pConsumer, PoseStack.Pose pPose, int r, int g, int b, boolean fadeOut) {
        float halfWidth = width / 2;
        int endAlpha = fadeOut ? 0 : 255;
        int light = LightTexture.pack(15, 15);
    	pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 0).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();

        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 0).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();

        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 0).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();

        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();
    }

    /**
     * Upstream builds these from {@code RenderStateShard}s (lightning blending, lightmap, no cull); here they are
     * shim render types with the same blending.
     */
    public static class LaserBeamRenderState {
        protected static final RenderType LASER_BEAM = RenderType.create("laser_beam", LASER_BEAM_TEXTURE, RenderType.Transparency.LIGHTNING, false, true);

        protected static final RenderType LASER_BEAM_ENTITY = RenderType.create("laser_beam_entity", LASER_BEAM_TEXTURE, RenderType.Transparency.LIGHTNING, false, true);

        public static RenderType getLaserBeam() {
            return LASER_BEAM;
        }

		public static RenderType getLaserBeamEntity() {
			return LASER_BEAM_ENTITY;
		}
	}
}
