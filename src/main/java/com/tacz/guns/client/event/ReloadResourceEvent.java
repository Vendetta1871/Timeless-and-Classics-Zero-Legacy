package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import net.minecraft.client.Minecraft;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class ReloadResourceEvent {
    public static final ResourceLocation BLOCK_ATLAS_TEXTURE = new ResourceLocation("textures/atlas/blocks.png");

    /**
     * 1.12.2: the block atlas is the only stitched texture map.
     */
    @SubscribeEvent
    public static void onTextureStitchEventPost(TextureStitchEvent.Post event) {
        if (event.getMap() == Minecraft.getMinecraft().getTextureMapBlocks()) {
            // InternalAssetLoader 需要加载一些默认的动画、模型，需要先于枪包加载。
            InternalAssetLoader.onResourceReload();
            SoundPlayManager.clearSoundResourceCache();
//            ClientReloadManager.reloadAllPack();
        }
    }
}
