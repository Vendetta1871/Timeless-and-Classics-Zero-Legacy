package com.tacz.guns.legacy.tags;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.GunMod;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.20.1 {@code TagKey} for the few tags TACZ reads (1.12.2 has no tags). The contents come from the tag files in the
 * mod's own {@code data/<namespace>/tags/<blocks|entity_types>/} resources, the same files upstream ships. References to
 * other tags are resolved the same way, falling back to {@link VanillaTags} for vanilla 1.20.1 tags; 1.20.1 ids are
 * mapped with {@link VanillaTags#flatten}. Ids that do not exist in 1.12.2 simply never match.
 * {@code state.is(tag)} becomes {@code tag.contains(state.getBlock())}, {@code entity.getType().is(tag)} becomes
 * {@code tag.contains(entity)}.
 */
public final class TagKey<T> {
    private final TagType<T> type;
    private final ResourceLocation location;
    @Nullable
    private volatile Set<ResourceLocation> values;

    private TagKey(TagType<T> type, ResourceLocation location) {
        this.type = type;
        this.location = location;
    }

    public static <T> TagKey<T> create(TagType<T> type, ResourceLocation location) {
        return new TagKey<>(type, location);
    }

    public ResourceLocation location() {
        return location;
    }

    public boolean contains(@Nullable T value) {
        if (value == null) {
            return false;
        }
        ResourceLocation id = type.idOf(value);
        return id != null && values().contains(id);
    }

    public Set<ResourceLocation> values() {
        Set<ResourceLocation> result = values;
        if (result == null) {
            result = Collections.unmodifiableSet(resolve(type, location, new HashSet<>()));
            values = result;
        }
        return result;
    }

    private static Set<ResourceLocation> resolve(TagType<?> type, ResourceLocation tag, Set<ResourceLocation> visiting) {
        Set<ResourceLocation> result = new HashSet<>();
        if (!visiting.add(tag)) {
            return result;
        }
        String path = "/data/" + tag.getNamespace() + "/tags/" + type.getDirectory() + "/" + tag.getPath() + ".json";
        try (InputStream stream = TagKey.class.getResourceAsStream(path)) {
            if (stream == null) {
                result.addAll(VanillaTags.get(type, tag));
                return result;
            }
            JsonObject json = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray entries = json.getAsJsonArray("values");
            for (JsonElement entry : entries) {
                String value = entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString();
                if (value.startsWith("#")) {
                    result.addAll(resolve(type, new ResourceLocation(value.substring(1)), visiting));
                } else {
                    List<ResourceLocation> ids = VanillaTags.flatten(type, new ResourceLocation(value));
                    result.addAll(ids);
                }
            }
        } catch (Exception e) {
            GunMod.LOGGER.warn("Failed to read tag {} ({})", tag, path, e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "TagKey[" + type.getDirectory() + " / " + location + "]";
    }
}
