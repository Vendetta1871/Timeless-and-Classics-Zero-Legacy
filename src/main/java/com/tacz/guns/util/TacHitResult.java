package com.tacz.guns.util;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class TacHitResult extends RayTraceResult {
    private final boolean headshot;

    public TacHitResult(EntityKineticBullet.EntityResult result) {
        super(result.getEntity(), result.getHitPos());
        this.headshot = result.isHeadshot();
    }

    public boolean isHeadshot() {
        return this.headshot;
    }

    /**
     * 1.12.2: {@code EntityHitResult#getEntity()}.
     */
    public Entity getEntity() {
        return this.entityHit;
    }

    /**
     * 1.12.2: {@code HitResult#getLocation()}.
     */
    public Vec3d getLocation() {
        return this.hitVec;
    }
}
