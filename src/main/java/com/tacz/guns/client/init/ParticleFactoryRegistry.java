package com.tacz.guns.client.init;

import com.tacz.guns.client.particle.BulletHoleParticle;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.12.2: particle types and providers do not exist, so there is nothing to register. The bullet hole particle is
 * spawned by {@code legacy.particle.BulletHoleMessage} with {@link #BULLET_HOLE} on the client's
 * {@code ParticleManager}.
 */
@SideOnly(Side.CLIENT)
public class ParticleFactoryRegistry {
    public static final BulletHoleParticle.Provider BULLET_HOLE = new BulletHoleParticle.Provider();
}
