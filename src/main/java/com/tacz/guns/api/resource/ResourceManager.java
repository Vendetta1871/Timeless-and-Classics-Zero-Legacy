package com.tacz.guns.api.resource;


import com.google.common.collect.Lists;
import com.tacz.guns.GunMod;

import java.nio.file.Paths;
import java.util.List;

/**
 * 用于注册需要解压释放的枪包，为其他附属模组使用
 */
public final class ResourceManager {
    /**
     * 存放所有待解压枪包路径
     */
    public static final List<ExtraEntry> EXTRA_ENTRIES = Lists.newArrayList();


    /** @deprecated 不再使用旧的文件入口。现在你可以直接将assets和data内置在模组中，或者使用下面的新方法导出
     *
     * @param modMainClass    附属模组的主类
     * @param extraFolderPath 需要解压的文件夹，比如 TACZ 自己就是 /assets/tacz/custom/tacz_default_gun <br>
     *                        这表示把 tacz_default_gun 文件夹解压出来，放置到枪械包安装目录下
     */
    @Deprecated
    public static void registerExtraGunPack(Class<?> modMainClass, String extraFolderPath) {
        GunMod.LOGGER.warn("some mod is using deprecated method to export gun pack, notifying the mod author to update it: {}", extraFolderPath);
    }

    public static void registerExportResource(Class<?> modMainClass, String extraFolderPath) {
        EXTRA_ENTRIES.add(new ExtraEntry(modMainClass, extraFolderPath, Paths.get(extraFolderPath).getFileName().toString()));
    }

    /**
     * 解压条目
     */
    public static final class ExtraEntry {
        private final Class<?> modMainClass;
        private final String srcPath;
        private final String extraDirName;

        public ExtraEntry(Class<?> modMainClass, String srcPath, String extraDirName) {
            this.modMainClass = modMainClass;
            this.srcPath = srcPath;
            this.extraDirName = extraDirName;
        }

        public Class<?> modMainClass() {
            return modMainClass;
        }

        public String srcPath() {
            return srcPath;
        }

        public String extraDirName() {
            return extraDirName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtraEntry that = (ExtraEntry) o;
            return java.util.Objects.equals(modMainClass, that.modMainClass) && java.util.Objects.equals(srcPath, that.srcPath) && java.util.Objects.equals(extraDirName, that.extraDirName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(modMainClass, srcPath, extraDirName);
        }

        @Override
        public String toString() {
            return "ExtraEntry[" + "modMainClass=" + modMainClass + ", " + "srcPath=" + srcPath + ", " + "extraDirName=" + extraDirName + "]";
        }
    }
}
