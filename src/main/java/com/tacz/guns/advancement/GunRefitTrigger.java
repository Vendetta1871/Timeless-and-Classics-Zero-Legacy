package com.tacz.guns.advancement;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.tacz.guns.legacy.util.SimpleCriterionTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GunRefitTrigger extends SimpleCriterionTrigger<GunRefitTrigger.TriggerInstance> {
    public static final GunRefitTrigger INSTANCE = new GunRefitTrigger();
    private static final ResourceLocation ID = new ResourceLocation("tacz", "gun_refit");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(EntityPlayerMP player, ItemStack before, ItemStack after) {
        if (!ItemStack.areItemStacksEqual(before, after)) {
            this.trigger(player, instance -> true);
        }
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, JsonDeserializationContext context) {
        return new TriggerInstance();
    }

    public static class TriggerInstance extends AbstractCriterionInstance {
        public TriggerInstance() {
            super(ID);
        }
    }
}
