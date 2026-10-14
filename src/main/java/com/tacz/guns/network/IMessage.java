package com.tacz.guns.network;

import com.tacz.guns.legacy.network.FriendlyByteBuf;
import com.tacz.guns.legacy.network.NetworkEvent;

import java.util.function.Supplier;

public interface IMessage<T> {
    void encode(T message, FriendlyByteBuf buffer);

    T decode(FriendlyByteBuf buffer);

    void handle(T message, Supplier<NetworkEvent.Context> supplier);
}