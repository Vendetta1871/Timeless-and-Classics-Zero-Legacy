package com.tacz.guns.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.network.PacketDistributor;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSyncBaseTimestamp;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class SyncBaseTimestamp {
    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayer player && !event.getWorld().isRemote) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (EntityPlayerMP) player), new ServerMessageSyncBaseTimestamp());
        }
    }
}
