package com.tacz.guns.client.particle;


import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Vector3f;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class AmmoParticleSpawner {
    public static void addParticle(EntityKineticBullet bullet) {
        TimelessAPI.getGunDisplay(bullet.getGunDisplayId(), bullet.getGunId()).ifPresent(gunIndex -> {
            AmmoParticle gunParticle = gunIndex.getParticle();
            if (gunParticle == null) {
                // 如果枪械没有粒子效果，那么调用子弹的
                TimelessAPI.getClientAmmoIndex(bullet.getAmmoId()).ifPresent(ammoIndex -> {
                    AmmoParticle ammoParticle = ammoIndex.getParticle();
                    if (ammoParticle == null) {
                        return;
                    }
                    spawnParticle(bullet, ammoParticle);
                });
            } else {
                // 否则调用调用枪械的
                spawnParticle(bullet, gunParticle);
            }
        });
    }

    private static void spawnParticle(EntityKineticBullet bullet, AmmoParticle particle) {
        // 1.12.2: EnumParticleTypes instead of ParticleOptions
        EnumParticleTypes particleOptions = particle.getParticleOptions();
        if (particleOptions == null) {
            return;
        }
        int count = particle.getCount();
        Vector3f delta = particle.getDelta();
        float particleSpeed = particle.getSpeed();
        ParticleManager particleEngine = Minecraft.getMinecraft().effectRenderer;
        if (count == 0) {
            double xSpeed = particleSpeed * delta.x();
            double ySpeed = particleSpeed * delta.y();
            double zSpeed = particleSpeed * delta.z();
            Particle result = particleEngine.spawnEffectParticle(particleOptions.getParticleID(), bullet.posX, bullet.posY, bullet.posZ, xSpeed, ySpeed, zSpeed);
            if (result != null) {
                result.setMaxAge(particle.getLifeTime());
            }
        } else {
            Random random = bullet.world.rand;
            Entity owner = bullet.getOwner();
            for (int i = 0; i < count; ++i) {
                createParticle(bullet, particle, random, delta, particleSpeed, owner, particleEngine, particleOptions);
            }
        }
    }

    private static void createParticle(EntityKineticBullet bullet, AmmoParticle particle, Random random, Vector3f delta, float particleSpeed, Entity owner, ParticleManager particleEngine, EnumParticleTypes particleOptions) {
        double deltaMovementRandom = random.nextDouble();
        double offsetX = random.nextGaussian() * delta.x() + deltaMovementRandom * bullet.motionX;
        double offsetY = random.nextGaussian() * delta.y() + deltaMovementRandom * bullet.motionY;
        double offsetZ = random.nextGaussian() * delta.z() + deltaMovementRandom * bullet.motionZ;
        double xSpeed = random.nextGaussian() * particleSpeed;
        double ySpeed = random.nextGaussian() * particleSpeed;
        double zSpeed = random.nextGaussian() * particleSpeed;

        double posX = bullet.posX + offsetX;
        double posY = bullet.posY + offsetY;
        double posZ = bullet.posZ + offsetZ;

        // 如果太贴近发射者，不进行粒子生成
        if (owner == null || owner.getDistanceSq(posX, posY, posZ) > 3 * 3) {
            Particle result = particleEngine.spawnEffectParticle(particleOptions.getParticleID(), posX, posY, posZ, xSpeed, ySpeed, zSpeed);
            if (result != null) {
                result.setMaxAge(particle.getLifeTime());
            }
        }
    }
}
