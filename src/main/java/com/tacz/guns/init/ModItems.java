package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import com.tacz.guns.legacy.registry.DeferredRegister;
import com.tacz.guns.legacy.registry.RegistryObject;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GunMod.MOD_ID);

    public static RegistryObject<ModernKineticGunItem> MODERN_KINETIC_GUN = ITEMS.register("modern_kinetic_gun", ModernKineticGunItem::new);

//    public static RegistryObject<ThrowableItem> M67 = ITEMS.register("m67", ThrowableItem::new);

    public static RegistryObject<Item> AMMO = ITEMS.register("ammo", AmmoItem::new);
    public static RegistryObject<AttachmentItem> ATTACHMENT = ITEMS.register("attachment", AttachmentItem::new);

    public static RegistryObject<GunSmithTableItem> GUN_SMITH_TABLE = ITEMS.register("gun_smith_table", () -> new DefaultTableItem(ModBlocks.GUN_SMITH_TABLE.get()));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_111 = ITEMS.register("workbench_a", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_111.get()));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_211 = ITEMS.register("workbench_b", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_211.get()));
    public static RegistryObject<GunSmithTableItem> WORKBENCH_121 = ITEMS.register("workbench_c", () -> new GunSmithTableItem(ModBlocks.WORKBENCH_121.get()));


    public static RegistryObject<Item> TARGET = ITEMS.register("target", () -> new ItemBlock(ModBlocks.TARGET.get()));
    public static RegistryObject<Item> STATUE = ITEMS.register("statue", () -> new ItemBlock(ModBlocks.STATUE.get()));
    public static RegistryObject<Item> GUN_PEGBOARD = ITEMS.register("gun_pegboard", () -> new GunPegboardItem(ModBlocks.GUN_PEGBOARD.get()));
    public static RegistryObject<Item> GLOW_GUN_PEGBOARD = ITEMS.register("glow_gun_pegboard", () -> new GunPegboardItem(ModBlocks.GLOW_GUN_PEGBOARD.get()));
    public static RegistryObject<Item> AMMO_STORAGE = ITEMS.register("ammo_storage", () -> new AmmoStorageBlockItem(ModBlocks.AMMO_STORAGE.get()));
    public static RegistryObject<Item> AMMO_STORAGE_GOLD = ITEMS.register("ammo_storage_gold", () -> new AmmoStorageBlockItem(ModBlocks.AMMO_STORAGE_GOLD.get()));
    public static RegistryObject<Item> AMMO_STORAGE_DIAMOND = ITEMS.register("ammo_storage_diamond", () -> new AmmoStorageBlockItem(ModBlocks.AMMO_STORAGE_DIAMOND.get()));
    public static RegistryObject<Item> GUN_DISPLAY_CASE = ITEMS.register("gun_display_case", () -> new ItemBlock(ModBlocks.GUN_DISPLAY_CASE.get()) {
        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, @Nullable World level, List<String> tooltip, ITooltipFlag flag) {
            tooltip.add(Components.translatable("tooltip.tacz.gun_display_case").withStyle(TextFormatting.GRAY).getFormattedText());
        }
    });
    public static RegistryObject<Item> AMMO_BOX = ITEMS.register("ammo_box", AmmoBoxItem::new);
    public static RegistryObject<Item> TARGET_MINECART = ITEMS.register("target_minecart", TargetMinecartItem::new);

    /**
     * 1.12.2: {@code RegisterEvent} for the item registry is {@code RegistryEvent.Register<Item>}; the items themselves
     * are created at HIGH priority by the {@link DeferredRegister}.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemRegister(RegistryEvent.Register<Item> event) {
        GunItemManager.registerGunItem(ModernKineticGunItem.TYPE_NAME, MODERN_KINETIC_GUN);
    }
}
