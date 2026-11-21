package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class GunSoundAngerEvent {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGunFire(GunFireEvent event) {
        if (event.isCanceled() || !GunConfig.ENABLE_GUN_SOUND_ANGER.get()) {
            return;
        }
        if (event.getLogicalSide() != Side.SERVER || !(event.getShooter() instanceof EntityPlayerMP player)) {
            return;
        }
        if (player instanceof FakePlayer || player.isCreative() || player.isSpectator()) {
            return;
        }

        ItemStack gunItem = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunItem);
        if (gun == null) {
            return;
        }

        TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem)).ifPresent(gunIndex -> {
            GunData gunData = gunIndex.getGunData();
            if (hasSilencer(gunItem, gunData)) {
                return;
            }

            float fireMultiplier = gunData.getFireSound().getFireMultiplier();
            int neutralRadius = getCappedRadius(GunConfig.DEFAULT_GUN_SOUND_NEUTRAL_MOB_ANGER_DISTANCE.get(), fireMultiplier);
            int hostileRadius = getCappedRadius(GunConfig.DEFAULT_GUN_SOUND_HOSTILE_MOB_ANGER_DISTANCE.get(), fireMultiplier);
            if (neutralRadius <= 0 && hostileRadius <= 0) {
                return;
            }

            angerMobs(player, neutralRadius, hostileRadius);
        });
    }

    private static boolean hasSilencer(ItemStack gunItem, GunData gunData) {
        final boolean[] found = {false};
        AttachmentDataUtils.getAllAttachmentData(gunItem, gunData, attachmentData -> {
            if (attachmentData.getModifier().containsKey(SilenceModifier.ID)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static int getCappedRadius(int baseRadius, float multiplier) {
        return Math.min(Math.round(baseRadius * multiplier), GunConfig.MAX_GUN_SOUND_MOB_ANGER_DISTANCE);
    }

    private static void angerMobs(EntityPlayerMP player, int neutralRadius, int hostileRadius) {
        World level = player.world;
        int searchRadius = Math.max(neutralRadius, hostileRadius);
        double neutralRadiusSqr = (double) neutralRadius * neutralRadius;
        double hostileRadiusSqr = (double) hostileRadius * hostileRadius;
        level.getEntitiesWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(searchRadius), mob -> mob != null && shouldAnger(player, mob, neutralRadiusSqr, hostileRadiusSqr))
                .forEach(mob -> {
                    if (isNeutralMob(mob)) {
                        mob.setRevengeTarget(player);
                    } else {
                        mob.setAttackTarget(player);
                    }
                });
    }

    private static boolean shouldAnger(EntityPlayerMP player, EntityLiving mob, double neutralRadiusSqr, double hostileRadiusSqr) {
        if (!mob.isEntityAlive() || mob.getAttackTarget() != null) {
            return false;
        }
        if (mob.isOnSameTeam(player) || !canAttack(mob, player)) {
            return false;
        }
        if (isNeutralMob(mob)) {
            if (mob instanceof EntityTameable tamable && tamable.isTamed()) {
                return false;
            }
            return !(mob instanceof EntityIronGolem ironGolem && ironGolem.isPlayerCreated()) && mob.getDistanceSq(player) <= neutralRadiusSqr;
        }
        return mob instanceof IMob && mob.getDistanceSq(player) <= hostileRadiusSqr;
    }

    /**
     * 1.12.2 has no {@code NeutralMob} interface: the vanilla mobs that implement it on 1.20.1 and exist on 1.12.2
     * (they all retaliate through {@code EntityAIHurtByTarget}, so the revenge target makes them attack).
     */
    private static boolean isNeutralMob(EntityLiving mob) {
        return mob instanceof EntityWolf || mob instanceof EntityIronGolem || mob instanceof EntityPolarBear
                || mob instanceof EntityPigZombie || mob instanceof EntityEnderman;
    }

    /**
     * {@code LivingEntity#canAttack(LivingEntity)}: no player targets on peaceful, and the target must be attackable
     * ({@code canBeSeenAsEnemy}; creative and spectator players were already filtered out).
     */
    private static boolean canAttack(EntityLiving mob, EntityPlayerMP player) {
        if (mob.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            return false;
        }
        return mob.canAttackClass(player.getClass()) && !player.getIsInvulnerable() && player.isEntityAlive();
    }
}
