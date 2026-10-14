package com.tacz.guns.legacy.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * 1.20.1 {@code Block#onProjectileHit}: implemented by blocks that react to TACZ projectiles (e.g. the target block).
 */
public interface ProjectileHitBlock {
    void onProjectileHit(World world, IBlockState state, RayTraceResult hit, Projectile projectile);
}
