package com.tacz.guns.network.message;

import com.tacz.guns.client.particle.HitBlockEffectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerMessageBulletHitBlock {
    private final double hitX;
    private final double hitY;
    private final double hitZ;
    private final EnumFacing direction;
    private final BlockPos blockPos;
    private final String ammoId;
    private final String gunId;
    private final String gunDisplayId;
    private final boolean incendiary;

    public ServerMessageBulletHitBlock(Vec3d hitPos, EnumFacing direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        this.hitX = hitPos.x;
        this.hitY = hitPos.y;
        this.hitZ = hitPos.z;
        this.direction = direction;
        this.blockPos = blockPos;
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.incendiary = incendiary;
    }

    public ServerMessageBulletHitBlock(double hitX, double hitY, double hitZ, EnumFacing direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.direction = direction;
        this.blockPos = blockPos;
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.incendiary = incendiary;
    }

    public static void encode(ServerMessageBulletHitBlock message, FriendlyByteBuf buf) {
        buf.writeDouble(message.hitX);
        buf.writeDouble(message.hitY);
        buf.writeDouble(message.hitZ);
        buf.writeEnum(message.direction);
        buf.writeBlockPos(message.blockPos);
        buf.writeUtf(message.ammoId);
        buf.writeUtf(message.gunId);
        buf.writeUtf(message.gunDisplayId);
        buf.writeBoolean(message.incendiary);
    }

    public static ServerMessageBulletHitBlock decode(FriendlyByteBuf buf) {
        double hitX = buf.readDouble();
        double hitY = buf.readDouble();
        double hitZ = buf.readDouble();
        EnumFacing direction = buf.readEnum(EnumFacing.class);
        BlockPos blockPos = buf.readBlockPos();
        String ammoId = buf.readUtf();
        String gunId = buf.readUtf();
        String gunDisplayId = buf.readUtf();
        boolean incendiary = buf.readBoolean();
        return new ServerMessageBulletHitBlock(hitX, hitY, hitZ, direction, blockPos, ammoId, gunId, gunDisplayId, incendiary);
    }

    public static void handle(ServerMessageBulletHitBlock message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> handleClient(message));
        }
        context.setPacketHandled(true);
    }

    @SideOnly(Side.CLIENT)
    private static void handleClient(ServerMessageBulletHitBlock message) {
        WorldClient level = Minecraft.getMinecraft().world;
        if (level == null) {
            return;
        }
        HitBlockEffectHandler.handle(level,
                new Vec3d(message.hitX, message.hitY, message.hitZ),
                message.direction,
                message.blockPos,
                message.ammoId,
                message.gunId,
                message.gunDisplayId,
                message.incendiary);
    }
}
