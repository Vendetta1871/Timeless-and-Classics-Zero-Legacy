package com.tacz.guns.legacy.network;

import com.tacz.guns.GunMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 1.20.1 Forge {@code SimpleChannel} on top of a 1.12.2 {@link FMLEventChannel}: messages keep their upstream
 * {@code encode/decode/handle} methods. A packet is the message index (var int) followed by the encoded message.
 * Handlers run on the network thread, as on 1.20.1; {@link NetworkEvent.Context#enqueueWork} moves to the main thread.
 */
public class SimpleChannel {
    private final String name;
    private final FMLEventChannel channel;
    private final Int2ObjectMap<MessageType<?>> byIndex = new Int2ObjectOpenHashMap<>();
    private final Map<Class<?>, MessageType<?>> byClass = new HashMap<>();

    public SimpleChannel(ResourceLocation name) {
        // 1.12.2 channel names are limited to 20 characters
        this.name = name.toString();
        this.channel = net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.newEventDrivenChannel(this.name);
        this.channel.register(this);
    }

    public <T> void registerMessage(int index, Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder,
                                    Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        registerMessage(index, messageType, encoder, decoder, handler, Optional.empty());
    }

    public <T> void registerMessage(int index, Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder,
                                    Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> handler,
                                    Optional<NetworkDirection> direction) {
        MessageType<T> type = new MessageType<>(index, messageType, encoder, decoder, handler, direction.orElse(null));
        if (byIndex.put(index, type) != null || byClass.put(messageType, type) != null) {
            throw new IllegalArgumentException("Duplicate message " + messageType.getName() + " / " + index + " on channel " + name);
        }
    }

    public void sendToServer(Object message) {
        channel.sendToServer(toPacket(message));
    }

    public void send(PacketDistributor.PacketTarget target, Object message) {
        switch (target.kind) {
            case PLAYER -> channel.sendTo(toPacket(message), (EntityPlayerMP) target.argument);
            case TRACKING_ENTITY -> channel.sendToAllTracking(toPacket(message), (Entity) target.argument);
            case TRACKING_ENTITY_AND_SELF -> {
                Entity entity = (Entity) target.argument;
                channel.sendToAllTracking(toPacket(message), entity);
                if (entity instanceof EntityPlayerMP player && player.connection != null) {
                    channel.sendTo(toPacket(message), player);
                }
            }
            case DIMENSION -> channel.sendToDimension(toPacket(message), (Integer) target.argument);
            case ALL -> channel.sendToAll(toPacket(message));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> FMLProxyPacket toPacket(T message) {
        MessageType<T> type = (MessageType<T>) byClass.get(message.getClass());
        if (type == null) {
            throw new IllegalArgumentException("Unregistered message " + message.getClass().getName() + " on channel " + name);
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(type.index);
        type.encoder.accept(message, buffer);
        // FMLProxyPacket splits large packets using the whole backing array, so hand it an exactly sized one
        byte[] bytes = Arrays.copyOf(buffer.array(), buffer.writerIndex());
        return new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(bytes)), name);
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        EntityPlayerMP sender = ((NetHandlerPlayServer) event.getHandler()).player;
        handle(event, new NetworkEvent.Context(NetworkDirection.PLAY_TO_SERVER, event.getManager(), event.getHandler(), sender));
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        handle(event, new NetworkEvent.Context(NetworkDirection.PLAY_TO_CLIENT, event.getManager(), event.getHandler(), null));
    }

    private void handle(FMLNetworkEvent.CustomPacketEvent<?> event, NetworkEvent.Context context) {
        ByteBuf payload = event.getPacket().payload();
        FriendlyByteBuf buffer = new FriendlyByteBuf(payload);
        int index = buffer.readVarInt();
        MessageType<?> type = byIndex.get(index);
        if (type == null) {
            GunMod.LOGGER.warn("Unknown message {} on channel {}", index, name);
            return;
        }
        if (type.direction != null && type.direction != context.getDirection()) {
            GunMod.LOGGER.warn("Message {} received in the wrong direction {}", type.messageType.getName(), context.getDirection());
            return;
        }
        type.handle(buffer, context);
    }

    private static final class MessageType<T> {
        private final int index;
        private final Class<T> messageType;
        private final BiConsumer<T, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, T> decoder;
        private final BiConsumer<T, Supplier<NetworkEvent.Context>> handler;
        private final NetworkDirection direction;

        private MessageType(int index, Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
                            BiConsumer<T, Supplier<NetworkEvent.Context>> handler, NetworkDirection direction) {
            this.index = index;
            this.messageType = messageType;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
            this.direction = direction;
        }

        private void handle(FriendlyByteBuf buffer, NetworkEvent.Context context) {
            T message = decoder.apply(buffer);
            handler.accept(message, () -> context);
        }
    }
}
