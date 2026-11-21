package com.tacz.guns.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

public enum PegboardShape implements IStringSerializable {
    STRAIGHT("straight"),
    CORNER_LEFT("corner_left"),
    CORNER_RIGHT("corner_right");

    private final String name;

    PegboardShape(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return name;
    }

    /**
     * 1.12.2 {@link IStringSerializable} name (upstream {@code getSerializedName}).
     */
    @Override
    public String getName() {
        return name;
    }

    public static PegboardShape bySerializedName(String name) {
        for (PegboardShape shape : values()) {
            if (shape.name.equals(name)) {
                return shape;
            }
        }
        return STRAIGHT;
    }

    public EnumFacing getCornerFacing(EnumFacing facing) {
        return switch (this) {
            case CORNER_LEFT -> facing.rotateYCCW();
            case CORNER_RIGHT -> facing.rotateY();
            case STRAIGHT -> facing;
        };
    }
}
