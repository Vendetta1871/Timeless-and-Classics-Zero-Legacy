package com.tacz.guns.client.model.papi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;

import java.util.function.Function;

public class PlayerNamePapi implements Function<ItemStack, String> {
    public static final String NAME = "player_name";

    @Override
    public String apply(ItemStack stack) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) {
            return player.getName();
        }
        return "";
    }
}
