package com.tacz.guns.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import com.tacz.guns.legacy.resource.FileToIdConverter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.entity.Entity;

import javax.annotation.Nullable;

public class GunSoundInstance extends PositionedSound {
    private static final FileToIdConverter TACZ_SOUND_LISTER = new FileToIdConverter("tacz_sounds", ".ogg");

    @Nullable
    private final ResourceLocation registryName;
    private final boolean canPlay;
    @Nullable
    private Sound redirectedSound;

    public GunSoundInstance(SoundEvent soundEvent, SoundCategory source, float volume, float pitch, Entity entity, int soundDistance, ResourceLocation registryName, boolean mono) {
        this(soundEvent, source, volume, pitch, entity, soundDistance, registryName, mono, false);
    }

    /**
     * 1.12.2: sounds cannot be listener-relative; a relative sound is placed at the entity like the others (with no
     * distance attenuation either way).
     */
    public GunSoundInstance(SoundEvent soundEvent, SoundCategory source, float volume, float pitch, Entity entity, int soundDistance, ResourceLocation registryName, boolean mono, boolean relative) {
        super(soundEvent, source);
        this.attenuationType = AttenuationType.NONE;
        this.registryName = registryName;
        this.canPlay = !entity.isSilent();
        this.volume = volume;
        this.pitch = pitch;
        this.xPosF = (float) entity.posX;
        this.yPosF = (float) entity.posY;
        this.zPosF = (float) entity.posZ;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && !relative) {
            this.volume = volume * (1.0F - Math.min(1.0F, (float) Math.sqrt(player.getDistanceSq(xPosF, yPosF, zPosF)) / soundDistance));
            this.volume *= this.volume;
        }
    }

    public void setStop() {
        Minecraft.getMinecraft().getSoundHandler().stopSound(this);
    }

    /**
     * 1.12.2: the sound engine does not ask; {@link SoundPlayManager} checks it before playing.
     */
    public boolean canPlaySound() {
        return this.canPlay;
    }

    @Nullable
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public SoundEventAccessor createAccessor(SoundHandler handler) {
        SoundEventAccessor events = super.createAccessor(handler);
        if (events != null && this.registryName != null) {
            this.redirectedSound = new TaczSound(this.registryName, TACZ_SOUND_LISTER.idToFile(this.registryName), super.getSound());
            this.sound = this.redirectedSound;
        } else {
            this.redirectedSound = null;
        }
        return events;
    }

    @Override
    public Sound getSound() {
        return redirectedSound == null ? super.getSound() : redirectedSound;
    }

    private static class TaczSound extends Sound {
        private final ResourceLocation path;

        private TaczSound(ResourceLocation location, ResourceLocation path, Sound template) {
            super(location.toString(), template.getVolume(), template.getPitch(), template.getWeight(), Type.FILE,
                    template.isStreaming());
            this.path = path;
        }

        /**
         * 1.12.2 builds {@code sounds/<path>.ogg} from the name; gun pack sounds live under {@code tacz_sounds}.
         */
        @Override
        public ResourceLocation getSoundAsOggLocation() {
            return path;
        }
    }
}
