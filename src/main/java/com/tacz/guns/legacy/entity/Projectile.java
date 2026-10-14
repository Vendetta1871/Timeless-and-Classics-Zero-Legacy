package com.tacz.guns.legacy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The parts of 1.20.1 {@code Projectile} that TACZ's bullet uses, on top of a plain 1.12.2 {@link Entity}: the owner
 * (kept as a UUID plus a cached entity, as upstream), {@link #ownedBy}, {@link #lerpRotation} and the block hit hook
 * ({@link ProjectileHitBlock}, upstream's {@code BlockState#onProjectileHit}).
 */
public abstract class Projectile extends Entity {
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;

    protected Projectile(World world) {
        super(world);
    }

    public void setOwner(@Nullable Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUniqueID();
            this.cachedOwner = owner;
        }
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isDead) {
            return this.cachedOwner;
        } else if (this.ownerUUID != null && this.world instanceof WorldServer serverLevel) {
            this.cachedOwner = serverLevel.getEntityFromUuid(this.ownerUUID);
            return this.cachedOwner;
        }
        return null;
    }

    public boolean ownedBy(@Nullable Entity entity) {
        return entity != null && this.ownerUUID != null && entity.getUniqueID().equals(this.ownerUUID);
    }

    protected void onHitBlock(RayTraceResult result) {
        net.minecraft.block.state.IBlockState state = this.world.getBlockState(result.getBlockPos());
        if (state.getBlock() instanceof ProjectileHitBlock hitBlock) {
            hitBlock.onProjectileHit(this.world, state, result, this);
        }
    }

    /**
     * 1.20.1 {@code Projectile#shootFromRotation}.
     */
    public void shootFromRotation(Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
        float f = -MathHelper.sin(yaw * ((float) Math.PI / 180F)) * MathHelper.cos(pitch * ((float) Math.PI / 180F));
        float f1 = -MathHelper.sin((pitch + roll) * ((float) Math.PI / 180F));
        float f2 = MathHelper.cos(yaw * ((float) Math.PI / 180F)) * MathHelper.cos(pitch * ((float) Math.PI / 180F));
        this.shoot(f, f1, f2, velocity, inaccuracy);
        this.motionX += shooter.motionX;
        this.motionY += shooter.onGround ? 0.0D : shooter.motionY;
        this.motionZ += shooter.motionZ;
    }

    /**
     * 1.20.1 {@code Projectile#shoot}: the direction with a triangular spread scaled by {@code inaccuracy}.
     */
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double length = Math.sqrt(x * x + y * y + z * z);
        double spread = 0.0172275D * (double) inaccuracy;
        double vx = (x / length + spread * (this.rand.nextDouble() - this.rand.nextDouble())) * velocity;
        double vy = (y / length + spread * (this.rand.nextDouble() - this.rand.nextDouble())) * velocity;
        double vz = (z / length + spread * (this.rand.nextDouble() - this.rand.nextDouble())) * velocity;
        this.motionX = vx;
        this.motionY = vy;
        this.motionZ = vz;
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        this.rotationYaw = (float) (MathHelper.atan2(vx, vz) * (double) (180F / (float) Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(vy, horizontal) * (double) (180F / (float) Math.PI));
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    protected static float lerpRotation(float current, float target) {
        while (target - current < -180.0F) {
            current -= 360.0F;
        }
        while (target - current >= 180.0F) {
            current += 360.0F;
        }
        return current + 0.2F * (target - current);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasUniqueId("Owner")) {
            this.ownerUUID = compound.getUniqueId("Owner");
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (this.ownerUUID != null) {
            compound.setUniqueId("Owner", this.ownerUUID);
        }
    }

    /**
     * Shortcut for {@link MathHelper#sqrt(double)} of the horizontal motion ({@code Vec3#horizontalDistance}).
     */
    protected double horizontalMotion() {
        return MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
    }
}
