package com.tacz.guns.client.sound;

import net.minecraft.client.audio.ITickableSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class EntityTrackingGunSoundInstance extends GunSoundInstance implements ITickableSound {
    private final WeakReference<Entity> entityRef;
    private boolean stopped;

    public EntityTrackingGunSoundInstance(SoundEvent soundEvent, SoundCategory source, float volume, float pitch, Entity entity, int soundDistance, @Nullable ResourceLocation registryName, boolean mono) {
        super(soundEvent, source, volume, pitch, entity, soundDistance, registryName, mono, false);
        this.entityRef = new WeakReference<>(entity);
    }

    @Override
    public boolean canPlaySound() {
        Entity entity = this.entityRef.get();
        return super.canPlaySound() && entity != null && !entity.isSilent();
    }

    @Override
    public boolean isDonePlaying() {
        return this.stopped;
    }

    @Override
    public void update() {
        Entity entity = this.entityRef.get();
        if (entity == null || entity.isDead || entity instanceof EntityLivingBase livingEntity && livingEntity.getHealth() <= 0) {
            this.setStop();
            return;
        }
        this.xPosF = (float) entity.posX;
        this.yPosF = (float) (entity.posY + entity.getEyeHeight());
        this.zPosF = (float) entity.posZ;
    }

    @Override
    public void setStop() {
        this.stopped = true;
        super.setStop();
    }
}
