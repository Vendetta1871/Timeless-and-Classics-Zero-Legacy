package com.tacz.guns.legacy.config;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Small stand-in for 1.20.1 {@code net.minecraftforge.common.ForgeConfigSpec}, built on Forge 1.12's
 * {@link Configuration}. It keeps the builder/value shape used by the upstream config classes
 * ({@code comment/push/pop/define/defineInRange/defineEnum/defineList/build}, {@code ConfigValue.get()}).
 * <p>
 * Values are declared with a {@link Builder} first (nothing touches the disk), then {@link #load(File)} reads the
 * config file, binds every declared value to it and writes the file back (so defaults and comments appear).
 * Until the spec is loaded, {@code get()} returns the default value instead of throwing.
 */
public class ForgeConfigSpec {
    private static final Gson GSON = new Gson();

    private final List<ConfigValue<?>> values;
    private Configuration configuration;
    private File file;

    private ForgeConfigSpec(List<ConfigValue<?>> values) {
        this.values = Collections.unmodifiableList(values);
    }

    public boolean isLoaded() {
        return this.configuration != null;
    }

    public File getFile() {
        return this.file;
    }

    /**
     * Loads (or creates) the given config file, applies it to every value of this spec and saves it again.
     */
    public synchronized void load(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        this.file = file;
        for (ConfigValue<?> value : this.values) {
            value.bind(configuration);
        }
        this.configuration = configuration;
        configuration.save();
    }

    /**
     * Writes the current values back to the file that was passed to {@link #load(File)}.
     */
    public synchronized void save() {
        if (this.configuration != null) {
            this.configuration.save();
        }
    }

    public static class Builder {
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();
        private String comment;

        public Builder comment(String comment) {
            this.comment = this.comment == null ? comment : this.comment + "\n" + comment;
            return this;
        }

        public Builder comment(String... comment) {
            return this.comment(String.join("\n", comment));
        }

        public Builder translation(String translationKey) {
            return this;
        }

        public Builder worldRestart() {
            return this;
        }

        public Builder push(String path) {
            this.path.add(path);
            return this;
        }

        public Builder push(List<String> path) {
            this.path.addAll(path);
            return this;
        }

        public Builder pop() {
            return this.pop(1);
        }

        public Builder pop(int count) {
            if (count > this.path.size()) {
                throw new IllegalArgumentException("Attempted to pop " + count + " elements when we only had: " + this.path);
            }
            for (int i = 0; i < count; i++) {
                this.path.remove(this.path.size() - 1);
            }
            return this;
        }

        public <T> ConfigValue<T> define(String path, T defaultValue) {
            return this.add(new ConfigValue<>(this.category(), path, this.takeComment(), defaultValue));
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return this.add(new BooleanValue(this.category(), path, this.takeComment(), defaultValue));
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return this.add(new IntValue(this.category(), path, this.takeComment(), defaultValue, min, max));
        }

        public LongValue defineInRange(String path, long defaultValue, long min, long max) {
            return this.add(new LongValue(this.category(), path, this.takeComment(), defaultValue, min, max));
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return this.add(new DoubleValue(this.category(), path, this.takeComment(), defaultValue, min, max));
        }

        public <V extends Enum<V>> EnumValue<V> defineEnum(String path, V defaultValue) {
            return this.add(new EnumValue<>(this.category(), path, this.takeComment(), defaultValue));
        }

        public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
            return this.add(new ConfigValue<List<? extends T>>(this.category(), path, this.takeComment(), defaultValue) {
                @Override
                @SuppressWarnings("unchecked")
                protected List<? extends T> readList(List<?> raw) {
                    List<T> filtered = new ArrayList<>();
                    for (Object element : raw) {
                        if (elementValidator.test(element)) {
                            filtered.add((T) element);
                        }
                    }
                    return filtered;
                }
            });
        }

        public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> defaultValue) {
            return this.defineList(path, defaultValue, e -> true);
        }

        public ForgeConfigSpec build() {
            return new ForgeConfigSpec(new ArrayList<>(this.values));
        }

        private <V extends ConfigValue<?>> V add(V value) {
            this.values.add(value);
            return value;
        }

        private String category() {
            return String.join(".", this.path);
        }

        private String takeComment() {
            String comment = this.comment;
            this.comment = null;
            return comment;
        }
    }

    public static class ConfigValue<T> implements Supplier<T> {
        protected final String category;
        protected final String name;
        protected final String comment;
        protected final T defaultValue;
        private volatile T value;
        private Property property;

        ConfigValue(String category, String name, String comment, T defaultValue) {
            this.category = category;
            this.name = name;
            this.comment = comment == null ? "" : comment;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public List<String> getPath() {
            List<String> path = new ArrayList<>();
            if (!this.category.isEmpty()) {
                path.addAll(Arrays.asList(this.category.split("\\.")));
            }
            path.add(this.name);
            return path;
        }

        public T getDefault() {
            return this.defaultValue;
        }

        @Override
        public T get() {
            return this.value;
        }

        public void set(T value) {
            this.value = value;
            if (this.property != null) {
                this.write(this.property, value);
            }
        }

        /**
         * Only pushes the current value into the in-memory {@link Property}: call {@link ForgeConfigSpec#save()}
         * afterwards to write the file.
         */
        public void save() {
            if (this.property != null) {
                this.write(this.property, this.value);
            }
        }

        // ---- binding to Forge's Configuration ----

        @SuppressWarnings("unchecked")
        void bind(Configuration configuration) {
            Object def = this.defaultValue;
            if (def instanceof Boolean) {
                this.property = configuration.get(this.category, this.name, (Boolean) def, this.comment);
                this.value = (T) Boolean.valueOf(this.property.getBoolean((Boolean) def));
            } else if (def instanceof Integer) {
                int min = this.min().intValue();
                int max = this.max().intValue();
                this.property = configuration.get(this.category, this.name, (Integer) def, this.comment, min, max);
                this.value = (T) Integer.valueOf(Math.max(min, Math.min(max, this.property.getInt((Integer) def))));
            } else if (def instanceof Long) {
                long min = this.min().longValue();
                long max = this.max().longValue();
                this.property = configuration.get(this.category, this.name, Long.toString((Long) def), this.rangeComment(min, max));
                long parsed;
                try {
                    parsed = Long.parseLong(this.property.getString().trim());
                } catch (NumberFormatException e) {
                    parsed = (Long) def;
                }
                this.value = (T) Long.valueOf(Math.max(min, Math.min(max, parsed)));
            } else if (def instanceof Double) {
                double min = this.min().doubleValue();
                double max = this.max().doubleValue();
                this.property = configuration.get(this.category, this.name, (Double) def, this.comment, min, max);
                this.value = (T) Double.valueOf(Math.max(min, Math.min(max, this.property.getDouble((Double) def))));
            } else if (def instanceof Enum) {
                Enum<?>[] constants = ((Enum<?>) def).getDeclaringClass().getEnumConstants();
                String[] names = new String[constants.length];
                for (int i = 0; i < constants.length; i++) {
                    names[i] = constants[i].name();
                }
                this.property = configuration.get(this.category, this.name, ((Enum<?>) def).name(), this.comment, names);
                Object parsed = def;
                String raw = this.property.getString().trim().toUpperCase(Locale.ROOT);
                for (Enum<?> constant : constants) {
                    if (constant.name().equals(raw)) {
                        parsed = constant;
                        break;
                    }
                }
                this.value = (T) parsed;
            } else if (def instanceof String) {
                this.property = configuration.get(this.category, this.name, (String) def, this.comment);
                this.value = (T) this.property.getString();
            } else if (def instanceof List) {
                List<?> defList = (List<?>) def;
                String[] defaults = new String[defList.size()];
                for (int i = 0; i < defaults.length; i++) {
                    defaults[i] = encodeElement(defList.get(i));
                }
                this.property = configuration.get(this.category, this.name, defaults, this.comment);
                List<Object> raw = new ArrayList<>();
                for (String element : this.property.getStringList()) {
                    raw.add(decodeElement(element));
                }
                this.value = (T) this.readList(raw);
            } else {
                throw new IllegalArgumentException("Unsupported config value type: " + (def == null ? null : def.getClass()));
            }
        }

        @SuppressWarnings("unchecked")
        private void write(Property property, T value) {
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                String[] encoded = new String[list.size()];
                for (int i = 0; i < encoded.length; i++) {
                    encoded[i] = encodeElement(list.get(i));
                }
                property.set(encoded);
            } else if (value instanceof Enum) {
                property.set(((Enum<?>) value).name());
            } else {
                property.set(String.valueOf(value));
            }
        }

        protected List<?> readList(List<?> raw) {
            return raw;
        }

        Number min() {
            return Double.NEGATIVE_INFINITY;
        }

        Number max() {
            return Double.POSITIVE_INFINITY;
        }

        private String rangeComment(long min, long max) {
            String range = "[range: " + min + " ~ " + max + "]";
            return this.comment.isEmpty() ? range : this.comment + "\n" + range;
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        BooleanValue(String category, String name, String comment, boolean defaultValue) {
            super(category, name, comment, defaultValue);
        }
    }

    public static class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        IntValue(String category, String name, String comment, int defaultValue, int min, int max) {
            super(category, name, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        Number min() {
            return this.min;
        }

        @Override
        Number max() {
            return this.max;
        }
    }

    public static class LongValue extends ConfigValue<Long> {
        private final long min;
        private final long max;

        LongValue(String category, String name, String comment, long defaultValue, long min, long max) {
            super(category, name, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        Number min() {
            return this.min;
        }

        @Override
        Number max() {
            return this.max;
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(String category, String name, String comment, double defaultValue, double min, double max) {
            super(category, name, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        Number min() {
            return this.min;
        }

        @Override
        Number max() {
            return this.max;
        }
    }

    public static class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
        EnumValue(String category, String name, String comment, T defaultValue) {
            super(category, name, comment, defaultValue);
        }
    }

    // ---- list element (de)serialization ----
    // A config list is stored as a Forge string list. Plain strings are stored as they are, nested lists
    // (e.g. List<List<String>>) are stored as a JSON array of strings.

    private static String encodeElement(Object element) {
        if (element instanceof List) {
            return GSON.toJson(element);
        }
        return String.valueOf(element);
    }

    private static Object decodeElement(String element) {
        String trimmed = element.trim();
        if (trimmed.startsWith("[\"") && trimmed.endsWith("]")) {
            try {
                JsonElement json = new JsonParser().parse(trimmed);
                if (json.isJsonArray()) {
                    JsonArray array = json.getAsJsonArray();
                    List<String> list = Lists.newArrayList();
                    for (JsonElement e : array) {
                        list.add(e.getAsString());
                    }
                    return list;
                }
            } catch (RuntimeException ignored) {
                // not a nested list, keep the raw string
            }
        }
        return element;
    }
}
