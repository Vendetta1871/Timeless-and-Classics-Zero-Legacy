package com.tacz.guns.event;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.tacz.guns.legacy.network.PacketDistributor;

@Mod.EventBusSubscriber
public class SyncBaseTimestamp {
    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayer player && !event.getLevel().isRemote) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (EntityPlayerMP) player), new ServerMessageSyncBaseTimestamp());
        }
    }
}
