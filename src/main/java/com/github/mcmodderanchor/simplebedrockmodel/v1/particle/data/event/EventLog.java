package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

/**
 * 日志事件节点。触发时输出日志消息。
 *
 * @param message 日志消息
 */
public final class EventLog implements IEventNode {
    private final String message;

    public EventLog(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventLog)) return false;
        EventLog that = (EventLog) o;
        return java.util.Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(message);
    }

    @Override
    public String toString() {
        return "EventLog[" + "message=" + message + "]";
    }

}
