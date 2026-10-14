package com.tacz.guns.legacy.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * 1.20.1 {@code NetworkEvent}: only {@link Context}, the handler context of a {@link SimpleChannel} message.
 */
public final class NetworkEvent {
    private NetworkEvent() {
    }

    public static class Context {
        private final NetworkDirection direction;
        private final NetworkManager networkManager;
        private final INetHandler netHandler;
        @Nullable
        private final EntityPlayerMP sender;
        private boolean packetHandled;

        Context(NetworkDirection direction, NetworkManager networkManager, INetHandler netHandler, @Nullable EntityPlayerMP sender) {
            this.direction = direction;
            this.networkManager = networkManager;
            this.netHandler = netHandler;
            this.sender = sender;
        }

        public NetworkDirection getDirection() {
            return direction;
        }

        /**
         * 1.12.2: {@code isMemoryConnection()} is {@link NetworkManager#isLocalChannel()} there.
         */
        public NetworkManager getNetworkManager() {
            return networkManager;
        }

        /**
         * The sending player for messages received on the server, {@code null} on the client.
         */
        @Nullable
        public EntityPlayerMP getSender() {
            return sender;
        }

        /**
         * Runs the task on the main thread of the receiving side (handlers are called on the network thread).
         */
        public CompletableFuture<Void> enqueueWork(Runnable runnable) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            FMLCommonHandler.instance().getWorldThread(netHandler).addScheduledTask(() -> {
                try {
                    runnable.run();
                    future.complete(null);
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                    throw throwable;
                }
            });
            return future;
        }

        public void setPacketHandled(boolean packetHandled) {
            this.packetHandled = packetHandled;
        }

        public boolean getPacketHandled() {
            return packetHandled;
        }
    }
}
