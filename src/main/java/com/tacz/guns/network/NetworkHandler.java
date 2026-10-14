package com.tacz.guns.network;

import com.tacz.guns.GunMod;
import com.tacz.guns.network.message.*;
import com.tacz.guns.network.message.event.*;
import com.tacz.guns.network.message.handshake.ServerMessageSyncedEntityDataMapping;
import com.tacz.guns.legacy.network.NetworkDirection;
import com.tacz.guns.legacy.particle.BulletHoleMessage;
import com.tacz.guns.legacy.network.NetworkRegistry;
import com.tacz.guns.legacy.network.PacketDistributor;
import com.tacz.guns.legacy.network.SimpleChannel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class NetworkHandler {
    private static final String VERSION = "1.0.5";

    public static final SimpleChannel HANDSHAKE_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(GunMod.MOD_ID, "handshake"),
            () -> VERSION, it -> it.equals(VERSION), it -> it.equals(VERSION));
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(GunMod.MOD_ID, "network"),
            () -> VERSION, it -> it.equals(VERSION), it -> it.equals(VERSION));

    private static final AtomicInteger ID_COUNT = new AtomicInteger(1);
    private static final AtomicInteger HANDSHAKE_ID_COUNT = new AtomicInteger(1);

    public static void init() {
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerShoot.class, ClientMessagePlayerShoot::encode, ClientMessagePlayerShoot::decode, ClientMessagePlayerShoot::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerReloadGun.class, ClientMessagePlayerReloadGun::encode, ClientMessagePlayerReloadGun::decode, ClientMessagePlayerReloadGun::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerCancelReload.class, ClientMessagePlayerCancelReload::encode, ClientMessagePlayerCancelReload::decode, ClientMessagePlayerCancelReload::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerFireSelect.class, ClientMessagePlayerFireSelect::encode, ClientMessagePlayerFireSelect::decode, ClientMessagePlayerFireSelect::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerAim.class, ClientMessagePlayerAim::encode, ClientMessagePlayerAim::decode, ClientMessagePlayerAim::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerCrawl.class, ClientMessagePlayerCrawl::encode, ClientMessagePlayerCrawl::decode, ClientMessagePlayerCrawl::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerDrawGun.class, ClientMessagePlayerDrawGun::encode, ClientMessagePlayerDrawGun::decode, ClientMessagePlayerDrawGun::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageSound.class, ServerMessageSound::encode, ServerMessageSound::decode, ServerMessageSound::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessageCraft.class, ClientMessageCraft::encode, ClientMessageCraft::decode, ClientMessageCraft::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageCraft.class, ServerMessageCraft::encode, ServerMessageCraft::decode, ServerMessageCraft::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerZoom.class, ClientMessagePlayerZoom::encode, ClientMessagePlayerZoom::decode, ClientMessagePlayerZoom::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessageRefitGun.class, ClientMessageRefitGun::encode, ClientMessageRefitGun::decode, ClientMessageRefitGun::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageRefreshRefitScreen.class, ServerMessageRefreshRefitScreen::encode, ServerMessageRefreshRefitScreen::decode, ServerMessageRefreshRefitScreen::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessageUnloadAttachment.class, ClientMessageUnloadAttachment::encode, ClientMessageUnloadAttachment::decode, ClientMessageUnloadAttachment::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageSwapItem.class, ServerMessageSwapItem::encode, ServerMessageSwapItem::decode, ServerMessageSwapItem::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerBoltGun.class, ClientMessagePlayerBoltGun::encode, ClientMessagePlayerBoltGun::decode, ClientMessagePlayerBoltGun::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageLevelUp.class, ServerMessageLevelUp::encode, ServerMessageLevelUp::decode, ServerMessageLevelUp::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunHurt.class, ServerMessageGunHurt::encode, ServerMessageGunHurt::decode, ServerMessageGunHurt::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunKill.class, ServerMessageGunKill::encode, ServerMessageGunKill::decode, ServerMessageGunKill::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageUpdateEntityData.class, ServerMessageUpdateEntityData::encode, ServerMessageUpdateEntityData::decode, ServerMessageUpdateEntityData::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageSyncGunPack.class, ServerMessageSyncGunPack::encode, ServerMessageSyncGunPack::decode, ServerMessageSyncGunPack::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessagePlayerMelee.class, ClientMessagePlayerMelee::encode, ClientMessagePlayerMelee::decode, ClientMessagePlayerMelee::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunDraw.class, ServerMessageGunDraw::encode, ServerMessageGunDraw::decode, ServerMessageGunDraw::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunFire.class, ServerMessageGunFire::encode, ServerMessageGunFire::decode, ServerMessageGunFire::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunFireSelect.class, ServerMessageGunFireSelect::encode, ServerMessageGunFireSelect::decode, ServerMessageGunFireSelect::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunMelee.class, ServerMessageGunMelee::encode, ServerMessageGunMelee::decode, ServerMessageGunMelee::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunReload.class, ServerMessageGunReload::encode, ServerMessageGunReload::decode, ServerMessageGunReload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageGunShoot.class, ServerMessageGunShoot::encode, ServerMessageGunShoot::decode, ServerMessageGunShoot::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ServerMessageSyncBaseTimestamp.class, ServerMessageSyncBaseTimestamp::encode, ServerMessageSyncBaseTimestamp::decode, ServerMessageSyncBaseTimestamp::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessageSyncBaseTimestamp.class, ClientMessageSyncBaseTimestamp::encode, ClientMessageSyncBaseTimestamp::decode, ClientMessageSyncBaseTimestamp::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), ClientMessageLaserColor.class, ClientMessageLaserColor::encode, ClientMessageLaserColor::decode, ClientMessageLaserColor::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // 1.12.2: no login phase, so there is nothing to acknowledge (Acknowledge is not ported)
//        registerAcknowledge();
        registerHandshakeMessage(ServerMessageSyncedEntityDataMapping.class, null);

        // 1.12.2: bullet holes (ServerLevel#sendParticles with a custom particle upstream)
        CHANNEL.registerMessage(ID_COUNT.getAndIncrement(), BulletHoleMessage.class, BulletHoleMessage::encode, BulletHoleMessage::decode, BulletHoleMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * 1.12.2: mod channels have no login phase. Handshake messages are registered on {@link #HANDSHAKE_CHANNEL} like
     * play messages and sent to each player as soon as they have logged in ({@link #onPlayerLoggedIn}), before the
     * other TACZ messages of that player.
     */
    private static final List<Function<Boolean, List<?>>> LOGIN_PACKETS = new ArrayList<>();

    public static <T extends LoginIndexHolder & IMessage<T>> void registerHandshakeMessage(Class<T> messageClass, @Nullable Function<Boolean, List<Pair<String, T>>> messages) {
        try {
            Constructor<T> constructor = messageClass.getDeclaredConstructor();
            T message = constructor.newInstance();
            HANDSHAKE_CHANNEL.registerMessage(HANDSHAKE_ID_COUNT.getAndIncrement(), messageClass, message::encode, message::decode, message::handle,
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            if (messages != null) {
                LOGIN_PACKETS.add(local -> messages.apply(local).stream().map(Pair::getValue).collect(Collectors.toList()));
            } else {
                LOGIN_PACKETS.add(local -> {
                    try {
                        return Collections.singletonList(constructor.newInstance());
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("The message %s is missing an empty parameter constructor", messageClass.getName()), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format("Unable to access the constructor of %s. Make sure the constructor is public.", messageClass.getName()), e);
        } catch (Exception e) {
            GunMod.LOGGER.error("Fail to register handshake message {}", messageClass.getName());
            e.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP player)) {
            return;
        }
        boolean local = player.connection.getNetworkManager().isLocalChannel();
        for (Function<Boolean, List<?>> packets : LOGIN_PACKETS) {
            for (Object message : packets.apply(local)) {
                HANDSHAKE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
            }
        }
    }

    public static void sendToClientPlayer(Object message, EntityPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> (EntityPlayerMP) player), message);
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> centerEntity), message);
    }

    public static void sendToAllPlayers(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendToTrackingEntity(Object message, final Entity centerEntity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> centerEntity), message);
    }

    public static void sendToDimension(Object message, final Entity centerEntity) {
        int dimension = centerEntity.dimension;
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
    }
}
