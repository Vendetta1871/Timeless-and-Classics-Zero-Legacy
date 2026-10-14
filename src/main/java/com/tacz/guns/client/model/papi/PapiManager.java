package com.tacz.guns.client.model.papi;

import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public final class PapiManager {
    private static final Map<String, Function<ItemStack, String>> PAPI = Maps.newHashMap();

    // 注册，不知道放哪里，先放这
    static {
        addPapi(PlayerNamePapi.NAME, new PlayerNamePapi());
        addPapi(AmmoCountPapi.NAME, new AmmoCountPapi());
    }

    public static void addPapi(String textKey, Function<ItemStack, String> function) {
        textKey = "%" + textKey + "%";
        PAPI.put(textKey, function);
    }

    public static String getTextShow(String textKey, ItemStack stack) {
        // Raw lookup without String.format, like upstream Language#getOrDefault (placeholders contain '%')
        String text = net.minecraft.util.text.translation.I18n.translateToLocal(textKey);
        for (var entry : PAPI.entrySet()) {
            String placeholder = entry.getKey();
            String data = entry.getValue().apply(stack);
            text = text.replace(placeholder, data);
        }
        return text;
    }
}
