package com.tacz.guns.util.block;

import com.google.common.collect.Sets;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.legacy.util.ClipContext;
import com.tacz.guns.util.HitboxHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

public class ProjectileExplosion extends Explosion {
    private final World level;
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final float radius;
    private final boolean knockback;
    private final Entity owner;
    private final Entity exploder;
    private final DamageSource damageSource;

    /**
     * 1.12.2 has no Explosion.BlockInteraction / ExplosionDamageCalculator: block interaction is a plain enum here,
     * and the damage calculator is replaced by the block's own explosion resistance.
     */
    public enum BlockInteraction {
        KEEP,
        DESTROY
    }

    public ProjectileExplosion(World level, Entity owner, Entity exploder, @javax.annotation.Nullable DamageSource source, double x, double y, double z, float power, float radius, boolean knockback, BlockInteraction mode) {
        super(level, exploder, x, y, z, radius, AmmoConfig.EXPLOSIVE_AMMO_FIRE.get(), mode == BlockInteraction.DESTROY);
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.radius = radius;
        this.owner = owner;
        this.exploder = exploder;
        this.damageSource = source == null ? DamageSource.causeExplosionDamage(this) : source;
        this.knockback = knockback;
    }

    @Override
    public void doExplosionA() {
        // TODO(port): upstream posts GameEvent.EXPLODE here (no 1.12.2 equivalent)
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int x = 0; x < i; ++x) {
            for (int y = 0; y < i; ++y) {
                for (int z = 0; z < i; ++z) {
                    if (x == 0 || x == i - 1 || y == 0 || y == i - 1 || z == 0 || z == i - 1) {
                        double d0 = ((float) x / (i - 1) * 2.0F - 1.0F);
                        double d1 = ((float) y / (i - 1) * 2.0F - 1.0F);
                        double d2 = ((float) z / (i - 1) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.7F + this.level.rand.nextFloat() * 0.6F);
                        double blockX = this.x;
                        double blockY = this.y;
                        double blockZ = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                            IBlockState blockState = this.level.getBlockState(pos);
                            if (!this.level.isValid(pos)) {
                                break;
                            }

                            // ExplosionDamageCalculator#getBlockExplosionResistance: air has no resistance
                            if (blockState.getMaterial() != net.minecraft.block.material.Material.AIR) {
                                float resistance = this.exploder != null
                                        ? this.exploder.getExplosionResistance(this, this.level, pos, blockState)
                                        : blockState.getBlock().getExplosionResistance(this.level, pos, null, this);
                                f -= (resistance + f1) * f1;
                            }

                            // ExplosionDamageCalculator#shouldBlockExplode is always true by default
                            if (f > 0.0F) {
                                set.add(pos);
                            }

                            blockX += d0 * (double) f1;
                            blockY += d1 * (double) f1;
                            blockZ += d2 * (double) f1;
                        }
                    }
                }
            }
        }

        this.getAffectedBlockPositions().addAll(set);
        float radius = this.radius;
        int minX = MathHelper.floor(this.x - (double) radius - 1.0D);
        int maxX = MathHelper.floor(this.x + (double) radius + 1.0D);
        int minY = MathHelper.floor(this.y - (double) radius - 1.0D);
        int maxY = MathHelper.floor(this.y + (double) radius + 1.0D);
        int minZ = MathHelper.floor(this.z - (double) radius - 1.0D);
        int maxZ = MathHelper.floor(this.z + (double) radius + 1.0D);
        radius *= 2;
        List<Entity> entities = this.level.getEntitiesWithinAABBExcludingEntity(this.exploder, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.level, this, entities, radius);
        Vec3d explosionPos = new Vec3d(this.x, this.y, this.z);

        for (Entity entity : entities) {
            if (entity.isImmuneToExplosions()) {
                continue;
            }

            AxisAlignedBB boundingBox = HitboxHelper.getFixedBoundingBox(entity, this.owner);
            RayTraceResult result;
            double strength;
            double deltaX;
            double deltaY;
            double deltaZ;
            double minDistance = radius;

            Vec3d[] d = new Vec3d[15];

            if (!(entity instanceof EntityLivingBase)) {
                strength = Math.sqrt(entity.getDistanceSq(explosionPos.x, explosionPos.y, explosionPos.z)) * 2 / radius;
                deltaX = entity.posX - this.x;
                deltaY = (entity instanceof EntityTNTPrimed ? entity.posY : entity.posY + entity.getEyeHeight()) - this.y;
                deltaZ = entity.posZ - this.z;
            } else {
                deltaX = (boundingBox.maxX + boundingBox.minX) / 2;
                deltaY = (boundingBox.maxY + boundingBox.minY) / 2;
                deltaZ = (boundingBox.maxZ + boundingBox.minZ) / 2;
                d[0] = new Vec3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                d[1] = new Vec3d(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
                d[2] = new Vec3d(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
                d[3] = new Vec3d(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
                d[4] = new Vec3d(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
                d[5] = new Vec3d(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
                d[6] = new Vec3d(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
                d[7] = new Vec3d(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                d[8] = new Vec3d(boundingBox.minX, deltaY, deltaZ);
                d[9] = new Vec3d(boundingBox.maxX, deltaY, deltaZ);
                d[10] = new Vec3d(deltaX, boundingBox.minY, deltaZ);
                d[11] = new Vec3d(deltaX, boundingBox.maxY, deltaZ);
                d[12] = new Vec3d(deltaX, deltaY, boundingBox.minZ);
                d[13] = new Vec3d(deltaX, deltaY, boundingBox.maxZ);
                d[14] = new Vec3d(deltaX, deltaY, deltaZ);
                for (int s = 0; s < 15; s++) {
                    result = BlockRayTrace.rayTraceBlocks(this.level, new ClipContext(explosionPos, d[s], ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
                    minDistance = (result.typeOfHit != RayTraceResult.Type.BLOCK) ? Math.min(minDistance, explosionPos.distanceTo(d[s])) : minDistance;
                }
                strength = minDistance * 2 / radius;
                deltaX -= this.x;
                deltaY -= this.y;
                deltaZ -= this.z;
            }

            if (strength > 1.0D) {
                continue;
            }

            double distanceToExplosion = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            if (distanceToExplosion != 0.0D) {
                deltaX /= distanceToExplosion;
                deltaY /= distanceToExplosion;
                deltaZ /= distanceToExplosion;
            }

            double damage = 1.0D - strength;
            entity.attackEntityFrom(this.damageSource, (float) damage * this.power);

            if (entity instanceof EntityLivingBase) {
                damage = (float) EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, damage);
            }

            float multiplier = this.power * radius / 500;
            // 启用击退效果
            if (AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get() && this.knockback) {
                entity.motionX += deltaX * damage * multiplier;
                entity.motionY += deltaY * damage * multiplier;
                entity.motionZ += deltaZ * damage * multiplier;
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (!player.isSpectator() && (!player.isCreative() || !player.capabilities.isFlying)) {
                        this.getPlayerKnockbackMap().put(player, new Vec3d(deltaX * damage * multiplier, deltaY * damage * multiplier, deltaZ * damage * multiplier));
                    }
                }
            }
        }
    }
}
