package com.tacz.guns.legacy.util;

/**
 * 1.20 {@code net.minecraft.world.inventory.ClickAction}: the mouse button of a container slot click
 * (left = {@code PRIMARY}, right = {@code SECONDARY}).
 */
public enum ClickAction {
    PRIMARY,
    SECONDARY;

    public static ClickAction of(int button) {
        return button == 0 ? PRIMARY : SECONDARY;
    }
}
