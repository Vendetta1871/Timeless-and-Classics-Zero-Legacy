package com.tacz.guns.sound;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import com.tacz.guns.legacy.network.PacketDistributor;

public class SoundManager {
    /**
     * 射击音效，自己能听见
     */
    public static String SHOOT_SOUND = "shoot";
    /**
     * 其他玩家听到的枪声
     */
    public static String SHOOT_3P_SOUND = "shoot_3p";
    /**
     * 消音器音效
     */
    public static String SILENCE_SOUND = "silence";
    /**
     * 其他玩家听到的消音器枪声
     */
    public static String SILENCE_3P_SOUND = "silence_3p";
    /**
     * 近战刺刀音效
     */
    public static String MELEE_BAYONET = "melee_bayonet";
    /**
     * 近战推人音效
     */
    public static String MELEE_PUSH = "melee_push";
    /**
     * 近战枪拖砸人音效
     */
    public static String MELEE_STOCK = "melee_stock";
    /**
     * 没有子弹时，空击的声音
     */
    public static String DRY_FIRE_SOUND = "dry_fire";
    /**
     * 空仓换弹声音
     */
    public static String RELOAD_EMPTY_SOUND = "reload_empty";
    /**
     * 战术换弹声音
     */
    public static String RELOAD_TACTICAL_SOUND = "reload_tactical";
    /**
     * 空仓检视声音
     */
    public static String INSPECT_EMPTY_SOUND = "inspect_empty";
    /**
     * 普通检视声音
     */
    public static String INSPECT_SOUND = "inspect";
    /**
     * 切枪切入声音
     */
    public static String DRAW_SOUND = "draw";
    /**
     * 切枪切出的声音
     */
    public static String PUT_AWAY_SOUND = "put_away";
    /**
     * 拉栓声音
     */
    public static String BOLT_SOUND = "bolt";
    /**
     * 切换开关模式的声音
     */
    public static String FIRE_SELECT = "fire_select";
    /**
     * 爆头击中声音
     */
    public static String HEAD_HIT_SOUND = "head_hit";
    /**
     * 普通击中声音
     */
    public static String FLESH_HIT_SOUND = "flesh_hit";
    /**
     * 击杀的声音
     */
    public static String KILL_SOUND = "kill";
    /**
     * 卸载配件的声音，用于配件的
     */
    public static String UNINSTALL_SOUND = "uninstall";
    /**
     * 装载配件的声音，用于配件的
     */
    public static String INSTALL_SOUND = "install";

//    public static void sendSoundToNearby(EntityLivingBase sourceEntity, int distance, ResourceLocation gunId, String soundName, float volume, float pitch) {
//        sendSoundToNearby(sourceEntity, distance, gunId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID, soundName, volume, pitch);
//    }

    public static void sendSoundToNearby(EntityLivingBase sourceEntity, int distance, ResourceLocation gunId, ResourceLocation gunDisplayId, String soundName, float volume, float pitch) {
        if (sourceEntity.world instanceof WorldServer serverLevel) {
            BlockPos pos = new BlockPos(sourceEntity);
            ServerMessageSound soundMessage = new ServerMessageSound(sourceEntity.getEntityId(), gunId, gunDisplayId, soundName, volume, pitch, distance);
            // 1.12.2: players in the world instead of the players watching the chunk, with the same distance check
            serverLevel.getPlayers(EntityPlayerMP.class, p -> p.getDistanceSq(pos.getX(), pos.getY(), pos.getZ()) < distance * distance).stream()
                    .filter(p -> p.getEntityId() != sourceEntity.getEntityId())
                    .forEach(p -> NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), soundMessage));
        }
    }
}
