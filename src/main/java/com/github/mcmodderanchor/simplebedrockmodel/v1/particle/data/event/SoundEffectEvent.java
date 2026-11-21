package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

/**
 * 音效事件节点。触发时播放音效。
 *
 * @param eventName 音效事件名称
 */
public final class SoundEffectEvent implements IEventNode {
    private final String eventName;

    public SoundEffectEvent(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoundEffectEvent)) return false;
        SoundEffectEvent that = (SoundEffectEvent) o;
        return java.util.Objects.equals(eventName, that.eventName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(eventName);
    }

    @Override
    public String toString() {
        return "SoundEffectEvent[" + "eventName=" + eventName + "]";
    }

}
