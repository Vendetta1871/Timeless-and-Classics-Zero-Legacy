package com.tacz.guns.legacy.advancement;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.util.SimpleCriterionTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.DamageSource;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * 1.20.1 {@code minecraft:player_killed_entity}. 1.12.2 has the trigger but no {@code player} condition, and its
 * entity predicate has no {@code equipment}; gun pack advancements are moved to this one (see
 * {@link GunPackAdvancements}). Fired from {@link AdvancementEvents}.
 * <p>
 * Entity predicates are 1.12.2's ({@code type}, {@code distance}, {@code location}, {@code effects}, {@code nbt}) plus
 * 1.20.1's {@code equipment}; both the plain object and the 1.20.1 list of {@code minecraft:entity_properties} loot
 * conditions (for {@code this}) are read.
 */
public final class PlayerKilledEntityTrigger extends SimpleCriterionTrigger<PlayerKilledEntityTrigger.TriggerInstance> {
    public static final PlayerKilledEntityTrigger INSTANCE = new PlayerKilledEntityTrigger();
    static final ResourceLocation ID = new ResourceLocation(GunMod.MOD_ID, "player_killed_entity");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(EntityPlayerMP player, Entity killed, DamageSource source) {
        this.trigger(player, instance -> instance.matches(player, killed, source));
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, JsonDeserializationContext context) {
        return new TriggerInstance(Predicate.fromJson(json.get("player")), Predicate.fromJson(json.get("entity")),
                DamageSourcePredicate.deserialize(json.get("killing_blow")));
    }

    public static class TriggerInstance extends AbstractCriterionInstance {
        private final Predicate player;
        private final Predicate entity;
        private final DamageSourcePredicate killingBlow;

        public TriggerInstance(Predicate player, Predicate entity, DamageSourcePredicate killingBlow) {
            super(ID);
            this.player = player;
            this.entity = entity;
            this.killingBlow = killingBlow;
        }

        public boolean matches(EntityPlayerMP player, Entity killed, DamageSource source) {
            return this.player.test(player, player) && this.entity.test(player, killed) && killingBlow.test(player, source);
        }
    }

    static final class Predicate {
        private static final Predicate ANY = new Predicate(EntityPredicate.ANY, new EnumMap<>(EntityEquipmentSlot.class));

        private final EntityPredicate entity;
        private final Map<EntityEquipmentSlot, ItemPredicate> equipment;

        private Predicate(EntityPredicate entity, Map<EntityEquipmentSlot, ItemPredicate> equipment) {
            this.entity = entity;
            this.equipment = equipment;
        }

        static Predicate fromJson(@Nullable JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return ANY;
            }
            JsonObject json;
            if (element.isJsonArray()) {
                if (element.getAsJsonArray().size() == 0) {
                    return ANY;
                }
                if (element.getAsJsonArray().size() > 1) {
                    throw new JsonSyntaxException("Only one entity_properties condition is supported");
                }
                JsonObject condition = JsonUtils.getJsonObject(element.getAsJsonArray().get(0), "condition");
                String type = new ResourceLocation(JsonUtils.getString(condition, "condition")).toString();
                if (!"minecraft:entity_properties".equals(type) || !"this".equals(JsonUtils.getString(condition, "entity", "this"))) {
                    throw new JsonSyntaxException("Unsupported entity condition " + type);
                }
                json = JsonUtils.getJsonObject(condition, "predicate");
            } else {
                json = JsonUtils.getJsonObject(element, "entity");
            }
            Map<EntityEquipmentSlot, ItemPredicate> equipment = new EnumMap<>(EntityEquipmentSlot.class);
            JsonObject rest = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if ("equipment".equals(entry.getKey())) {
                    for (Map.Entry<String, JsonElement> slot : JsonUtils.getJsonObject(entry.getValue(), "equipment").entrySet()) {
                        equipment.put(EntityEquipmentSlot.fromString(slot.getKey()), LegacyItemPredicate.fromJson(slot.getValue()));
                    }
                } else {
                    rest.add(entry.getKey(), entry.getValue());
                }
            }
            return new Predicate(EntityPredicate.deserialize(rest), equipment);
        }

        boolean test(EntityPlayerMP player, Entity entity) {
            if (!this.entity.test(player, entity)) {
                return false;
            }
            for (Map.Entry<EntityEquipmentSlot, ItemPredicate> entry : equipment.entrySet()) {
                if (!(entity instanceof EntityLivingBase living) || !entry.getValue().test(living.getItemStackFromSlot(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
