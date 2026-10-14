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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 1.12.2: creative tabs are {@link CreativeTabs} subclasses created in order (no registry); {@link #register()} only
 * triggers the class initialization.
 */
@SuppressWarnings("all")
public class ModCreativeTabs {
    public static void register() {
    }

    public static CreativeTabs OTHER_TAB = new Tab("tacz_other", "itemGroup.tab.tacz.other",
            () -> new ItemStack(ModItems.GUN_SMITH_TABLE.get()),
            output -> {
                output.addAll(GunSmithTableItem.fillItemCategory());
                output.add(new ItemStack(ModItems.TARGET.get()));
                output.add(new ItemStack(ModItems.STATUE.get()));
                output.add(new ItemStack(ModItems.TARGET_MINECART.get()));
                AmmoBoxItem.fillItemCategory(output);
            });

    public static CreativeTabs AMMO_TAB = new Tab("tacz_ammo", "itemGroup.tab.tacz.ammo",
            () -> AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build(),
            output -> output.addAll(AmmoItem.fillItemCategory()));

    public static CreativeTabs ATTACHMENT_SCOPE_TAB = new Tab("tacz_scope", "tacz.type.scope.name",
            () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "scope_acog_ta31")).build(),
            output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.SCOPE)));

    public static CreativeTabs ATTACHMENT_MUZZLE_TAB = new Tab("tacz_muzzle", "tacz.type.muzzle.name",
            () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "muzzle_compensator_trident")).build(),
            output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.MUZZLE)));

    public static CreativeTabs ATTACHMENT_STOCK_TAB = new Tab("tacz_stock", "tacz.type.stock.name",
            () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "stock_militech_b5")).build(),
            output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.STOCK)));

    public static CreativeTabs ATTACHMENT_GRIP_TAB = new Tab("tacz_grip", "tacz.type.grip.name",
            () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "grip_magpul_afg_2")).build(),
            output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.GRIP)));

    public static CreativeTabs ATTACHMENT_EXTENDED_MAG_TAB = new Tab("tacz_extended_mag", "tacz.type.extended_mag.name",
            () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "extended_mag_3")).build(),
            output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.EXTENDED_MAG)));

    public static CreativeTabs ATTACHMENT_LASER_TAB = new Tab("tacz_laser", "tacz.type.laser.name",
            () -> AttachmentItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "laser_compact")).build(),
            output -> output.addAll(AttachmentItem.fillItemCategory(AttachmentType.LASER)));

    public static CreativeTabs GUN_PISTOL_TAB = new Tab("tacz_pistol", "tacz.type.pistol.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "glock_17")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.PISTOL)));

    public static CreativeTabs GUN_SNIPER_TAB = new Tab("tacz_sniper", "tacz.type.sniper.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ai_awp")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.SNIPER)));

    public static CreativeTabs GUN_RIFLE_TAB = new Tab("tacz_rifle", "tacz.type.rifle.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "ak47")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.RIFLE)));

    public static CreativeTabs GUN_SHOTGUN_TAB = new Tab("tacz_shotgun", "tacz.type.shotgun.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "db_short")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.SHOTGUN)));

    public static CreativeTabs GUN_SMG_TAB = new Tab("tacz_smg", "tacz.type.smg.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "hk_mp5a5")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.SMG)));

    public static CreativeTabs GUN_RPG_TAB = new Tab("tacz_rpg", "tacz.type.rpg.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "rpg7")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.RPG)));

    public static CreativeTabs GUN_MG_TAB = new Tab("tacz_mg", "tacz.type.mg.name",
            () -> GunItemBuilder.create().setId(new ResourceLocation(GunMod.MOD_ID, "m249")).build(),
            output -> output.addAll(AbstractGunItem.fillItemCategory(GunTabType.MG)));

    /**
     * {@code CreativeModeTab.builder().title(..).icon(..).displayItems(..)}
     */
    public static class Tab extends CreativeTabs {
        private final String title;
        private final Supplier<ItemStack> icon;
        private final Consumer<NonNullList<ItemStack>> displayItems;

        public Tab(String label, String title, Supplier<ItemStack> icon, Consumer<NonNullList<ItemStack>> displayItems) {
            super(label);
            this.title = title;
            this.icon = icon;
            this.displayItems = displayItems;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public String getTranslationKey() {
            return title;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() {
            return icon.get();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void displayAllRelevantItems(NonNullList<ItemStack> output) {
            displayItems.accept(output);
        }
    }
}
