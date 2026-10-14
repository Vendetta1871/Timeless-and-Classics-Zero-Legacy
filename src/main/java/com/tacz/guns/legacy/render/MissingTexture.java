package com.tacz.guns.legacy.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stands in for {@code MissingTextureAtlasSprite.getLocation()}: in 1.12.2 the missing texture is only an atlas
 * sprite, so the standalone purple-black texture is registered under its own location on first use.
 */
@SideOnly(Side.CLIENT)
public final class MissingTexture {
    private static final ResourceLocation LOCATION = new ResourceLocation("tacz", "legacy/missing");
    private static boolean registered;

    private MissingTexture() {
    }

    public static ResourceLocation getLocation() {
        if (!registered) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(LOCATION, TextureUtil.MISSING_TEXTURE);
            registered = true;
        }
        return LOCATION;
    }
}
