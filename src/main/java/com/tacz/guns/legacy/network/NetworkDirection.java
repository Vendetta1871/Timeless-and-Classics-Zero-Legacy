package com.tacz.guns.legacy.network;

import net.minecraftforge.fml.relauncher.Side;

/**
 * 1.20.1 {@code NetworkDirection} (play phase only).
 */
public enum NetworkDirection {
    PLAY_TO_SERVER(Side.SERVER),
    PLAY_TO_CLIENT(Side.CLIENT);

    private final Side receptionSide;

    NetworkDirection(Side receptionSide) {
        this.receptionSide = receptionSide;
    }

    public Side getReceptionSide() {
        return receptionSide;
    }

    public Side getOriginationSide() {
        return receptionSide == Side.SERVER ? Side.CLIENT : Side.SERVER;
    }
}
