package com.tacz.guns.client.particle;

import com.tacz.guns.client.init.ParticleFactoryRegistry;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class HitBlockEffectHandler {
    private static final int MAX_SAME_STEP_SOUND_PER_TICK = 1;
    private static final double MAX_HIT_STEP_SOUND_DISTANCE_SQR = 64.0D * 64.0D;
    private static final Map<SoundEvent, SoundLimit> STEP_SOUND_LIMITS = new HashMap<>();

    private HitBlockEffectHandler() {
    }

    public static void handle(WorldClient level, Vec3d hitPos, EnumFacing direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        IBlockState state = level.getBlockState(blockPos);
        if (state.getBlock() == ModBlocks.TARGET.get() || shouldRemove(level, blockPos, hitPos)) {
            return;
        }
        HitSurfaceParticleSpawner.spawn(level, hitPos, direction, blockPos, ammoId, gunId, gunDisplayId, incendiary);
        playHitBlockStepSound(level, state, hitPos.x, hitPos.y, hitPos.z);
        if (BulletHoleParticle.isBulletHoleEnabled(ammoId, gunId, gunDisplayId)) {
            // 1.12.2: no particle options/types; the provider creates the particle directly (always shown, as upstream's force flag)
            BulletHoleOption option = new BulletHoleOption(direction, blockPos, ammoId, gunId, gunDisplayId, incendiary);
            Minecraft.getMinecraft().effectRenderer.addEffect(ParticleFactoryRegistry.BULLET_HOLE.createParticle(option, level, hitPos.x, hitPos.y, hitPos.z, 0, 0, 0));
        }
    }

    private static void playHitBlockStepSound(WorldClient level, IBlockState state, double x, double y, double z) {
        if (!canHearHitBlockStepSound(x, y, z)) {
            return;
        }
        SoundType soundType = state.getBlock().getSoundType(state, level, new BlockPos(x, y, z), null);
        SoundEvent stepSound = soundType.getStepSound();
        if (!canPlayStepSound(level, stepSound)) {
            return;
        }
        float volume = soundType.getVolume() * 0.6F;
        float pitch = soundType.getPitch() * (0.9F + level.rand.nextFloat() * 0.2F);
        level.playSound(x, y, z, stepSound, SoundCategory.BLOCKS, volume, pitch, false);
    }

    private static boolean canHearHitBlockStepSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.getDistanceSq(x, y, z) <= MAX_HIT_STEP_SOUND_DISTANCE_SQR;
    }

    private static boolean canPlayStepSound(WorldClient level, SoundEvent sound) {
        long gameTime = level.getTotalWorldTime();
        SoundLimit limit = STEP_SOUND_LIMITS.get(sound);
        if (limit == null || limit.gameTime != gameTime) {
            STEP_SOUND_LIMITS.put(sound, new SoundLimit(gameTime, 1));
            return true;
        }
        if (limit.count >= MAX_SAME_STEP_SOUND_PER_TICK) {
            return false;
        }
        limit.count++;
        return true;
    }

    private static boolean shouldRemove(WorldClient level, BlockPos blockPos, Vec3d hitPos) {
        IBlockState blockState = level.getBlockState(blockPos);
        if (blockState.getMaterial() == Material.AIR) {
            return true;
        }
        // 1.12.2: one collision box instead of a voxel shape (null when the block has no collision)
        AxisAlignedBB baseBlockBoundingBox = blockState.getCollisionBoundingBox(level, blockPos);
        if (baseBlockBoundingBox == null) {
            return true;
        }
        AxisAlignedBB blockBoundingBox = baseBlockBoundingBox.offset(blockPos);
        boolean intersects = blockBoundingBox.intersects(
                hitPos.x - 0.1, hitPos.y - 0.1, hitPos.z - 0.1,
                hitPos.x + 0.1, hitPos.y + 0.1, hitPos.z + 0.1);
        return !intersects;
    }

    private static class SoundLimit {
        private final long gameTime;
        private int count;

        private SoundLimit(long gameTime, int count) {
            this.gameTime = gameTime;
            this.count = count;
        }
    }
}
