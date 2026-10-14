package com.tacz.guns.api.modifier;

import net.minecraft.util.text.ITextComponent;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 配件从 Json 读取的数据
 *
 * @param <T> Json 读取后转换成的中间数据类型
 */
public abstract class JsonProperty<T> {
    protected List<ITextComponent> components = Lists.newArrayList();
    private @Nullable T value;

    public JsonProperty(@Nullable T value) {
        this.value = value;
    }

    @Nullable
    public T getValue() {
        return value;
    }

    public void setValue(@Nullable T value) {
        this.value = value;
    }

    public List<ITextComponent> getComponents() {
        return components;
    }

    /**
     * 初始化文本提示，用于配件的描述文本
     */
    public abstract void initComponents();
}
