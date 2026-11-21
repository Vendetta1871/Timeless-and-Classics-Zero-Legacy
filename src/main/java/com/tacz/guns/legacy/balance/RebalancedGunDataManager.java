package com.tacz.guns.legacy.balance;

import com.google.gson.JsonElement;
import com.tacz.guns.legacy.resource.ProfilerFiller;
import com.tacz.guns.legacy.resource.ResourceManager;
import com.tacz.guns.resource.manager.GunDataManager;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

/**
 * 1.12.2 addition (com.tacz.guns.legacy): {@link GunDataManager} that runs the
 * {@link DamageRebalance} on the scanned JSON before it is parsed and before the same elements are
 * cached as strings for {@code ServerMessageSyncGunPack}. The logical server, the remote clients
 * (through the synced pack data) and the local client reload all end up with the rebalanced values.
 */
public class RebalancedGunDataManager extends GunDataManager {
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        DamageRebalance.reload();
        DamageRebalance.rebalance(pObject);
        super.apply(pObject, pResourceManager, pProfiler);
    }
}
