package com.tacz.guns.legacy.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.EnumParticleTypes;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Stands in for {@code ParticleArgument.readParticle(..)}: maps a 1.20.1 particle string ({@code minecraft:smoke},
 * {@code dust 1 0 0 1}) to the closest 1.12.2 {@link EnumParticleTypes}. Particle arguments are ignored.
 */
public final class LegacyParticles {
    private static final Map<String, EnumParticleTypes> RENAMED = ImmutableMap.<String, EnumParticleTypes>builder()
            .put("smoke", EnumParticleTypes.SMOKE_NORMAL)
            .put("large_smoke", EnumParticleTypes.SMOKE_LARGE)
            .put("campfire_cosy_smoke", EnumParticleTypes.SMOKE_LARGE)
            .put("campfire_signal_smoke", EnumParticleTypes.SMOKE_LARGE)
            .put("white_ash", EnumParticleTypes.TOWN_AURA)
            .put("ash", EnumParticleTypes.TOWN_AURA)
            .put("mycelium", EnumParticleTypes.TOWN_AURA)
            .put("poof", EnumParticleTypes.EXPLOSION_NORMAL)
            .put("explosion", EnumParticleTypes.EXPLOSION_LARGE)
            .put("explosion_emitter", EnumParticleTypes.EXPLOSION_HUGE)
            .put("firework", EnumParticleTypes.FIREWORKS_SPARK)
            .put("electric_spark", EnumParticleTypes.FIREWORKS_SPARK)
            .put("enchanted_hit", EnumParticleTypes.CRIT_MAGIC)
            .put("dust", EnumParticleTypes.REDSTONE)
            .put("dust_color_transition", EnumParticleTypes.REDSTONE)
            .put("soul_fire_flame", EnumParticleTypes.FLAME)
            .put("small_flame", EnumParticleTypes.FLAME)
            .put("bubble", EnumParticleTypes.WATER_BUBBLE)
            .put("splash", EnumParticleTypes.WATER_SPLASH)
            .put("rain", EnumParticleTypes.WATER_DROP)
            .put("dripping_water", EnumParticleTypes.DRIP_WATER)
            .put("dripping_lava", EnumParticleTypes.DRIP_LAVA)
            .put("item_snowball", EnumParticleTypes.SNOWBALL)
            .put("snowflake", EnumParticleTypes.SNOW_SHOVEL)
            .put("witch", EnumParticleTypes.SPELL_WITCH)
            .put("effect", EnumParticleTypes.SPELL)
            .put("instant_effect", EnumParticleTypes.SPELL_INSTANT)
            .put("entity_effect", EnumParticleTypes.SPELL_MOB)
            .put("ambient_entity_effect", EnumParticleTypes.SPELL_MOB_AMBIENT)
            .put("angry_villager", EnumParticleTypes.VILLAGER_ANGRY)
            .put("happy_villager", EnumParticleTypes.VILLAGER_HAPPY)
            .put("totem_of_undying", EnumParticleTypes.TOTEM)
            .put("elder_guardian", EnumParticleTypes.MOB_APPEARANCE)
            .put("underwater", EnumParticleTypes.SUSPENDED_DEPTH)
            .put("damage_indicator", EnumParticleTypes.DAMAGE_INDICATOR)
            .put("sweep_attack", EnumParticleTypes.SWEEP_ATTACK)
            .put("end_rod", EnumParticleTypes.END_ROD)
            .build();

    private LegacyParticles() {
    }

    @Nullable
    public static EnumParticleTypes readParticle(String particle) {
        String name = particle.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }
        EnumParticleTypes type = RENAMED.get(name);
        if (type != null) {
            return type;
        }
        // names that did not change, written in 1.12.2 camel case (e.g. "flame", "crit", "cloud", "lava")
        StringBuilder camel = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                camel.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return EnumParticleTypes.getByName(camel.toString());
    }
}
