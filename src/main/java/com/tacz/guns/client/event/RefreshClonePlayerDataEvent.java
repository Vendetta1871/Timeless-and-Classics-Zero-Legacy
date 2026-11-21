package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.sync.core.DataHolder;
import com.tacz.guns.entity.sync.core.SyncedEntityData;
import com.tacz.guns.util.DelayedTask;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

/**
 * 当玩家跨越维度时，客户端需要刷新一次玩家的配件属性缓存
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class RefreshClonePlayerDataEvent {
    /** 1.12.2: the clone event's old player; the previous local player of this connection. */
    private static WeakReference<EntityPlayerSP> lastLocalPlayer = new WeakReference<>(null);

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        lastLocalPlayer = new WeakReference<>(null);
    }

    /**
     * 1.12.2: there is no client clone event; a respawned or teleported local player is a new
     * {@link EntityPlayerSP} joining the client world (this also fires once on login).
     */
    @SubscribeEvent
    public static void onClientPlayerClone(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote || !(event.getEntity() instanceof EntityPlayerSP newPlayer)) {
            return;
        }
        EntityPlayerSP oldPlayer = lastLocalPlayer.get();
        lastLocalPlayer = new WeakReference<>(newPlayer);
        if (oldPlayer != null && oldPlayer != newPlayer) {
            // 服务端只在实体重新加入世界时全量同步 SyncedEntityData。如果只有客户端重建了玩家实体（例如换皮肤模组/插件
            // 发送的 respawn 包），服务端不会重新同步，新实体会退回默认值（-1 的切枪/近战冷却），导致无法开火。
            // 因此把旧实体的数据复制过来；死亡重生、跨维度时服务端随后仍会全量同步覆盖。
            // 1.12.2: capabilities are never invalidated, so no reviveCaps/invalidateCaps around the copy
            DataHolder oldHolder = SyncedEntityData.instance().getDataHolder(oldPlayer);
            DataHolder newHolder = SyncedEntityData.instance().getDataHolder(newPlayer);
            if (oldHolder != null && newHolder != null) {
                newHolder.dataMap = new HashMap<>(oldHolder.dataMap);
            }
            // 基时间戳同理，只在服务端下发同步消息时更新，丢失会导致开火包的时间戳校验失败
            IClientPlayerGunOperator.fromLocalPlayer(newPlayer).getDataHolder().clientBaseTimestamp =
                    IClientPlayerGunOperator.fromLocalPlayer(oldPlayer).getDataHolder().clientBaseTimestamp;
        }
        // 但是这个事件触发时，玩家的背包并未同步，导致无法读取枪械数据进行配件属性缓存的刷新
        // 延迟 10 tick 执行缓存刷新就好了
        DelayedTask.add(() -> IGunOperator.fromLivingEntity(newPlayer).initialData(), 10);
    }

    /**
     * 延迟执行是通过这个方法执行的
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            try {
                DelayedTask.SUPPLIERS.removeIf(BooleanSupplier::getAsBoolean);
            } catch (Exception e) {
                DelayedTask.SUPPLIERS.clear();
                GunMod.LOGGER.catching(e);
            }
        }
    }
}
