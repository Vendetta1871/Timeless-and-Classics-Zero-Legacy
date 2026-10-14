package com.tacz.guns.api;

import com.google.common.reflect.TypeToken;
import com.tacz.guns.legacy.util.Util;

public final class GunProperty<T> {
    private final String name;
    private final Class<T> type;

    public GunProperty(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    public static <T> GunProperty<T> of(String name, Class<T> type) {
        return Util.make(new GunProperty<>(name, type), property -> GunProperties.ALL.put(name, property));
    }

    @SuppressWarnings("unchecked")
    public static <T> GunProperty<T> of(String name, TypeToken<T> type) {
        return Util.make(new GunProperty<>(name, (Class<T>) type.getRawType()), property -> GunProperties.ALL.put(name, property));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GunProperty<?> that = (GunProperty<?>) o;
        return java.util.Objects.equals(name, that.name) && java.util.Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "GunProperty[" + "name=" + name + ", " + "type=" + type + "]";
    }
}
