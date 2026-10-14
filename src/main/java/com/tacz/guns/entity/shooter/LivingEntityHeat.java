package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.entity.EntityLivingBase;

public class LivingEntityHeat {

    private final EntityLivingBase shooter;
    private final ShooterDataHolder data;

    public LivingEntityHeat(EntityLivingBase shooter, ShooterDataHolder dataHolder) {
        this.shooter = shooter;
        this.data = dataHolder;
    }

    public void tickHeat() {
        var gunStack = shooter.getHeldItemMainhand();
        if (gunStack.getItem() instanceof AbstractGunItem gunItem) {
            gunItem.tickHeat(data, gunStack, shooter);
        }
    }
}
