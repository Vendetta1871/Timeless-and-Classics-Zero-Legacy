package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 1.12.2: there are no data-driven damage types. Each upstream type (all with message id {@code tacz.bullet}) is a
 * {@link BulletDamageSource} configured like its JSON and its vanilla tags ({@code bypasses_armor},
 * {@code bypasses_invulnerability}); {@link #BULLETS_TAG} matches all of them.
 */
public class ModDamageTypes {
    public static final ResourceLocation BULLET = new ResourceLocation(GunMod.MOD_ID, "bullet");
    public static final ResourceLocation BULLET_IGNORE_ARMOR = new ResourceLocation(GunMod.MOD_ID, "bullet_ignore_armor");
    public static final ResourceLocation BULLET_VOID = new ResourceLocation(GunMod.MOD_ID, "bullet_void");
    public static final ResourceLocation BULLET_VOID_IGNORE_ARMOR = new ResourceLocation(GunMod.MOD_ID, "bullet_void_ignore_armor");

    /**
     * {@code source.is(BULLETS_TAG)} becomes {@code BULLETS_TAG.contains(source)}.
     */
    public static final DamageTypeTag BULLETS_TAG = source -> source instanceof BulletDamageSource;

    public static class Sources {
        public static DamageSource bullet(Entity bullet, @Nullable Entity shooter, boolean ignoreArmor) {
            return new BulletDamageSource(ignoreArmor ? BULLET_IGNORE_ARMOR : BULLET, bullet, shooter, ignoreArmor, false);
        }

        public static DamageSource bulletVoid(Entity bullet, @Nullable Entity shooter, boolean ignoreArmor) {
            return new BulletDamageSource(ignoreArmor ? BULLET_VOID_IGNORE_ARMOR : BULLET_VOID, bullet, shooter, ignoreArmor, true);
        }
    }

    @FunctionalInterface
    public interface DamageTypeTag {
        boolean contains(DamageSource source);
    }

    /**
     * A TACZ bullet damage: message id {@code tacz.bullet}, exhaustion 0.05, never scaled with difficulty.
     */
    public static class BulletDamageSource extends EntityDamageSourceIndirect {
        private final ResourceLocation type;

        public BulletDamageSource(ResourceLocation type, Entity bullet, @Nullable Entity shooter, boolean ignoreArmor, boolean bypassesInvulnerability) {
            super(GunMod.MOD_ID + ".bullet", bullet, shooter);
            this.type = type;
            this.setProjectile();
            if (ignoreArmor) {
                this.setDamageBypassesArmor();
            }
            if (bypassesInvulnerability) {
                this.setDamageAllowedInCreativeMode();
            }
        }

        public ResourceLocation getType() {
            return type;
        }

        @Override
        public float getHungerDamage() {
            return 0.05F;
        }

        @Override
        public boolean isDifficultyScaled() {
            return false;
        }
    }
}
