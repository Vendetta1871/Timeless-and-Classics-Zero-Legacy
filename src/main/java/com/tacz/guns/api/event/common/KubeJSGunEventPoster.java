package com.tacz.guns.api.event.common;

// TODO(port): needs TimelessClientEvents / TimelessCommonEvents / TimelessServerEvents (compat/kubejs, no KubeJS on 1.12.2)
// import com.tacz.guns.compat.kubejs.events.TimelessClientEvents;
// import com.tacz.guns.compat.kubejs.events.TimelessCommonEvents;
// import com.tacz.guns.compat.kubejs.events.TimelessServerEvents;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.Loader;

public interface KubeJSGunEventPoster<E extends Event> {
    default void postEventToKubeJS(E event) {
        if (Loader.isModLoaded("kubejs")) {
            // TODO(port): needs TimelessCommonEvents
            // TimelessCommonEvents.INSTANCE.postKubeJSEvent(event);
        }
    }

    //客户端事件应调用此方法
    default void postClientEventToKubeJS(E event) {
        if (Loader.isModLoaded("kubejs")) {
            // TODO(port): needs TimelessClientEvents
            // TimelessClientEvents.INSTANCE.postKubeJSEvent(event);
        }
    }

    //服务端事件应调用此方法
    default void postServerEventToKubeJS(E event) {
        if (Loader.isModLoaded("kubejs")) {
            // TODO(port): needs TimelessServerEvents
            // TimelessServerEvents.INSTANCE.postKubeJSEvent(event);
        }
    }
}
