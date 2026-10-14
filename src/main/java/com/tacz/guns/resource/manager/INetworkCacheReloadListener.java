package com.tacz.guns.resource.manager;

import com.tacz.guns.resource.network.DataType;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.resource.PreparableReloadListener;

import java.util.Map;

public interface INetworkCacheReloadListener extends PreparableReloadListener {
    Map<ResourceLocation, String> getNetworkCache();

    DataType getType();
}
