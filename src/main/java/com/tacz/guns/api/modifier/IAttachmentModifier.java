package com.tacz.guns.api.modifier;

import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 配件属性修改器
 *
 * @param <T> Json 读取后处理的数据类型
 * @param <K> 配件缓存属性值
 */
public interface IAttachmentModifier<T, K> {
    /**
     * 配件属性修改器，同时也用于 Json 读取时作为字段名进行识别
     *
     * @return Json 读取时的字段名
     */
    String getId();

    /**
     * 可选字段，为了兼容旧版本 json 文件设立此方法
     *
     * @return 旧版本的配件属性修改的 json 字段名
     */
    default String getOptionalFields() {
        return StringUtils.EMPTY;
    }

    /**
     * 从 Json 读取数据
     *
     * @param json 输入的 json 字符串
     * @return 读取后经过处理的 json 对象
     */
    JsonProperty<T> readJson(String json);

    /**
     * 初始化缓存，用于填入枪械的默认数据
     *
     * @param gunItem 当前枪械物品
     * @param gunData 枪械数据
     * @return 初始化读取的数据
     */
    CacheValue<K> initCache(ItemStack gunItem, GunData gunData);

    /**
     * 计算，用于将各个配件的数据与枪械数据求值，最终计算出来
     *
     * @param modifiedValues 各个配件的数据值
     * @param cache          缓存的枪械默认数值
     */
    void eval(List<T> modifiedValues, CacheValue<K> cache);

    /**
     * 获取改装界面的配置属性条相关数据
     */
    @SideOnly(Side.CLIENT)
    default List<DiagramsData> getPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty) {
        return Collections.emptyList();
    }

    /**
     * 用于获取改装界面的配置属性条个数，用于按钮的偏移
     */
    @SideOnly(Side.CLIENT)
    default int getDiagramsDataSize() {
        return 0;
    }

    /**
     * 属性条数据
     *
     * @param defaultPercent   默认枪械值百分比
     * @param modifierPercent  修改值百分比
     * @param modifier         修改值，用于与默认值做对比判断
     * @param titleKey         属性名称语言文件 key
     * @param positivelyString 大于默认数值时，显示的文本
     * @param negativeString   小于默认数值时，显示的文本
     * @param defaultString    等于默认数值时，显示的文本
     * @param positivelyBetter true 时，大于默认数值显示为绿色，否则显示红色
     */
    @SideOnly(Side.CLIENT)
    final class DiagramsData {
        private final double defaultPercent;
        private final double modifierPercent;
        private final Number modifier;
        private final String titleKey;
        private final String positivelyString;
        private final String negativeString;
        private final String defaultString;
        private final boolean positivelyBetter;

        public DiagramsData(double defaultPercent, double modifierPercent, Number modifier, String titleKey, String positivelyString, String negativeString, String defaultString, boolean positivelyBetter) {
            this.defaultPercent = defaultPercent;
            this.modifierPercent = modifierPercent;
            this.modifier = modifier;
            this.titleKey = titleKey;
            this.positivelyString = positivelyString;
            this.negativeString = negativeString;
            this.defaultString = defaultString;
            this.positivelyBetter = positivelyBetter;
        }

        public double defaultPercent() {
            return defaultPercent;
        }

        public double modifierPercent() {
            return modifierPercent;
        }

        public Number modifier() {
            return modifier;
        }

        public String titleKey() {
            return titleKey;
        }

        public String positivelyString() {
            return positivelyString;
        }

        public String negativeString() {
            return negativeString;
        }

        public String defaultString() {
            return defaultString;
        }

        public boolean positivelyBetter() {
            return positivelyBetter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DiagramsData that = (DiagramsData) o;
            return Double.compare(defaultPercent, that.defaultPercent) == 0 && Double.compare(modifierPercent, that.modifierPercent) == 0 && java.util.Objects.equals(modifier, that.modifier) && java.util.Objects.equals(titleKey, that.titleKey) && java.util.Objects.equals(positivelyString, that.positivelyString) && java.util.Objects.equals(negativeString, that.negativeString) && java.util.Objects.equals(defaultString, that.defaultString) && positivelyBetter == that.positivelyBetter;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(defaultPercent, modifierPercent, modifier, titleKey, positivelyString, negativeString, defaultString, positivelyBetter);
        }

        @Override
        public String toString() {
            return "DiagramsData[" + "defaultPercent=" + defaultPercent + ", " + "modifierPercent=" + modifierPercent + ", " + "modifier=" + modifier + ", " + "titleKey=" + titleKey + ", " + "positivelyString=" + positivelyString + ", " + "negativeString=" + negativeString + ", " + "defaultString=" + defaultString + ", " + "positivelyBetter=" + positivelyBetter + "]";
        }
    }
}