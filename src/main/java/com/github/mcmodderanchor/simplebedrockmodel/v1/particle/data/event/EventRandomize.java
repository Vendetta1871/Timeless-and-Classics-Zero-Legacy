package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

import java.util.List;

/**
 * 加权随机事件节点。按权重随机选择一个子事件执行。
 *
 * @param entries 加权事件条目列表
 */
public final class EventRandomize implements IEventNode {
    private final List<WeightedEntry> entries;

    public EventRandomize(List<WeightedEntry> entries) {
        this.entries = entries;
    }

    public List<WeightedEntry> entries() {
        return entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventRandomize)) return false;
        EventRandomize that = (EventRandomize) o;
        return java.util.Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(entries);
    }

    @Override
    public String toString() {
        return "EventRandomize[" + "entries=" + entries + "]";
    }


    /**
     * 加权事件条目。
     *
     * @param weight 权重
     * @param node   事件节点
     */
    public static final class WeightedEntry {
        private final float weight;
        private final IEventNode node;

        public WeightedEntry(float weight, IEventNode node) {
            this.weight = weight;
            this.node = node;
        }

        public float weight() {
            return weight;
        }

        public IEventNode node() {
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WeightedEntry)) return false;
            WeightedEntry that = (WeightedEntry) o;
            return java.util.Objects.equals(weight, that.weight) && java.util.Objects.equals(node, that.node);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(weight, node);
        }

        @Override
        public String toString() {
            return "WeightedEntry[" + "weight=" + weight + ", " + "node=" + node + "]";
        }
}
}
