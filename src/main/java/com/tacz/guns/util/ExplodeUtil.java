package com.tacz.guns.util;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.world.WorldServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.ForgeEventFactory;

public class ExplodeUtil {
    public static void createExplosion(Entity owner, Entity exploder, float damage, float radius, boolean knockback, boolean destroy, Vec3d hitPos) {
        // 客户端不执行
        if (!(exploder.world instanceof WorldServer)) {
            return;
        }
        WorldServer level = (WorldServer) exploder.world;
        // 依据配置文件读取方块破坏方式
        ProjectileExplosion.BlockInteraction mode = ProjectileExplosion.BlockInteraction.KEEP;
        if (destroy) {
            mode = ProjectileExplosion.BlockInteraction.DESTROY;
        }
        // 创建爆炸
        ProjectileExplosion explosion = new ProjectileExplosion(level, owner, exploder, null, hitPos.x, hitPos.y, hitPos.z, damage, radius, knockback, mode);
        // 监听 forge 事件
        if (ForgeEventFactory.onExplosionStart(level, explosion)) {
            return;
        }
        // 执行爆炸逻辑
        explosion.doExplosionA();
        explosion.doExplosionB(true);
        if (mode == ProjectileExplosion.BlockInteraction.KEEP) {
            explosion.clearAffectedBlockPositions();
        }
        // 客户端发包，发送爆炸相关信息
        level.getPlayers(EntityPlayerMP.class, player -> MathHelper.sqrt((float) player.getDistanceSq(hitPos.x, hitPos.y, hitPos.z)) < AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get()).forEach(player -> {
            SPacketExplosion packet = new SPacketExplosion(hitPos.x, hitPos.y, hitPos.z, radius, explosion.getAffectedBlockPositions(), explosion.getPlayerKnockbackMap().get(player));
            player.connection.sendPacket(packet);
        });
    }
}
