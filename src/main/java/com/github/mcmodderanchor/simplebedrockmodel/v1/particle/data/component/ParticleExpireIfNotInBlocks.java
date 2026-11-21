package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 粒子方块过期组件。对应 "minecraft:particle_expire_if_not_in_blocks"。
 * <p>
 * 当粒子不在指定方块内时过期。
 *
 * @param blocks 方块 ID 集合（如 "minecraft:water"）
 */
public final class ParticleExpireIfNotInBlocks implements IParticleComponentDefinition, IParticleComponent {
    private final Set<String> blocks;

    public ParticleExpireIfNotInBlocks(Set<String> blocks) {
        this.blocks = blocks;
    }

    public Set<String> blocks() {
        return blocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleExpireIfNotInBlocks)) return false;
        ParticleExpireIfNotInBlocks that = (ParticleExpireIfNotInBlocks) o;
        return java.util.Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(blocks);
    }

    @Override
    public String toString() {
        return "ParticleExpireIfNotInBlocks[" + "blocks=" + blocks + "]";
    }


    @Override
    public int order() {
        return 340;
    }

    @Override
    public boolean requireUpdate() {
        return !blocks.isEmpty();
    }

    public static ParticleExpireIfNotInBlocks fromJson(JsonElement value) {
        Set<String> blocks = new LinkedHashSet<>();
        if (value.isJsonArray()) {
            JsonArray arr = value.getAsJsonArray();
            for (JsonElement elem : arr) {
                blocks.add(elem.getAsString());
            }
        }
        return new ParticleExpireIfNotInBlocks(blocks);
    }
}
