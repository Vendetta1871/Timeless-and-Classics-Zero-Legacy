package com.tacz.guns.util.block;

import com.tacz.guns.config.common.AmmoConfig;
// TODO(port): needs ModBlocks
// import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.legacy.util.ClipContext;
import com.tacz.guns.legacy.util.LegacyMath;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BlockRayTrace {
    // TODO(port): needs ModBlocks (BULLET_IGNORE_BLOCKS block tag; 1.12.2 has no block tags)
    // private static final Predicate<IBlockState> IGNORES = input -> input != null &&
    //         input.is(ModBlocks.BULLET_IGNORE_BLOCKS);
    private static final Predicate<IBlockState> IGNORES = input -> false; // placeholder until ModBlocks is ported

    public static RayTraceResult rayTraceBlocks(World level, ClipContext context) {
        return performRayTrace(context, (rayTraceContext, blockPos) -> {
            IBlockState blockState = level.getBlockState(blockPos);
            // 这里添加判断方块是否可以穿透，如果可以穿透则返回 null
            List<String> ids = AmmoConfig.PASS_THROUGH_BLOCKS.get();
            ResourceLocation blockId = blockState.getBlock().getRegistryName();
            if (blockId != null && ids.contains(blockId.toString())) {
                return null;
            }
            // tag
            if (IGNORES.test(blockState)) {
                return null;
            }
            return getBlockHitResult(level, rayTraceContext, blockPos, blockState);
        }, (rayTraceContext) -> {
            Vec3d vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return new RayTraceResult(RayTraceResult.Type.MISS, rayTraceContext.getTo(), EnumFacing.getFacingFromVector((float) vec3.x, (float) vec3.y, (float) vec3.z), new BlockPos(rayTraceContext.getTo()));
        });
    }

    // 1.12.2 has no FluidState / VoxelShape: the fluid part of the upstream trace is dropped (upstream callers use Fluid.NONE)
    @Nullable
    private static RayTraceResult getBlockHitResult(World level, ClipContext rayTraceContext, BlockPos blockPos, IBlockState blockState) {
        Vec3d startVec = rayTraceContext.getFrom();
        Vec3d endVec = rayTraceContext.getTo();
        Block block = blockState.getBlock();
        // 与原版 World#rayTraceBlocks 一致：没有碰撞箱的方块直接忽略
        if (blockState.getCollisionBoundingBox(level, blockPos) == Block.NULL_AABB) {
            return null;
        }
        if (!block.canCollideCheck(blockState, false)) {
            return null;
        }
        return blockState.collisionRayTrace(level, blockPos, startVec, endVec);
    }

    private static <T> T performRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> missFactory) {
        Vec3d startVec = context.getFrom();
        Vec3d endVec = context.getTo();
        if (!startVec.equals(endVec)) {
            double startX = LegacyMath.lerp(-0.0000001, endVec.x, startVec.x);
            double startY = LegacyMath.lerp(-0.0000001, endVec.y, startVec.y);
            double startZ = LegacyMath.lerp(-0.0000001, endVec.z, startVec.z);
            double endX = LegacyMath.lerp(-0.0000001, startVec.x, endVec.x);
            double endY = LegacyMath.lerp(-0.0000001, startVec.y, endVec.y);
            double endZ = LegacyMath.lerp(-0.0000001, startVec.z, endVec.z);

            int blockX = MathHelper.floor(endX);
            int blockY = MathHelper.floor(endY);
            int blockZ = MathHelper.floor(endZ);

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(blockX, blockY, blockZ);
            T t = hitFunction.apply(context, mutablePos);
            if (t != null) {
                return t;
            }

            double deltaX = startX - endX;
            double deltaY = startY - endY;
            double deltaZ = startZ - endZ;
            int signX = LegacyMath.sign(deltaX);
            int signY = LegacyMath.sign(deltaY);
            int signZ = LegacyMath.sign(deltaZ);
            double d9 = signX == 0 ? Double.MAX_VALUE : (double) signX / deltaX;
            double d10 = signY == 0 ? Double.MAX_VALUE : (double) signY / deltaY;
            double d11 = signZ == 0 ? Double.MAX_VALUE : (double) signZ / deltaZ;
            double d12 = d9 * (signX > 0 ? 1.0D - MathHelper.frac(endX) : MathHelper.frac(endX));
            double d13 = d10 * (signY > 0 ? 1.0D - MathHelper.frac(endY) : MathHelper.frac(endY));
            double d14 = d11 * (signZ > 0 ? 1.0D - MathHelper.frac(endZ) : MathHelper.frac(endZ));

            while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
                if (d12 < d13) {
                    if (d12 < d14) {
                        blockX += signX;
                        d12 += d9;
                    } else {
                        blockZ += signZ;
                        d14 += d11;
                    }
                } else if (d13 < d14) {
                    blockY += signY;
                    d13 += d10;
                } else {
                    blockZ += signZ;
                    d14 += d11;
                }

                T t1 = hitFunction.apply(context, mutablePos.setPos(blockX, blockY, blockZ));
                if (t1 != null) {
                    return t1;
                }
            }
        }
        return missFactory.apply(context);
    }
}
