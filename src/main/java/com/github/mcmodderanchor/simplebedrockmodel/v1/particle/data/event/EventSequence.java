package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

import java.util.List;

/**
 * 顺序事件节点。按顺序执行子事件列表。
 *
 * @param nodes 子事件节点列表
 */
public final class EventSequence implements IEventNode {
    private final List<IEventNode> nodes;

    public EventSequence(List<IEventNode> nodes) {
        this.nodes = nodes;
    }

    public List<IEventNode> nodes() {
        return nodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventSequence)) return false;
        EventSequence that = (EventSequence) o;
        return java.util.Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(nodes);
    }

    @Override
    public String toString() {
        return "EventSequence[" + "nodes=" + nodes + "]";
    }

}
