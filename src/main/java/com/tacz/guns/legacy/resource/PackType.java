package com.tacz.guns.legacy.resource;

/**
 * 1.20.1 {@code net.minecraft.server.packs.PackType}: which half of a gun pack is read.
 */
public enum PackType {
    CLIENT_RESOURCES("assets"),
    SERVER_DATA("data");

    private final String directory;

    PackType(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}
