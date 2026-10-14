package com.tacz.guns.legacy.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 1.20.1 {@code net.minecraft.server.packs.resources.Resource}: one file inside one gun pack.
 */
public class Resource {
    private final String packId;
    private final IoSupplier<InputStream> streamSupplier;

    public Resource(String packId, IoSupplier<InputStream> streamSupplier) {
        this.packId = packId;
        this.streamSupplier = streamSupplier;
    }

    public String sourcePackId() {
        return packId;
    }

    public InputStream open() throws IOException {
        return streamSupplier.get();
    }

    public BufferedReader openAsReader() throws IOException {
        return new BufferedReader(new InputStreamReader(open(), StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    public interface IoSupplier<T> {
        T get() throws IOException;
    }
}
