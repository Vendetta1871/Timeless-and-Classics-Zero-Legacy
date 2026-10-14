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
import java.util.Collections;
import java.util.Set;

/**
 * Serves the mod jar's own JSON language files ({@code assets/<domain>/lang/*.json}) as 1.12.2 {@code .lang}.
 */
@SideOnly(Side.CLIENT)
public class ModJsonLangPack implements IResourcePack {
    private final String domain;

    public ModJsonLangPack(String domain) {
        this.domain = domain;
    }

    @Nullable
    private InputStream openJson(ResourceLocation location) {
        if (!domain.equals(location.getNamespace()) || !JsonLang.isLangPath(location.getPath())) {
            return null;
        }
        return ModJsonLangPack.class.getResourceAsStream("/assets/" + domain + "/" + JsonLang.jsonPathFor(location.getPath()));
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream json = openJson(location);
        if (json == null) {
            throw new FileNotFoundException(location.toString());
        }
        try (InputStream in = json) {
            return new ByteArrayInputStream(JsonLang.toLegacyLang(in));
        }
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        InputStream json = openJson(location);
        if (json == null) {
            return false;
        }
        try {
            json.close();
        } catch (IOException ignored) {
        }
        return true;
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton(domain);
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
        return "TACZ JSON language bridge";
    }
}
