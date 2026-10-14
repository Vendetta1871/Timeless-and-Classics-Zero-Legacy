package com.tacz.guns.particles;

import com.tacz.guns.legacy.network.FriendlyByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * 1.12.2: particle options do not exist; this is the data of a bullet hole particle, sent to clients by
 * {@code legacy.particle.BulletHoleMessage}. The codec, the command deserializer and the particle type are not ported.
 */
public class BulletHoleOption {
    private final EnumFacing direction;
    private final BlockPos pos;
    private final String ammoId;
    private final String gunId;
    private final String gunDisplayId;

    public BulletHoleOption(int dir, long pos, String ammoId, String gunId, String gunDisplayId) {
        this.direction = EnumFacing.values()[dir];
        this.pos = BlockPos.fromLong(pos);
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
    }

    public BulletHoleOption(EnumFacing dir, BlockPos pos, String ammoId, String gunId, String gunDisplayId) {
        this.direction = dir;
        this.pos = pos;
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
    }

    public EnumFacing getDirection() {
        return this.direction;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public String getAmmoId() {
        return ammoId;
    }

    public String getGunId() {
        return gunId;
    }

    public String getGunDisplayId() {
        return gunDisplayId;
    }

    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.direction);
        buffer.writeBlockPos(this.pos);
        buffer.writeUtf(this.ammoId);
        buffer.writeUtf(this.gunId);
        buffer.writeUtf(this.gunDisplayId);
    }

    /**
     * 1.12.2: upstream's {@code DESERIALIZER.fromNetwork}, matching {@link #writeToNetwork}.
     */
    public static BulletHoleOption fromNetwork(FriendlyByteBuf buffer) {
        return new BulletHoleOption(buffer.readEnum(EnumFacing.class), buffer.readBlockPos(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
    }
}
