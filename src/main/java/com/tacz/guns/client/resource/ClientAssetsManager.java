package com.tacz.guns.client.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.util.EnumMapTypeAdapterFactory;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.api.vmlib.LuaAnimationConstant;
import com.tacz.guns.api.vmlib.LuaGunAnimationConstant;
import com.tacz.guns.api.vmlib.LuaLibrary;
import com.tacz.guns.client.resource.manager.DisplayManager;
import com.tacz.guns.client.resource.manager.GltfManager;
import com.tacz.guns.client.resource.manager.PackInfoManager;
import com.tacz.guns.client.resource.pojo.CommonTransformObject;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.client.resource.pojo.animation.bedrock.AnimationKeyframes;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.animation.bedrock.SoundEffectKeyframes;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.CubesItem;
import com.tacz.guns.client.resource.serialize.AnimationKeyframesSerializer;
import com.tacz.guns.client.resource.serialize.ItemStackSerializer;
import com.tacz.guns.client.resource.serialize.SoundEffectKeyframesSerializer;
import com.tacz.guns.client.resource.serialize.Vector3fSerializer;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.manager.LazyJsonDataManager;
import com.tacz.guns.resource.manager.ScriptManager;
import net.minecraft.client.Minecraft;
import com.tacz.guns.legacy.render.ItemTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import com.tacz.guns.legacy.resource.FileToIdConverter;
import net.minecraft.util.ResourceLocation;
import com.tacz.guns.legacy.resource.PreparableReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import com.google.common.collect.ImmutableList;

/**
 * 客户端资源管理器<br/>
 * 所有枪包资源缓存在此
 */
@SideOnly(Side.CLIENT)
public enum ClientAssetsManager {
    INSTANCE;
    public static final Gson GSON = new GsonBuilder()
            // 1.12.2: Gson 2.8.0 cannot create EnumMap fields on its own
            .registerTypeAdapterFactory(new EnumMapTypeAdapterFactory()).registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(CommonTransformObject.class, new CommonTransformObject.Serializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(AnimationKeyframes.class, new AnimationKeyframesSerializer())
            .registerTypeAdapter(SoundEffectKeyframes.class, new SoundEffectKeyframesSerializer())
            .registerTypeAdapter(ItemCameraTransforms.class, new ItemTransforms.Deserializer())
            .registerTypeAdapter(ItemTransformVec3f.class, new ItemTransforms.TransformDeserializer())
            .create();

    // 枪械展示数据
    private DisplayManager<GunDisplay> gunDisplay;
    // 弹药展示数据
    private DisplayManager<AmmoDisplay> ammoDisplay;
    // 配件展示数据
    private DisplayManager<AttachmentDisplay> attachmentDisplay;
    // 方块展示数据
    private DisplayManager<BlockDisplay> blockDisplay;
    // 原始基岩版模型
    private LazyJsonDataManager<BedrockModelPOJO> bedrockModel;
    // 基岩版模型动画
    private LazyJsonDataManager<BedrockAnimationFile> bedrockAnimation;
    // gltf 动画
    private GltfManager gltfAnimation;
    // 客户端脚本
    private final List<LuaLibrary> libList = ImmutableList.of(new LuaAnimationConstant(), new LuaGunAnimationConstant());
    private ScriptManager scriptManager;
    // 音效
    // 枪包元数据
    private PackInfoManager packInfo;

    private List<PreparableReloadListener> listeners;

    public void reloadAndRegister(Consumer<PreparableReloadListener> register) {
        if (listeners == null) {
            listeners = new ArrayList<>();
            gunDisplay = register(new DisplayManager<>(GunDisplay.class, GSON, "display/guns", "GunDisplayLoader"));
            ammoDisplay = register(new DisplayManager<>(AmmoDisplay.class, GSON, "display/ammo", "AmmoDisplayLoader"));
            attachmentDisplay = register(new DisplayManager<>(AttachmentDisplay.class, GSON, "display/attachments", "AttachmentDisplayLoader"));
            blockDisplay = register(new DisplayManager<>(BlockDisplay.class, GSON, "display/blocks", "BlockDisplayLoader"));

            bedrockModel = register(new LazyJsonDataManager<>(BedrockModelPOJO.class, GSON, "geo_models", "BedrockModelLoader",
                    id -> GunMod.MOD_ID.equals(id.getNamespace())));
            bedrockAnimation = register(new LazyJsonDataManager<>(BedrockAnimationFile.class, GSON, new FileToIdConverter("animations", ".animation.json"),
                    "BedrockAnimationLoader", id -> GunMod.MOD_ID.equals(id.getNamespace())));
            gltfAnimation = register(new GltfManager());
            scriptManager = register(new ScriptManager(new FileToIdConverter("scripts", ".lua"), libList));
            packInfo = register(new PackInfoManager());
            register(resourceManager -> ClientIndexManager.reload());
        }
        listeners.forEach(register);
    }

    private <T extends PreparableReloadListener> T register(T listener) {
        listeners.add(listener);
        return listener;
    }

    @Nullable
    public GunDisplay getGunDisplay(ResourceLocation id) {
        return gunDisplay.getData(id);
    }

    public Set<Map.Entry<ResourceLocation, GunDisplay>> getGunDisplays() {
        return gunDisplay.getAllData().entrySet();
    }

    public Set<ResourceLocation> getGunDisplayIds() {
        return gunDisplay.getAllData().keySet();
    }

    @Nullable
    public AttachmentDisplay getAttachmentDisplay(ResourceLocation id) {
        return attachmentDisplay.getData(id);
    }

    @Nullable
    public AmmoDisplay getAmmoDisplay(ResourceLocation id) {
        return ammoDisplay.getData(id);
    }

    @Nullable
    public BlockDisplay getBlockDisplay(ResourceLocation id) {
        return blockDisplay.getData(id);
    }

    @Nullable
    public BedrockModelPOJO getBedrockModelPOJO(ResourceLocation id) {
        return bedrockModel.getData(id);
    }

    @Nullable
    public BedrockAnimationFile getBedrockAnimations(ResourceLocation id) {
        return bedrockAnimation.getData(id);
    }

    @Nullable
    public LuaTable getScript(ResourceLocation id) {
        return scriptManager.getScript(id);
    }

    @Nullable
    public AnimationStructure getGltfAnimation(ResourceLocation id) {
        return gltfAnimation.getGltfAnimation(id);
    }

    @Nullable
    public PackInfo getPackInfo(String namespace) {
        return packInfo.getData(namespace);
    }

    @Nullable
    public PackInfo getPackInfo(@Nullable ResourceLocation namespace) {
        if (namespace == null) {
            return null;
        }
        return packInfo.getData(namespace.getNamespace());
    }

    @SideOnly(Side.CLIENT)
    public static void reloadAllPack() {
        // 1.12.2: the resource reload runs synchronously on the client thread
        Minecraft.getMinecraft().refreshResources();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            // 直接刷新data
            server.addScheduledTask(CommonAssetsManager::reloadAllPack);
        }
    }

}
