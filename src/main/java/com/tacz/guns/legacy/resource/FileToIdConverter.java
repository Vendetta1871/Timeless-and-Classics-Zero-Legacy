package com.tacz.guns.legacy.resource;

import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * 1.20.1 {@code net.minecraft.resources.FileToIdConverter}.
 */
public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String prefix, String extension) {
        this.prefix = prefix;
        this.extension = extension;
    }

    public static FileToIdConverter json(String name) {
        return new FileToIdConverter(name, ".json");
    }

    public ResourceLocation idToFile(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), this.prefix + "/" + id.getPath() + this.extension);
    }

    public ResourceLocation fileToId(ResourceLocation file) {
        String path = file.getPath();
        return new ResourceLocation(file.getNamespace(), path.substring(this.prefix.length() + 1, path.length() - this.extension.length()));
    }

    public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager resourceManager) {
        return resourceManager.listResources(this.prefix, location -> location.getPath().endsWith(this.extension));
    }

    public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager resourceManager) {
        return resourceManager.listResourceStacks(this.prefix, location -> location.getPath().endsWith(this.extension));
    }
}
