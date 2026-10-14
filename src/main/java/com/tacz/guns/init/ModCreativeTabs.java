package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.AmmoBoxItem;
import com.tacz.guns.item.AmmoItem;
import com.tacz.guns.item.AttachmentItem;
import com.tacz.guns.item.GunSmithTableItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import com.tacz.guns.legacy.registry.DeferredRegister;
import com.tacz.guns.legacy.registry.RegistryObject;
import com.tacz.guns.legacy.util.Components;

@SuppressWarnings("all")
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeTabs> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GunMod.MOD_ID);

    public static RegistryObject<CreativeTabs> OTHER_TAB = TABS.register("other", () -> CreativeTabs.builder()
            .title(Components.translatable("itemGroup.tab.tacz.other"))
            .icon(() -> ModItems.GUN_SMITH_TABLE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.acceptAll(GunSmithTableItem.fillItemCategory());
                output.accept(ModItems.TARGET.get());
                output.accept(ModItems.STATUE.get());
                output.accept(ModItems.TARGET_MINECART.get());
                AmmoBoxItem.fillItemCategory(output);
            }).build());

    public static RegistryObject<CreativeTabs> AMMO_TAB = TABS.register("ammo", () -> CreativeTabs.builder()
            .title(Components.translatable("itemGroup.tab.tacz.ammo")).withTabsBefore(OTHER_TAB.getId())
            .icon(() -> AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build())
            .displayItems((parameters, output) -> output.acceptAll(AmmoItem.fillItemCategory())).build());

    public static RegistryObject<CreativeTabs> ATTACHMENT_SCOPE_TAB = TABS.register("scope", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.scope.name")).withTabsBefore(AMMO_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "scope_acog_ta31")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.SCOPE))).build());

    public static RegistryObject<CreativeTabs> ATTACHMENT_MUZZLE_TAB = TABS.register("muzzle", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.muzzle.name")).withTabsBefore(ATTACHMENT_SCOPE_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "muzzle_compensator_trident")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.MUZZLE))).build());

    public static RegistryObject<CreativeTabs> ATTACHMENT_STOCK_TAB = TABS.register("stock", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.stock.name")).withTabsBefore(ATTACHMENT_MUZZLE_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "stock_militech_b5")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.STOCK))).build());

    public static RegistryObject<CreativeTabs> ATTACHMENT_GRIP_TAB = TABS.register("grip", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.grip.name")).withTabsBefore(ATTACHMENT_STOCK_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "grip_magpul_afg_2")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.GRIP))).build());

    public static RegistryObject<CreativeTabs> ATTACHMENT_EXTENDED_MAG_TAB = TABS.register("extended_mag", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.extended_mag.name")).withTabsBefore(ATTACHMENT_GRIP_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "extended_mag_3")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.EXTENDED_MAG))).build());

    public static RegistryObject<CreativeTabs> ATTACHMENT_LASER_TAB = TABS.register("laser", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.laser.name")).withTabsBefore(ATTACHMENT_EXTENDED_MAG_TAB.getId())
            .icon(() -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "laser_compact")).build())
            .displayItems((parameters, output) -> output.acceptAll(AttachmentItem.fillItemCategory(AttachmentType.LASER))).build());

    public static RegistryObject<CreativeTabs> GUN_PISTOL_TAB = TABS.register("pistol", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.pistol.name")).withTabsBefore(ATTACHMENT_LASER_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "glock_17")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.PISTOL))).build());

    public static RegistryObject<CreativeTabs> GUN_SNIPER_TAB = TABS.register("sniper", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.sniper.name")).withTabsBefore(GUN_PISTOL_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ai_awp")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SNIPER))).build());

    public static RegistryObject<CreativeTabs> GUN_RIFLE_TAB = TABS.register("rifle", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.rifle.name")).withTabsBefore(GUN_SNIPER_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ak47")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.RIFLE))).build());

    public static RegistryObject<CreativeTabs> GUN_SHOTGUN_TAB = TABS.register("shotgun", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.shotgun.name")).withTabsBefore(GUN_RIFLE_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "db_short")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SHOTGUN))).build());

    public static RegistryObject<CreativeTabs> GUN_SMG_TAB = TABS.register("smg", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.smg.name")).withTabsBefore(GUN_SHOTGUN_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "hk_mp5a5")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.SMG))).build());

    public static RegistryObject<CreativeTabs> GUN_RPG_TAB = TABS.register("rpg", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.rpg.name")).withTabsBefore(GUN_SMG_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "rpg7")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.RPG))).build());

    public static RegistryObject<CreativeTabs> GUN_MG_TAB = TABS.register("mg", () -> CreativeTabs.builder()
            .title(Components.translatable("tacz.type.mg.name")).withTabsBefore(GUN_RPG_TAB.getId())
            .icon(() -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "m249")).build())
            .displayItems((parameters, output) -> output.acceptAll(AbstractGunItem.fillItemCategory(GunTabType.MG))).build());
}
