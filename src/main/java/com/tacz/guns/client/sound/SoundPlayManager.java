package com.tacz.guns.client.sound;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.config.client.SoundConfig;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.entity.EntityPlayerSP;
import com.tacz.guns.legacy.resource.FileToIdConverter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class SoundPlayManager {
    private static final FileToIdConverter TACZ_SOUND_LISTER = new FileToIdConverter("tacz_sounds", ".ogg");

    private static final Map<SoundKey, ArrayDeque<TrackedGunSound>> TRACKED_GUN_SOUNDS = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> SOUND_RESOURCE_EXISTS_CACHE = new HashMap<>();
    private static final Set<ResourceLocation> MISSING_SOUND_WARNED = new HashSet<>();

    private static int soundCleanupTickCounter = 0;

    /**
     * 用于阻止连发时，反复播放 DryFire 音效
     */
    private static boolean DRY_SOUND_TRACK = true;

    /**
     * 临时缓存，用于停止播放的
     */
    private static GunSoundInstance tmpSoundInstance = null;

    @Nullable
    public static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance, boolean mono) {
        return playClientSound(entity, name, volume, pitch, distance, mono, SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT.get(), true, false);
    }

    @Nullable
    private static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (name == null || !hasSoundResource(minecraft, name)) {
            return null;
        }
        if (concurrencyLimit > 0) {
            limitConcurrentGunSound(minecraft, entity.getEntityId(), name, concurrencyLimit);
        }
        GunSoundInstance instance = trackEntity
                ? new EntityTrackingGunSoundInstance(ModSounds.GUN.get(), SoundCategory.PLAYERS, volume, pitch, entity, distance, name, mono)
                : new GunSoundInstance(ModSounds.GUN.get(), SoundCategory.PLAYERS, volume, pitch, entity, distance, name, mono, relative);
        // 1.12.2: the sound engine has no canPlaySound() check of its own
        if (instance.canPlaySound()) {
            minecraft.getSoundHandler().playSound(instance);
        }
        if (concurrencyLimit > 0) {
            trackGunSound(entity.getEntityId(), entity.getUniqueID(), name, instance);
        }
//        traceGunSoundPlay(minecraft, instance, entity, name, volume, pitch, distance, mono);
        return instance;
    }

    @Nullable
    public static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance) {
        return playClientSound(entity, name, volume, pitch, distance, false);
    }

    @Nullable
    public static GunSoundInstance playAnimationSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance) {
        if (isLocalPlayer(entity)) {
            boolean trackFirstPerson = SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING.get();
            return playClientSound(entity, name, volume, pitch, distance, false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), trackFirstPerson, !trackFirstPerson);
        }
        return playClientSound(entity, name, volume, pitch, distance, false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), true, false);
    }


    public static void stopPlayGunSound() {
        if (tmpSoundInstance != null) {
            tmpSoundInstance.setStop();
        }
    }

    public static void stopPlayGunSound(GunDisplayInstance gunIndex, String animationName) {
        if (tmpSoundInstance != null) {
            if (tmpSoundInstance.getRegistryName() != null && tmpSoundInstance.getRegistryName().equals(gunIndex.getSounds(animationName))) {
                tmpSoundInstance.setStop();
            }
        }
    }

    public static void playerRefitSound(ItemStack attachmentItem, EntityPlayerSP player, String soundName) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (iAttachment == null) {
            return;
        }
        ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
        TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(index -> {
            Map<String, ResourceLocation> sounds = index.getSounds();
            if (sounds.containsKey(soundName)) {
                ResourceLocation resourceLocation = sounds.get(soundName);
                SoundPlayManager.playClientSound(player, resourceLocation, 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
            }
        });
    }

    public static void playShootSound(EntityLivingBase entity, GunDisplayInstance gunIndex, GunData gunData) {
        playClientSound(entity, gunIndex.getSounds(SoundManager.SHOOT_SOUND), 0.8f, 0.9f + entity.getRNG().nextFloat() * 0.125f, (int) (GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get() * gunData.getFireSound().getFireMultiplier()), false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), false, false);
    }

    public static void playSilenceSound(EntityLivingBase entity, GunDisplayInstance gunIndex, GunData gunData) {
        playClientSound(entity, gunIndex.getSounds(SoundManager.SILENCE_SOUND), 0.6f, 0.9f + entity.getRNG().nextFloat() * 0.125f, (int) (GunConfig.DEFAULT_GUN_SILENCE_SOUND_DISTANCE.get() * gunData.getFireSound().getSilenceMultiplier()), false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), false, false);
    }

    public static void playDryFireSound(EntityLivingBase entity, GunDisplayInstance gunIndex) {
        if (DRY_SOUND_TRACK) {
            playClientSound(entity, gunIndex.getSounds(SoundManager.DRY_FIRE_SOUND), 1.0f, 0.9f + entity.getRNG().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
            DRY_SOUND_TRACK = false;
        }
    }

    /**
     * 只有松开鼠标时，才会重置
     */
    public static void resetDryFireSound() {
        DRY_SOUND_TRACK = true;
    }

    public static void playReloadSound(EntityLivingBase entity, GunDisplayInstance display, boolean noAmmo) {
        if (noAmmo) {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.RELOAD_EMPTY_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        } else {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.RELOAD_TACTICAL_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        }
    }

    public static void playInspectSound(EntityLivingBase entity, GunDisplayInstance display, boolean noAmmo) {
        if (noAmmo) {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.INSPECT_EMPTY_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        } else {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.INSPECT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        }
    }

    public static void playBoltSound(EntityLivingBase entity, GunDisplayInstance display) {
        tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.BOLT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playDrawSound(EntityLivingBase entity, GunDisplayInstance display) {
        tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.DRAW_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playPutAwaySound(EntityLivingBase entity, GunDisplayInstance display) {
        tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.PUT_AWAY_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playFireSelectSound(EntityLivingBase entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.FIRE_SELECT), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMeleeBayonetSound(EntityLivingBase entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.MELEE_BAYONET), 1.0f, 0.9f + entity.getRNG().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMeleePushSound(EntityLivingBase entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.MELEE_PUSH), 1.0f, 0.9f + entity.getRNG().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMeleeStockSound(EntityLivingBase entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.MELEE_STOCK), 1.0f, 0.9f + entity.getRNG().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playHeadHitSound(EntityLivingBase entity, GunDisplayInstance display) {
        boolean relative = isLocalPlayer(entity);
        playClientSound(entity, display.getSounds(SoundManager.HEAD_HIT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get(), false, SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT.get(), !relative, relative);
    }

    public static void playFleshHitSound(EntityLivingBase entity, GunDisplayInstance display) {
        boolean relative = isLocalPlayer(entity);
        playClientSound(entity, display.getSounds(SoundManager.FLESH_HIT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get(), false, SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT.get(), !relative, relative);
    }

    public static void playKillSound(EntityLivingBase entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.KILL_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMessageSound(ServerMessageSound message) {
        WorldClient level = Minecraft.getMinecraft().world;
        if (level == null || !(level.getEntityByID(message.getEntityId()) instanceof EntityLivingBase livingEntity)) {
            return;
        }
        ResourceLocation gunId = message.getGunId();
        ResourceLocation gunDisplayId = message.getGunDisplayId();
        TimelessAPI.getGunDisplay(gunDisplayId, gunId).ifPresent(index -> {
            String soundName = message.getSoundName();
            ResourceLocation soundId = index.getSounds(soundName);
            if (soundId == null) {
                return;
            }
            if (SoundManager.SHOOT_3P_SOUND.equals(soundName) || SoundManager.SILENCE_3P_SOUND.equals(soundName)) {
                playClientSound(livingEntity, soundId, message.getVolume(), message.getPitch(), message.getDistance(), true, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), true, false);
            } else {
                playClientSound(livingEntity, soundId, message.getVolume(), message.getPitch(), message.getDistance());
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            cleanupInvalidEntitySounds(minecraft);
            return;
        }
        soundCleanupTickCounter++;
        if (soundCleanupTickCounter % 5 == 0) {
            cleanupInvalidEntitySounds(minecraft);
        }
    }

    public static void clearSoundResourceCache() {
        SOUND_RESOURCE_EXISTS_CACHE.clear();
        MISSING_SOUND_WARNED.clear();
    }

    private static void limitConcurrentGunSound(Minecraft minecraft, int entityId, ResourceLocation soundId, int limit) {
        SoundKey key = new SoundKey(entityId, soundId);
        ArrayDeque<TrackedGunSound> sounds = TRACKED_GUN_SOUNDS.get(key);
        if (sounds == null) {
            return;
        }
        int activeForKey = 0;
        Iterator<TrackedGunSound> iterator = sounds.iterator();
        while (iterator.hasNext()) {
            TrackedGunSound tracked = iterator.next();
            if (minecraft.getSoundHandler().isSoundPlaying(tracked.instance())) {
                activeForKey++;
            } else {
                iterator.remove();
            }
        }
        if (sounds.isEmpty()) {
            TRACKED_GUN_SOUNDS.remove(key);
        }

        int toStop = activeForKey - limit + 1;
        if (toStop <= 0) {
            return;
        }

        int stopped = 0;
        iterator = sounds.iterator();
        while (iterator.hasNext() && stopped < toStop) {
            TrackedGunSound tracked = iterator.next();
            if (minecraft.getSoundHandler().isSoundPlaying(tracked.instance())) {
                tracked.instance().setStop();
                iterator.remove();
                stopped++;
            }
        }
        if (sounds.isEmpty()) {
            TRACKED_GUN_SOUNDS.remove(key);
        }
    }

    private static void trackGunSound(int entityId, UUID entityUuid, ResourceLocation soundId, GunSoundInstance instance) {
        SoundKey key = new SoundKey(entityId, soundId);
        TRACKED_GUN_SOUNDS.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(new TrackedGunSound(instance, entityUuid));
    }

    private static void cleanupInvalidEntitySounds(Minecraft minecraft) {
        if (minecraft.world == null) {
            stopAndClearTrackedSounds();
            return;
        }
        Iterator<Map.Entry<SoundKey, ArrayDeque<TrackedGunSound>>> entryIterator = TRACKED_GUN_SOUNDS.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<SoundKey, ArrayDeque<TrackedGunSound>> entry = entryIterator.next();
            Iterator<TrackedGunSound> soundIterator = entry.getValue().iterator();
            while (soundIterator.hasNext()) {
                TrackedGunSound tracked = soundIterator.next();
                if (!minecraft.getSoundHandler().isSoundPlaying(tracked.instance())) {
                    soundIterator.remove();
                    continue;
                }
                Entity owner = minecraft.world.getEntityByID(entry.getKey().entityId());
                if (isInvalidSoundOwner(owner, tracked.entityUuid())) {
                    tracked.instance().setStop();
                    soundIterator.remove();
                }
            }
            if (entry.getValue().isEmpty()) {
                entryIterator.remove();
            }
        }
    }

    private static void stopAndClearTrackedSounds() {
        for (ArrayDeque<TrackedGunSound> sounds : TRACKED_GUN_SOUNDS.values()) {
            for (TrackedGunSound tracked : sounds) {
                tracked.instance().setStop();
            }
        }
        TRACKED_GUN_SOUNDS.clear();
    }

    private static boolean isInvalidSoundOwner(@Nullable Entity entity, UUID entityUuid) {
        return entity == null
                || !entity.getUniqueID().equals(entityUuid)
                || entity.isDead
                || entity instanceof EntityLivingBase livingEntity && livingEntity.getHealth() <= 0;
    }

    private static boolean isLocalPlayer(Entity entity) {
        return entity == Minecraft.getMinecraft().player;
    }

    /**
     * 1.12.2: {@code getResource} throws instead of returning an empty optional.
     */
    private static boolean resourceExists(Minecraft minecraft, ResourceLocation location) {
        try (IResource ignored = minecraft.getResourceManager().getResource(location)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean hasSoundResource(Minecraft minecraft, ResourceLocation soundId) {
        boolean exists = SOUND_RESOURCE_EXISTS_CACHE.computeIfAbsent(soundId, id -> {
            ResourceLocation soundPath = TACZ_SOUND_LISTER.idToFile(id);
            return resourceExists(minecraft, soundPath);
        });
        if (!exists && MISSING_SOUND_WARNED.add(soundId)) {
            ResourceLocation soundPath = TACZ_SOUND_LISTER.idToFile(soundId);
            GunMod.LOGGER.warn("[TACZ Sound] Missing gun sound resource, skipped. sound={}, path={}", soundId, soundPath);
        }
        return exists;
    }

    private static final class SoundKey {
        private final int entityId;
        private final ResourceLocation soundId;

        public SoundKey(int entityId, ResourceLocation soundId) {
            this.entityId = entityId;
            this.soundId = soundId;
        }

        public int entityId() {
            return entityId;
        }

        public ResourceLocation soundId() {
            return soundId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SoundKey)) return false;
            SoundKey that = (SoundKey) o;
            return java.util.Objects.equals(entityId, that.entityId) && java.util.Objects.equals(soundId, that.soundId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(entityId, soundId);
        }

        @Override
        public String toString() {
            return "SoundKey[" + "entityId=" + entityId + ", " + "soundId=" + soundId + "]";
        }
}

    private static final class TrackedGunSound {
        private final GunSoundInstance instance;
        private final UUID entityUuid;

        public TrackedGunSound(GunSoundInstance instance, UUID entityUuid) {
            this.instance = instance;
            this.entityUuid = entityUuid;
        }

        public GunSoundInstance instance() {
            return instance;
        }

        public UUID entityUuid() {
            return entityUuid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TrackedGunSound)) return false;
            TrackedGunSound that = (TrackedGunSound) o;
            return java.util.Objects.equals(instance, that.instance) && java.util.Objects.equals(entityUuid, that.entityUuid);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(instance, entityUuid);
        }

        @Override
        public String toString() {
            return "TrackedGunSound[" + "instance=" + instance + ", " + "entityUuid=" + entityUuid + "]";
        }
}
}
