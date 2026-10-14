package com.tacz.guns.legacy.resource;

import com.tacz.guns.GunMod;
import net.minecraft.util.ResourceLocation;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The set of loaded gun packs (folders and zips), bottom to top. Stands in for the vanilla pack repository that
 * TACZ hooks on 1.20.1: {@link #resourceManager(PackType)} lists and reads the {@code assets/} or {@code data/}
 * half of all packs. Paths are matched case-insensitively because 1.12.2 lower-cases resource locations.
 */
public final class GunPackRepository implements Closeable {
    private final List<PackSource> packs;

    public GunPackRepository(List<PackSource> packs) {
        this.packs = Collections.unmodifiableList(new ArrayList<>(packs));
    }

    public static GunPackRepository empty() {
        return new GunPackRepository(Collections.emptyList());
    }

    public List<PackSource> getPacks() {
        return packs;
    }

    public ResourceManager resourceManager(PackType type) {
        return new View(type);
    }

    @Override
    public void close() {
        for (PackSource pack : packs) {
            try {
                pack.close();
            } catch (IOException e) {
                GunMod.LOGGER.warn("Failed to close gun pack {}", pack.getName(), e);
            }
        }
    }

    private final class View implements ResourceManager {
        private final PackType type;

        private View(PackType type) {
            this.type = type;
        }

        private String filePath(ResourceLocation location) {
            return type.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath();
        }

        @Override
        public Set<String> getNamespaces() {
            Set<String> namespaces = new LinkedHashSet<>();
            for (PackSource pack : packs) {
                namespaces.addAll(pack.getNamespaces(type));
            }
            return namespaces;
        }

        @Override
        public Optional<Resource> getResource(ResourceLocation location) {
            String path = filePath(location);
            for (int i = packs.size() - 1; i >= 0; i--) {
                PackSource pack = packs.get(i);
                if (pack.hasFile(path)) {
                    return Optional.of(pack.resource(path));
                }
            }
            return Optional.empty();
        }

        @Override
        public List<Resource> getResourceStack(ResourceLocation location) {
            String path = filePath(location);
            List<Resource> stack = new ArrayList<>();
            for (PackSource pack : packs) {
                if (pack.hasFile(path)) {
                    stack.add(pack.resource(path));
                }
            }
            return stack;
        }

        @Override
        public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
            Map<ResourceLocation, Resource> result = new TreeMap<>(GunPackRepository::compareLocations);
            for (PackSource pack : packs) {
                pack.list(type, path, filter, (location, resource) -> result.put(location, resource));
            }
            return result;
        }

        @Override
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
            Map<ResourceLocation, List<Resource>> result = new TreeMap<>(GunPackRepository::compareLocations);
            for (PackSource pack : packs) {
                pack.list(type, path, filter, (location, resource) -> result.computeIfAbsent(location, k -> new ArrayList<>()).add(resource));
            }
            return result;
        }
    }

    private static int compareLocations(ResourceLocation a, ResourceLocation b) {
        int namespace = a.getNamespace().compareTo(b.getNamespace());
        return namespace != 0 ? namespace : a.getPath().compareTo(b.getPath());
    }

    /**
     * One gun pack: a folder or a zip file. Its file list is indexed once when opened.
     */
    public abstract static class PackSource implements Closeable {
        private final String name;
        private final Path path;
        /**
         * lower-cased path relative to the pack root → actual path
         */
        private final Map<String, String> files = new LinkedHashMap<>();

        protected PackSource(String name, Path path) {
            this.name = name;
            this.path = path;
        }

        public static PackSource of(String name, Path path) throws IOException {
            return Files.isDirectory(path) ? new DirSource(name, path) : new ZipSource(name, path);
        }

        public String getName() {
            return name;
        }

        public Path getPath() {
            return path;
        }

        protected void index(String actualPath) {
            files.put(actualPath.toLowerCase(Locale.ROOT), actualPath);
        }

        public boolean hasFile(String relativePath) {
            return files.containsKey(relativePath.toLowerCase(Locale.ROOT));
        }

        public Resource resource(String relativePath) {
            String actual = files.get(relativePath.toLowerCase(Locale.ROOT));
            return new Resource(name, () -> open(actual));
        }

        public Set<String> getNamespaces(PackType type) {
            String prefix = type.getDirectory() + "/";
            Set<String> namespaces = new LinkedHashSet<>();
            for (String file : files.keySet()) {
                if (file.startsWith(prefix)) {
                    int end = file.indexOf('/', prefix.length());
                    if (end > prefix.length()) {
                        namespaces.add(file.substring(prefix.length(), end));
                    }
                }
            }
            return namespaces;
        }

        void list(PackType type, String path, Predicate<ResourceLocation> filter, ListConsumer consumer) {
            String typePrefix = type.getDirectory() + "/";
            String lowerPath = path.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String file = entry.getKey();
                if (!file.startsWith(typePrefix)) {
                    continue;
                }
                int namespaceEnd = file.indexOf('/', typePrefix.length());
                if (namespaceEnd < 0) {
                    continue;
                }
                String namespace = file.substring(typePrefix.length(), namespaceEnd);
                String filePath = file.substring(namespaceEnd + 1);
                if (!filePath.startsWith(lowerPath + "/")) {
                    continue;
                }
                ResourceLocation location;
                try {
                    location = new ResourceLocation(namespace, filePath);
                } catch (RuntimeException e) {
                    continue;
                }
                if (filter.test(location)) {
                    String actual = entry.getValue();
                    consumer.accept(location, new Resource(name, () -> open(actual)));
                }
            }
        }

        protected abstract InputStream open(String actualPath) throws IOException;

        @FunctionalInterface
        interface ListConsumer {
            void accept(ResourceLocation location, Resource resource);
        }
    }

    private static final class DirSource extends PackSource {
        DirSource(String name, Path root) throws IOException {
            super(name, root);
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile).forEach(file ->
                        index(root.relativize(file).toString().replace('\\', '/')));
            }
        }

        @Override
        protected InputStream open(String actualPath) throws IOException {
            return Files.newInputStream(getPath().resolve(actualPath));
        }

        @Override
        public void close() {
        }
    }

    private static final class ZipSource extends PackSource {
        private final ZipFile zipFile;
        private final Map<String, ZipEntry> entries = new HashMap<>();

        ZipSource(String name, Path zip) throws IOException {
            super(name, zip);
            this.zipFile = new ZipFile(zip.toFile());
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), entry);
                    index(entry.getName());
                }
            }
        }

        @Override
        protected InputStream open(String actualPath) throws IOException {
            ZipEntry entry = entries.get(actualPath);
            if (entry == null) {
                throw new IOException("No entry " + actualPath + " in " + getPath());
            }
            return zipFile.getInputStream(entry);
        }

        @Override
        public void close() throws IOException {
            zipFile.close();
        }
    }
}
