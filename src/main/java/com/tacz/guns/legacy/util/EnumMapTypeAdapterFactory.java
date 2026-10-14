package com.tacz.guns.legacy.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

/**
 * Gson 2.8.0 (bundled with 1.12.2) builds a {@code LinkedHashMap} for {@code EnumMap} fields and then fails to assign
 * it; newer Gson versions, which upstream relies on, create the {@code EnumMap} directly. Registered on TACZ's Gson
 * instances.
 */
public final class EnumMapTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != EnumMap.class || !(type.getType() instanceof ParameterizedType)) {
            return null;
        }
        Type[] arguments = ((ParameterizedType) type.getType()).getActualTypeArguments();
        Class<? extends Enum> keyClass = (Class<? extends Enum>) TypeToken.get(arguments[0]).getRawType();
        TypeAdapter<Map<?, ?>> delegate = (TypeAdapter<Map<?, ?>>) gson.getAdapter(TypeToken.getParameterized(Map.class, arguments[0], arguments[1]));
        return (TypeAdapter<T>) new TypeAdapter<EnumMap>() {
            @Override
            public void write(JsonWriter out, EnumMap value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public EnumMap read(JsonReader in) throws IOException {
                Map<?, ?> map = delegate.read(in);
                if (map == null) {
                    return null;
                }
                EnumMap result = new EnumMap(keyClass);
                result.putAll(map);
                return result;
            }
        }.nullSafe();
    }
}
