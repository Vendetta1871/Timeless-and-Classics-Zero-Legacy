package com.tacz.guns.advancement;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.tacz.guns.legacy.util.SimpleCriterionTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

public class TargetHitTrigger extends SimpleCriterionTrigger<TargetHitTrigger.TriggerInstance> {
    public static final TargetHitTrigger INSTANCE = new TargetHitTrigger();
    static final ResourceLocation ID = new ResourceLocation("tacz", "target_hit");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(EntityPlayerMP player, double distance) {
        this.trigger(player, instance -> instance.matches(distance));
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, JsonDeserializationContext context) {
        // 1.12.2: MinMaxBounds is the float variant of MinMaxBounds.Doubles
        MinMaxBounds distance = MinMaxBounds.deserialize(json.get("distance"));
        return new TriggerInstance(distance);
    }

    public static class TriggerInstance extends AbstractCriterionInstance {
        private final MinMaxBounds distance;

        public TriggerInstance(MinMaxBounds distance) {
            super(ID);
            this.distance = distance;
        }

        public boolean matches(double distance) {
            return this.distance.test((float) distance);
        }

        // TODO(port): 1.12.2 criteria are never serialized (no serializeToJson; data generation only upstream)
    }
}
