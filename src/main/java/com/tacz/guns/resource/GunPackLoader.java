package com.tacz.guns.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.legacy.resource.GunPackRepository;
import com.tacz.guns.legacy.util.FMLPaths;
import com.tacz.guns.util.GetJarResources;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 1.12.2: there are no pack repositories to add a {@code RepositorySource} to. {@link #discoverExtensions()} returns
 * the gun packs as a {@link GunPackRepository}; the client exposes its {@code assets} half to the vanilla resource
 * manager ({@code ClientGunPacks}) and to the client reload listeners, the server reads the {@code data} half.
 */
public enum GunPackLoader {
    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("GunPackFinder");
    /**
     * 1.12.2: the TACZ version this port mirrors. Gun packs declare their {@code tacz} dependency against upstream
     * versions, so it is checked against this instead of the port's own version.
     */
    public static final String UPSTREAM_VERSION = "1.1.8-hotfix2";
    private boolean firstLoad = true;

    public GunPackRepository discoverExtensions() {
        Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("tacz");
        File folder = resourcePacksPath.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                GunMod.LOGGER.warn(MARKER, "Failed to init tacz resource directory...", e);
                return GunPackRepository.empty();
            }
        }

        // 确保配置文件加载，这个阶段将比标准的forge配置文件加载早
        PreLoadConfig.load(resourcePacksPath);

        // 仅在第一次加载时复制默认资源包
        if (firstLoad) {
            if (!PreLoadConfig.override.get()) {
                for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES) {
                    GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), resourcePacksPath, entry.extraDirName());
                }
            }
            firstLoad = false;
        }

        GunMod.LOGGER.info(MARKER, "Start scanning for gun packs in {}", resourcePacksPath);
        List<GunPack> gunPacks = scanExtensions(resourcePacksPath);
        GunMod.LOGGER.info(MARKER, "Found {} possible gunpack(s) and added them to resource set.", gunPacks.size());
        List<GunPackRepository.PackSource> extensionPacks = new ArrayList<>();

        for (GunPack gunPack : gunPacks) {
            try {
                extensionPacks.add(GunPackRepository.PackSource.of(gunPack.name, gunPack.path));
            } catch (IOException e) {
                GunMod.LOGGER.error(MARKER, "Failed to open gun pack {}", gunPack.path.getFileName(), e);
            }
        }

        return new GunPackRepository(extensionPacks);
    }

    // 1.12.2: getModIcon (the pack.png of the merged "tacz_resources" pack) is not ported, there is no pack screen entry

    // 检查路径中的config.json
    // 应该不会在用这个了，先保留
//    private static RepositoryConfig checkConfig(Path resourcePacksPath) {
//        Path configPath = resourcePacksPath.resolve("config.json");
//        if (Files.exists(configPath)) {
//            try (InputStream stream = Files.newInputStream(configPath)) {
//                return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), RepositoryConfig.class);
//            } catch (IOException | JsonSyntaxException | JsonIOException e) {
//                GunMod.LOGGER.warn(MARKER, "Failed to read config json: {}", configPath);
//            }
//        }
//        // 不存在或者出问题了，新建一个
//        RepositoryConfig config = new RepositoryConfig(true);
//        // 使用Gson写文件
//        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
//            GSON.toJson(config, writer);
//        } catch (IOException e) {
//            GunMod.LOGGER.warn(MARKER, "Failed to init config json: {}", configPath);
//        }
//        return config;
//    }

    private static GunPack fromDirPath(Path path) throws IOException {
        Path packInfoFilePath = path.resolve("gunpack.meta.json");
        try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
            PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (info == null) {
                GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
                return null;
            }

            if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", packInfoFilePath.getFileName());
                return null;
            }

            return new GunPack(path, info.getName());
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException exception) {
            GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
            GunMod.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static GunPack fromZipPath(Path path)  {
        try(ZipFile zipFile = new ZipFile(path.toFile())){
            ZipEntry extDescriptorEntry = zipFile.getEntry("gunpack.meta.json");
            if (extDescriptorEntry == null) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No gunpack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(extDescriptorEntry)) {
                PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (info == null) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
                    return null;
                }

                if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                    GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                    return null;
                }

                return new GunPack(path, info.getName());
            } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException e) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
                return null;
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
            return null;
        }
    }

    private static List<GunPack> scanExtensions(Path extensionsPath) {
        List<GunPack> gunPacks = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)){
            for (Path entry : stream) {
                GunPack gunPack = null;
                if (Files.isDirectory(entry)) {
                    gunPack = fromDirPath(entry);
                } else if (entry.toString().endsWith(".zip")) {
                    gunPack = fromZipPath(entry);
                }
                if (gunPack != null) {
                    GunMod.LOGGER.info(MARKER, "- {}, Main namespace: {}", gunPack.path.getFileName(), gunPack.name);
                    gunPacks.add(gunPack);
                }
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER, "Failed to scan extensions from {}. Error: {}", extensionsPath, e);
        }

        return gunPacks;
    }

    private static boolean modVersionAllMatch(PackMeta info) throws InvalidVersionSpecificationException {
        HashMap<String, String> dependencies = info.getDependencies();
        for (String modId : dependencies.keySet()) {
            if (!modVersionMatch(modId, dependencies.get(modId))) {
                return false;
            }
        }
        return true;
    }

    private static boolean modVersionMatch(String modId, String version) throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);
        // 1.12.2: ModList -> Loader, and TACZ reports the upstream version it mirrors
        String modVersion = GunMod.MOD_ID.equals(modId) ? UPSTREAM_VERSION
                : Optional.ofNullable(Loader.instance().getIndexedModList().get(modId)).map(ModContainer::getVersion).orElse(null);
        return modVersion != null && versionRange.containsVersion(new DefaultArtifactVersion(modVersion));
    }


    public static final class GunPack {
        private final Path path;
        private final String name;

        public GunPack(Path path, String name) {
            this.path = path;
            this.name = name;
        }

        public Path path() {
            return path;
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GunPack)) return false;
            GunPack that = (GunPack) o;
            return Objects.equals(path, that.path) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, name);
        }

        @Override
        public String toString() {
            return "GunPack[path=" + path + ", name=" + name + "]";
        }
    }
}
