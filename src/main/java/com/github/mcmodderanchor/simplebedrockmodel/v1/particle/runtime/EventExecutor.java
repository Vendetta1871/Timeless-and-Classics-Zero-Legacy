package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 粒子事件执行引擎。
 */
public final class EventExecutor {

    private EventExecutor() {}

    /**
     * 事件执行上下文。
     */
    public static final class EventContext {
        private final ParticleEmitterInstance emitter;
        private final ParticleMolangEnvironment molang;
        @Nullable
        private final WorldClient level;
        @Nullable
        private final Vec3d position;
        private final Random random;  // 本地随机源，用于 EventRandomize

        public EventContext(ParticleEmitterInstance emitter, ParticleMolangEnvironment molang,
                            @Nullable WorldClient level, @Nullable Vec3d position, Random random) {
            this.emitter = emitter;
            this.molang = molang;
            this.level = level;
            this.position = position;
            this.random = random;
        }

        public EventContext(ParticleEmitterInstance emitter, ParticleMolangEnvironment molang,
                            @Nullable WorldClient level, @Nullable Vec3d position) {
            this(emitter, molang, level, position, new Random());
        }

        public ParticleEmitterInstance emitter() {
            return emitter;
        }

        public ParticleMolangEnvironment molang() {
            return molang;
        }

        @Nullable
        public WorldClient level() {
            return level;
        }

        @Nullable
        public Vec3d position() {
            return position;
        }

        public Random random() {
            return random;
        }
    }

    public static void fireEvents(List<String> eventNames, ParticleEffectDefinition definition, EventContext ctx) {
        if (eventNames.isEmpty()) return;
        Map<String, List<IEventNode>> allEvents = definition.getEvents();
        if (allEvents.isEmpty()) return;

        for (String name : eventNames) {
            List<IEventNode> nodes = allEvents.get(name);
            if (nodes == null) continue;
            for (IEventNode node : nodes) {
                execute(node, ctx);
            }
        }
    }

    public static void execute(IEventNode node, EventContext ctx) {
        if (node instanceof EventSequence seq) {
            for (IEventNode child : seq.nodes()) {
                execute(child, ctx);
            }
        } else if (node instanceof EventRandomize randomize) {
            executeRandomize(randomize, ctx);
        } else if (node instanceof ParticleEffectEvent effect) {
            executeParticleEffect(effect, ctx);
        } else if (node instanceof SoundEffectEvent sound) {
            executeSoundEffect(sound, ctx);
        } else if (node instanceof MolangExpressionEvent expr) {
            executeMolangExpression(expr, ctx);
        } else if (node instanceof EventLog log) {
            executeLog(log, ctx);
        }
    }

    private static void executeRandomize(EventRandomize randomize, EventContext ctx) {
        List<EventRandomize.WeightedEntry> entries = randomize.entries();
        if (entries.isEmpty()) return;

        float totalWeight = 0;
        for (EventRandomize.WeightedEntry entry : entries) {
            totalWeight += entry.weight();
        }
        if (totalWeight <= 0) return;

        float roll = ctx.random().nextFloat() * totalWeight;
        float cumulative = 0;
        for (EventRandomize.WeightedEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                execute(entry.node(), ctx);
                return;
            }
        }
        execute(entries.get(entries.size() - 1).node(), ctx);
    }

    private static void executeParticleEffect(ParticleEffectEvent effect, EventContext ctx) {
        if (ctx.level == null || ctx.position == null) return;

        // 执行预编译的 pre_effect_expression
        if (effect.compiledPreEffect() != null) {
            try {
                effect.compiledPreEffect().evaluate(ctx.molang.getContext());
            } catch (Exception e) {
                SimpleBedrockModel.LOGGER.warn("Failed to evaluate pre_effect_expression: {}", effect.preEffectExpression(), e);
            }
        }

        ResourceLocation effectId = new ResourceLocation(effect.effect());
        ParticleEffectDefinition childDef = ParticleDefinitionLoader.getInstance().getDefinition(effectId);
        if (childDef == null) {
            SimpleBedrockModel.LOGGER.debug("Particle effect event references unknown effect: {}", effect.effect());
            return;
        }

        WorldEmitterManager.getInstance().addEmitter(ctx.level, ctx.position, Vec3d.ZERO, childDef);
    }

    private static void executeSoundEffect(SoundEffectEvent sound, EventContext ctx) {
        if (ctx.level == null || ctx.position == null) return;
        String eventName = sound.eventName();
        if (eventName == null || eventName.isEmpty()) return;

        try {
            ResourceLocation soundId = new ResourceLocation(eventName);
            SoundEvent soundEvent = new SoundEvent(soundId);
            ctx.level.playSound(
                    ctx.position.x, ctx.position.y, ctx.position.z,
                    soundEvent, SoundCategory.AMBIENT, 1.0f, 1.0f, false
            );
        } catch (Exception e) {
            SimpleBedrockModel.LOGGER.warn("Failed to play sound effect: {}", eventName, e);
        }
    }

    private static void executeMolangExpression(MolangExpressionEvent expr, EventContext ctx) {
        // 使用预编译的表达式
        if (expr.compiledExpression() == null) return;
        try {
            expr.compiledExpression().evaluate(ctx.molang.getContext());
        } catch (Exception e) {
            SimpleBedrockModel.LOGGER.warn("Failed to evaluate event molang expression: {}", expr.expression(), e);
        }
    }

    private static void executeLog(EventLog log, EventContext ctx) {
        SimpleBedrockModel.LOGGER.info("[ParticleEvent] {}", log.message());
    }
}
