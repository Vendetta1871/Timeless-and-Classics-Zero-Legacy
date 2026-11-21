package com.tacz.guns.legacy.fp;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.tacz.guns.legacy.render.MultiBufferSource;
import com.tacz.guns.legacy.render.PoseStack;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

/**
 * Port of SimpleBedrockModel's (2.6.3) {@code v1.client.renderer.IFPGeoItemRenderer} (the library TACZ 1.20.1
 * bundles): an item renderer that draws the item itself in first person, replacing vanilla hand rendering.
 */
@SideOnly(Side.CLIENT)
public interface IFPGeoItemRenderer {
    default boolean isSameItem(ItemStack oldStack, ItemStack newStack) {
        return ItemStack.areItemsEqual(oldStack, newStack);
    }

    @Nullable
    default IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity) {
        return null;
    }

    @Nullable
    default IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity, EnumHand hand) {
        return createAnimationInstance(stack, entity);
    }

    default long getPutAwayDuration(ItemStack stack) {
        return 0;
    }

    /**
     * 当该物品被持于主手时，是否禁止副手的第一人称渲染。
     * 用于双手长枪等会霸占整个视野的物品：主手持有时副手不渲染。
     * 注意这是主手视角的判定，与副手物品自身无关。
     *
     * @return 持于主手时是否禁止副手渲染
     */
    default boolean blockOffhandRender() {
        return false;
    }

    default boolean blockOffhandRender(ItemStack stack) {
        return blockOffhandRender();
    }

    /**
     * 该物品当前是否允许在指定手进行第一人称渲染。
     * <p>
     * 用于「物品本身决定能否在某只手呈现」的场景：例如双手长枪放入副手时不应在副手渲染。
     * 与 {@link #blockOffhandRender(ItemStack)} 区分——后者是「主手物品霸占视野从而禁止副手」的
     * 主手视角判定；本方法是被渲染物品对自身所在手的判定。
     *
     * @param stack 待渲染物品
     * @param hand  渲染所在手
     * @return 允许渲染返回 {@code true}（主手默认），否则 {@code false}
     */
    default boolean canRenderInHand(ItemStack stack, EnumHand hand) {
        return hand == EnumHand.MAIN_HAND;
    }

    /**
     * 渲染变体键：标识「同一物品」当前应使用哪一套渲染变体（模型骨架 / 动画 / 控制器）。
     * <p>
     * 默认返回 {@code null}（无变体，仅靠物品变化驱动切换）。返回值需可用 {@code equals} 比较，
     * 且对同一形态稳定。
     *
     * @param stack 当前物品
     * @param hand  所在手
     * @return 当前变体键；{@code null} 表示无变体
     */
    @Nullable
    default Object getRenderVariantKey(ItemStack stack, EnumHand hand) {
        return null;
    }

    /**
     * 使用该渲染器的物品会阻止原版的 viewBobbing，以便应用自定义的跑步/走路动画。
     *
     * @return 是否阻止原版 viewBobbing
     */
    default boolean blockViewBobbing() {
        return true;
    }

    /**
     * 应用摄像机动画对世界的变换（只有旋转生效）。默认什么都不做。
     * <p>
     * 1.12.2: {@code ViewportEvent.ComputeCameraAngles} is {@link EntityViewRenderEvent.CameraSetup}.
     */
    default void applyLevelCameraAnimation(EntityViewRenderEvent.CameraSetup event, ItemStack stack, Quaternionf animateRot, float partialTicks) {
    }

    /**
     * 应用摄像机动画对手持物品的变换（只有旋转生效）。默认什么都不做。
     */
    default void applyItemInHandCameraAnimation(PoseStack poseStack, ItemStack stack, Quaternionf animateRot, float partialTicks) {
    }

    /**
     * 更新粒子发射器的变换矩阵。由 {@code FirstPersonRenderHandler} 在渲染物品前调用。
     * 具体渲染器应当覆写此方法，为其注册的发射器设置 emitterTransform 和 worldTransform。
     * <p>
     * 默认空实现（无粒子效果的物品无需实现）。
     *
     * @param system    全局粒子系统实例
     * @param poseStack 当前渲染使用的 PoseStack
     * @param hand      当前渲染的手（主手 / 副手），用于按手隔离发射器绑定状态
     */
    default void updateParticleEmitterTransforms(FirstPersonParticleSystem system, PoseStack poseStack, EnumHand hand) {
        // 默认空实现，由有粒子效果的渲染器覆写
    }

    /**
     * 该渲染器是否在自身的模型渲染窗口内（模板测试仍开启时）自行渲染粒子。
     * <p>
     * 需要让第一人称粒子被瞄具的模板缓冲剔除（如枪口火光/烟雾不穿透镜片）时覆写并返回
     * {@code true}：渲染器在 {@link #renderFirstPerson} 的模型渲染窗口内调用
     * {@link FirstPersonRenderHandler#renderParticlesNow} 提交粒子并 flush；
     * 返回 {@code true} 后 {@code FirstPersonRenderHandler} 将跳过事件末尾的自动粒子渲染，
     * 避免双重绘制。
     * <p>
     * 默认返回 {@code false}（由 SBM 在事件末尾统一渲染）。
     */
    default boolean renderParticlesInModelWindow() {
        return false;
    }

    void renderFirstPerson(EntityPlayerSP player, ItemStack stack, ItemCameraTransforms.TransformType ctx, PoseStack poseStack,
                           MultiBufferSource bufferSource, int light, float partialTick);
}
