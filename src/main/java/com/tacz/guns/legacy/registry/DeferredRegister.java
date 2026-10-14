package com.tacz.guns.legacy.registry;

import com.tacz.guns.GunMod;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1.20.1 {@code DeferredRegister} for the 1.12.2 Forge registries ({@code ForgeRegistries.ITEMS}, {@code BLOCKS},
 * {@code SOUND_EVENTS}, {@code ENTITIES}, {@code POTIONS}, {@code ENCHANTMENTS}). Entries are created and registered
 * in the registry's {@code RegistryEvent.Register}, which 1.12.2 fires after pre-init, so {@link #register(EventBus)}
 * must be called before that (mod construction or pre-init). The registry name is set from the given name.
 */
@Mod.EventBusSubscriber(modid = GunMod.MOD_ID)
public class DeferredRegister<T extends IForgeRegistryEntry<T>> {
    private static final List<DeferredRegister<?>> REGISTERS = new ArrayList<>();

    private final IForgeRegistry<T> registry;
    private final String modid;
    private final Map<RegistryObject<T>, Supplier<? extends T>> entries = new LinkedHashMap<>();
    private boolean seenRegisterEvent = false;

    private DeferredRegister(IForgeRegistry<T> registry, String modid) {
        this.registry = registry;
        this.modid = modid;
    }

    public static <B extends IForgeRegistryEntry<B>> DeferredRegister<B> create(IForgeRegistry<B> registry, String modid) {
        return new DeferredRegister<>(registry, modid);
    }

    @SuppressWarnings("unchecked")
    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> supplier) {
        if (seenRegisterEvent) {
            throw new IllegalStateException("Cannot register new entries to DeferredRegister after RegistryEvent.Register has been fired.");
        }
        RegistryObject<I> object = new RegistryObject<>(new ResourceLocation(modid, name));
        if (entries.putIfAbsent((RegistryObject<T>) object, supplier) != null) {
            throw new IllegalArgumentException("Duplicate registration " + name);
        }
        return object;
    }

    /**
     * Upstream passes the mod event bus; 1.12.2 fires registry events on {@code MinecraftForge.EVENT_BUS}, which
     * this class already listens to, so the bus is only kept for the signature.
     */
    public void register(EventBus bus) {
        if (!REGISTERS.contains(this)) {
            REGISTERS.add(this);
        }
    }

    public java.util.Collection<RegistryObject<T>> getEntries() {
        return java.util.Collections.unmodifiableSet(entries.keySet());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addEntries(IForgeRegistry<?> eventRegistry) {
        if (eventRegistry != registry) {
            return;
        }
        seenRegisterEvent = true;
        for (Map.Entry<RegistryObject<T>, Supplier<? extends T>> entry : entries.entrySet()) {
            T value = entry.getValue().get();
            value.setRegistryName(entry.getKey().getId());
            registry.register(value);
            ((RegistryObject) entry.getKey()).set(value);
        }
    }

    private static void fire(IForgeRegistry<?> registry) {
        for (DeferredRegister<?> register : REGISTERS) {
            register.addEntries(registry);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlocks(RegistryEvent.Register<Block> event) {
        fire(event.getRegistry());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItems(RegistryEvent.Register<Item> event) {
        fire(event.getRegistry());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSounds(RegistryEvent.Register<SoundEvent> event) {
        fire(event.getRegistry());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntities(RegistryEvent.Register<EntityEntry> event) {
        fire(event.getRegistry());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPotions(RegistryEvent.Register<Potion> event) {
        fire(event.getRegistry());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEnchantments(RegistryEvent.Register<Enchantment> event) {
        fire(event.getRegistry());
    }
}
