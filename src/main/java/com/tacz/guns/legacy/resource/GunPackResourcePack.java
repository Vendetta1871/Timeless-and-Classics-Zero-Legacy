package com.tacz.guns.legacy.resource;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Exposes the {@code assets/} half of one gun pack to the 1.12.2 resource manager, so textures, sounds and models
 * resolve by resource location as they do on 1.20.1. JSON language files are served as {@code .lang}.
 */
@SideOnly(Side.CLIENT)
public class GunPackResourcePack implements IResourcePack {
    private final GunPackRepository.PackSource pack;

    public GunPackResourcePack(GunPackRepository.PackSource pack) {
        this.pack = pack;
    }

    private static String filePath(ResourceLocation location) {
        return PackType.CLIENT_RESOURCES.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath();
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String path = filePath(location);
        if (pack.hasFile(path)) {
            return pack.resource(path).open();
        }
        if (JsonLang.isLangPath(location.getPath())) {
            String jsonPath = JsonLang.jsonPathFor(path);
            if (pack.hasFile(jsonPath)) {
                try (InputStream json = pack.resource(jsonPath).open()) {
                    return new ByteArrayInputStream(JsonLang.toLegacyLang(json));
                }
            }
        }
        throw new FileNotFoundException(location.toString());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        String path = filePath(location);
        return pack.hasFile(path) || JsonLang.isLangPath(location.getPath()) && pack.hasFile(JsonLang.jsonPathFor(path));
    }

    @Override
    public Set<String> getResourceDomains() {
        return pack.getNamespaces(PackType.CLIENT_RESOURCES);
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return "TACZ gun pack: " + pack.getName();
    }
}
