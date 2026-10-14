package com.tacz.guns.api.event.server;

import com.tacz.guns.api.event.common.KubeJSGunEventPoster;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 子弹击中方块时触发的事件，目前仅在服务端触发
 */
public class AmmoHitBlockEvent extends Event implements KubeJSGunEventPoster<AmmoHitBlockEvent> {
    private final World level;
    private final RayTraceResult hitResult;
    private final IBlockState state;
    private final EntityKineticBullet ammo;

    public AmmoHitBlockEvent(World level, RayTraceResult hitResult, IBlockState state, EntityKineticBullet ammo) {
        this.level = level;
        this.hitResult = hitResult;
        this.state = state;
        this.ammo = ammo;
        postServerEventToKubeJS(this);
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    public World getLevel() {
        return level;
    }

    public RayTraceResult getHitResult() {
        return hitResult;
    }

    public IBlockState getState() {
        return state;
    }

    public EntityKineticBullet getAmmo() {
        return ammo;
    }
}
