package com.tacz.guns.legacy.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 1.20.1 {@code SimpleCriterionTrigger}: the per-player listener bookkeeping that every 1.12.2 criterion trigger
 * writes by hand ({@code UsedTotemTrigger.Listeners} and the like). Subclasses implement {@link #createInstance} and
 * call {@link #trigger(EntityPlayerMP, Predicate)}. 1.12.2 criteria have no {@code player} predicate, so
 * {@code createInstance} gets no {@code ContextAwarePredicate}.
 */
public abstract class SimpleCriterionTrigger<T extends ICriterionInstance> implements ICriterionTrigger<T> {
    private final Map<PlayerAdvancements, Set<Listener<T>>> players = Maps.newIdentityHashMap();

    @Override
    public final void addListener(PlayerAdvancements playerAdvancements, Listener<T> listener) {
        this.players.computeIfAbsent(playerAdvancements, key -> Sets.newHashSet()).add(listener);
    }

    @Override
    public final void removeListener(PlayerAdvancements playerAdvancements, Listener<T> listener) {
        Set<Listener<T>> listeners = this.players.get(playerAdvancements);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                this.players.remove(playerAdvancements);
            }
        }
    }

    @Override
    public final void removeAllListeners(PlayerAdvancements playerAdvancements) {
        this.players.remove(playerAdvancements);
    }

    protected abstract T createInstance(JsonObject json, JsonDeserializationContext context);

    @Override
    public final T deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return createInstance(json, context);
    }

    protected void trigger(EntityPlayerMP player, Predicate<T> testTrigger) {
        PlayerAdvancements playerAdvancements = player.getAdvancements();
        Set<Listener<T>> listeners = this.players.get(playerAdvancements);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        // granting a criterion can remove listeners: collect first
        List<Listener<T>> matched = new ArrayList<>();
        for (Listener<T> listener : listeners) {
            if (testTrigger.test(listener.getCriterionInstance())) {
                matched.add(listener);
            }
        }
        for (Listener<T> listener : matched) {
            listener.grantCriterion(playerAdvancements);
        }
    }
}
