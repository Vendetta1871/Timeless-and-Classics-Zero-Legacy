package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 粒子方块过期组件。对应 "minecraft:particle_expire_if_in_blocks"。
 * <p>
 * 当粒子位于指定方块内时过期。
 *
 * @param blocks 方块 ID 集合（如 "minecraft:water"）
 */
public final class ParticleExpireIfInBlocks implements IParticleComponentDefinition, IParticleComponent {
    private final Set<String> blocks;

    public ParticleExpireIfInBlocks(Set<String> blocks) {
        this.blocks = blocks;
    }

    public Set<String> blocks() {
        return blocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticleExpireIfInBlocks)) return false;
        ParticleExpireIfInBlocks that = (ParticleExpireIfInBlocks) o;
        return java.util.Objects.equals(blocks, that.blocks);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(blocks);
    }

    @Override
    public String toString() {
        return "ParticleExpireIfInBlocks[" + "blocks=" + blocks + "]";
    }


    @Override
    public int order() {
        return 330;
    }

    @Override
    public boolean requireUpdate() {
        return !blocks.isEmpty();
    }

    public static ParticleExpireIfInBlocks fromJson(JsonElement value) {
        Set<String> blocks = new LinkedHashSet<>();
        if (value.isJsonArray()) {
            JsonArray arr = value.getAsJsonArray();
            for (JsonElement elem : arr) {
                blocks.add(elem.getAsString());
            }
        }
        return new ParticleExpireIfInBlocks(blocks);
    }
}
