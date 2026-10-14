package com.tacz.guns.legacy.util;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Static helpers that 1.20.1's {@code ResourceLocation} has and 1.12.2's does not.
 * <p>
 * Note: {@code new ResourceLocation(String)} in 1.12.2 never throws for invalid characters (1.20.1 throws a
 * ResourceLocationException); use {@link #tryParse(String)} wherever upstream does.
 */
public final class ResourceLocations {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]*");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    private ResourceLocations() {
    }

    /**
     * 1.20.1 {@code ResourceLocation.tryParse}: null when the string is not a valid location.
     */
    @Nullable
    public static ResourceLocation tryParse(String string) {
        if (string == null) {
            return null;
        }
        int index = string.indexOf(':');
        String namespace = index >= 0 ? string.substring(0, index) : "minecraft";
        String path = index >= 0 ? string.substring(index + 1) : string;
        if (namespace.isEmpty()) {
            namespace = "minecraft";
        }
        return isValidNamespace(namespace) && isValidPath(path) ? new ResourceLocation(namespace, path) : null;
    }

    /**
     * 1.20.1 {@code ResourceLocation.isValidResourceLocation}.
     */
    public static boolean isValidResourceLocation(String string) {
        return tryParse(string) != null;
    }

    public static boolean isValidNamespace(String namespace) {
        return NAMESPACE.matcher(namespace).matches();
    }

    public static boolean isValidPath(String path) {
        return PATH.matcher(path).matches();
    }
}
