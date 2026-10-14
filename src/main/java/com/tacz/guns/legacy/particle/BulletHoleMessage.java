package com.tacz.guns.legacy.particle;

import com.tacz.guns.legacy.network.FriendlyByteBuf;
import com.tacz.guns.legacy.network.NetworkEvent;
import com.tacz.guns.legacy.network.PacketDistributor;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Supplier;

/**
 * 1.12.2 stand-in for {@code ServerLevel#sendParticles(BulletHoleOption, ...)}: the vanilla particle packet only
 * knows {@code EnumParticleTypes}, so bullet holes travel as a TACZ message to players within the vanilla particle
 * range (32 blocks, like {@code WorldServer#spawnParticle}).
 */
public class BulletHoleMessage {
    private static final double RANGE_SQ = 32 * 32;

    private final BulletHoleOption option;
    private final double x;
    private final double y;
    private final double z;

    public BulletHoleMessage(BulletHoleOption option, double x, double y, double z) {
        this.option = option;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void send(WorldServer level, BulletHoleOption option, double x, double y, double z) {
        BulletHoleMessage message = new BulletHoleMessage(option, x, y, z);
        for (EntityPlayerMP player : level.getPlayers(EntityPlayerMP.class, p -> p.getDistanceSq(x, y, z) < RANGE_SQ)) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static void encode(BulletHoleMessage message, FriendlyByteBuf buf) {
        message.option.writeToNetwork(buf);
        buf.writeDouble(message.x);
        buf.writeDouble(message.y);
        buf.writeDouble(message.z);
    }

    public static BulletHoleMessage decode(FriendlyByteBuf buf) {
        BulletHoleOption option = BulletHoleOption.fromNetwork(buf);
        return new BulletHoleMessage(option, buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(BulletHoleMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> spawn(message));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void spawn(BulletHoleMessage message) {
        // TODO(port): needs BulletHoleParticle (client/particle): add new BulletHoleParticle.Provider().createParticle(..)
        //  to Minecraft#effectRenderer at message.x/y/z
    }
}
