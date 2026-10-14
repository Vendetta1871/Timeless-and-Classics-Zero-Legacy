package com.tacz.guns.legacy.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/**
 * Reduced 1.20.1 {@code ClipContext} (ray trace request): start/end point, which block shape to trace against and
 * which fluids to stop at. 1.12.2 has no VoxelShape / FluidState, so the shape suppliers of upstream are not ported.
 */
public class ClipContext {
    private final Vec3d from;
    private final Vec3d to;
    private final Block block;
    private final Fluid fluid;
    @Nullable
    private final Entity entity;

    public ClipContext(Vec3d from, Vec3d to, Block block, Fluid fluid, @Nullable Entity entity) {
        this.from = from;
        this.to = to;
        this.block = block;
        this.fluid = fluid;
        this.entity = entity;
    }

    public Vec3d getFrom() {
        return this.from;
    }

    public Vec3d getTo() {
        return this.to;
    }

    public Block getBlock() {
        return this.block;
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }

    public enum Block {
        COLLIDER,
        OUTLINE,
        VISUAL,
        FALLDAMAGE_RESETTING
    }

    public enum Fluid {
        NONE,
        SOURCE_ONLY,
        ANY
    }
}
