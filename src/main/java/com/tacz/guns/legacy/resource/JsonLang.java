package com.tacz.guns.legacy.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Converts a 1.13+ JSON language file to 1.12.2 {@code .lang} text in memory. The output starts with Forge's
 * {@code #PARSE_ESCAPES} marker, so values are read with {@link Properties} escaping (newlines, unicode) intact.
 */
public final class JsonLang {
    private JsonLang() {
    }

    public static String jsonPathFor(String langPath) {
        return langPath.substring(0, langPath.length() - ".lang".length()) + ".json";
    }

    public static boolean isLangPath(String path) {
        return path.startsWith("lang/") && path.endsWith(".lang");
    }

    /**
     * 1.12.2 names items {@code item.<key>.name} and blocks {@code tile.<key>.name}, where 1.20.1 uses
     * {@code item.<namespace>.<path>} and {@code block.<namespace>.<path>}; the 1.12 key is added next to the original.
     */
    static String legacyNameKey(String key) {
        if (key.startsWith("item.") && key.chars().filter(c -> c == '.').count() == 2) {
            return key + ".name";
        }
        if (key.startsWith("block.") && key.chars().filter(c -> c == '.').count() == 2) {
            return "tile." + key.substring("block.".length()) + ".name";
        }
        if (key.startsWith("entity.") && key.chars().filter(c -> c == '.').count() == 2) {
            return "entity." + key.substring(key.lastIndexOf('.') + 1) + ".name";
        }
        return null;
    }

    public static byte[] toLegacyLang(InputStream json) throws IOException {
        Properties properties = new Properties();
        try (JsonReader reader = new JsonReader(new InputStreamReader(json, StandardCharsets.UTF_8))) {
            reader.setLenient(true);
            JsonElement root = new JsonParser().parse(reader);
            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        properties.setProperty(entry.getKey(), entry.getValue().getAsString());
                        String legacyKey = legacyNameKey(entry.getKey());
                        if (legacyKey != null && !object.has(legacyKey)) {
                            properties.setProperty(legacyKey, entry.getValue().getAsString());
                        }
                    }
                }
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write("#PARSE_ESCAPES\n");
            properties.store(writer, null);
        }
        return out.toByteArray();
    }
}
