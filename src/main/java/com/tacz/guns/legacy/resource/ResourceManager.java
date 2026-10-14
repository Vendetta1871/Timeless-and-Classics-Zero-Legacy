package com.tacz.guns.legacy.resource;

import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The read side of 1.20.1 {@code net.minecraft.server.packs.resources.ResourceManager}. 1.12.2's
 * {@code IResourceManager} cannot list files, so gun packs are read through {@link GunPackRepository} instead.
 * Locations are full file paths, e.g. {@code tacz:display/guns/ak47_display.json}.
 */
public interface ResourceManager {
    Set<String> getNamespaces();

    /**
     * The topmost pack's copy of a file.
     */
    Optional<Resource> getResource(ResourceLocation location);

    /**
     * Every pack's copy of a file, bottom to top.
     */
    List<Resource> getResourceStack(ResourceLocation location);

    /**
     * Files under {@code path} in every namespace, topmost copy only.
     */
    Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter);

    Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter);
}
