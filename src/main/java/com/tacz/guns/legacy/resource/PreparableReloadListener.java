package com.tacz.guns.legacy.resource;

/**
 * 1.20.1 {@code PreparableReloadListener}, run synchronously: TACZ reloads gun packs as a whole on the client
 * (resource reload) and on the server (start, {@code /tacz reload}).
 */
public interface PreparableReloadListener {
    void reload(ResourceManager resourceManager);

    default String getName() {
        return getClass().getSimpleName();
    }
}
