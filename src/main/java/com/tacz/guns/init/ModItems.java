package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.item.*;
import com.tacz.guns.legacy.registry.DeferredRegister;
import com.tacz.guns.legacy.registry.RegistryObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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
