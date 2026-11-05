package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.I18n;

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

        /**
         * Upstream's {@code damageSources().indirectMagic(bullet, shooter)}: the armor-piercing magic damage dealt to
         * the entities of the {@code tacz:use_magic_damage_on} tag (endermen).
         */
        public static DamageSource indirectMagic(Entity bullet, @Nullable Entity shooter) {
            return new MagicBulletDamageSource(bullet, shooter);
        }
    }

    @FunctionalInterface
    public interface DamageTypeTag {
        boolean contains(DamageSource source);
    }

    /**
     * A TACZ bullet damage: message id {@code tacz.bullet}, exhaustion 0.05, never scaled with difficulty.
     *
     * <p>Deliberately not an {@code EntityDamageSourceIndirect}: in 1.12.2 that class marks the damage as arrow-like,
     * and {@code EntityEnderman} ignores every indirect source outright (it teleports away unharmed). 1.20.1 has no
     * indirect damage sources at all and bullets do damage endermen, so the source mirrors upstream's
     * {@code new DamageSource(type, bullet, shooter)}: the true source is the shooter, the immediate source the bullet.
     */
    public static class BulletDamageSource extends EntityDamageSource {
        private final ResourceLocation type;
        private final Entity bullet;

        public BulletDamageSource(ResourceLocation type, Entity bullet, @Nullable Entity shooter, boolean ignoreArmor, boolean bypassesInvulnerability) {
            super(GunMod.MOD_ID + ".bullet", shooter);
            this.type = type;
            this.bullet = bullet;
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

        /**
         * The bullet itself, as upstream's {@code DamageSource.getDirectEntity()}; the true source stays the shooter.
         */
        @Override
        @Nullable
        public Entity getImmediateSource() {
            return this.bullet;
        }

        /**
         * The bullet's position, as upstream reads {@code getDirectEntity().position()} (the shield-block direction).
         */
        @Override
        @Nullable
        public Vec3d getDamageLocation() {
            return this.bullet == null ? null : this.bullet.getPositionVector();
        }

        /**
         * The shooter's name and held item, like {@code EntityDamageSourceIndirect} used to print, with the bullet
         * as the fallback when the shooter is gone.
         */
        @Override
        public ITextComponent getDeathMessage(EntityLivingBase entityLivingBaseIn) {
            Entity killer = this.getTrueSource() == null ? this.bullet : this.getTrueSource();
            ItemStack itemstack = killer instanceof EntityLivingBase ? ((EntityLivingBase) killer).getHeldItemMainhand() : ItemStack.EMPTY;
            String s = "death.attack." + this.damageType;
            String s1 = s + ".item";
            return !itemstack.isEmpty() && itemstack.hasDisplayName() && I18n.canTranslate(s1) ?
                    new TextComponentTranslation(s1, new Object[] {entityLivingBaseIn.getDisplayName(), killer.getDisplayName(), itemstack.getTextComponent()}) :
                    new TextComponentTranslation(s, new Object[] {entityLivingBaseIn.getDisplayName(), killer.getDisplayName()});
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

    /**
     * The {@code indirectMagic} damage for the {@code tacz:use_magic_damage_on} tag (endermen): armor-piercing magic,
     * like vanilla's {@code causeIndirectMagicDamage} but a direct source, for the same enderman reason as
     * {@link BulletDamageSource}. The enderman takes the hit and only then teleports away (unblockable damage),
     * as it does on 1.20.1.
     */
    public static class MagicBulletDamageSource extends EntityDamageSource {
        private final Entity bullet;

        public MagicBulletDamageSource(Entity bullet, @Nullable Entity shooter) {
            super("indirectMagic", shooter);
            this.bullet = bullet;
            this.setDamageBypassesArmor();
            this.setMagicDamage();
        }

        @Override
        @Nullable
        public Entity getImmediateSource() {
            return this.bullet;
        }

        @Override
        @Nullable
        public Vec3d getDamageLocation() {
            return this.bullet == null ? null : this.bullet.getPositionVector();
        }

        /**
         * Like {@code EntityDamageSourceIndirect}: the shooter's name, the bullet as fallback when the shooter is gone.
         */
        @Override
        public ITextComponent getDeathMessage(EntityLivingBase entityLivingBaseIn) {
            Entity killer = this.getTrueSource() == null ? this.bullet : this.getTrueSource();
            return new TextComponentTranslation("death.attack." + this.damageType, entityLivingBaseIn.getDisplayName(), killer.getDisplayName());
        }
    }
}
