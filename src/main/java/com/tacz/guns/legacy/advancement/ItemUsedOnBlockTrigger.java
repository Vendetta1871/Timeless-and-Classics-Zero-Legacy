package com.tacz.guns.legacy.advancement;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.legacy.util.SimpleCriterionTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.20.1 {@code minecraft:item_used_on_block} (1.12.2 has none): a block accepted a right click with an item, or the
 * item was used on it. Fired by {@code PlayerInteractionManagerMixin} with the stack as it was before the use.
 * <p>
 * Conditions: {@code location}, a list of loot conditions of which {@code minecraft:location_check} is understood
 * ({@code predicate.block.blocks} plus the 1.12.2 location predicate's position, biome, feature and dimension), and
 * {@code item}. 1.20.1 itself ignores {@code item} (its trigger reads only {@code location}); here it is honoured, so
 * that "place a gun in a display case" needs a gun.
 */
public final class ItemUsedOnBlockTrigger extends SimpleCriterionTrigger<ItemUsedOnBlockTrigger.TriggerInstance> {
    public static final ItemUsedOnBlockTrigger INSTANCE = new ItemUsedOnBlockTrigger();
    static final ResourceLocation ID = new ResourceLocation(GunMod.MOD_ID, "item_used_on_block");

    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(EntityPlayerMP player, BlockPos pos, ItemStack stack) {
        this.trigger(player, instance -> instance.matches(player.getServerWorld(), pos, stack));
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, JsonDeserializationContext context) {
        List<LocationCheck> checks = new ArrayList<>();
        JsonElement location = json.get("location");
        if (location != null && location.isJsonArray()) {
            for (JsonElement element : location.getAsJsonArray()) {
                JsonObject condition = JsonUtils.getJsonObject(element, "condition");
                String type = new ResourceLocation(JsonUtils.getString(condition, "condition")).toString();
                if (!"minecraft:location_check".equals(type)) {
                    throw new JsonSyntaxException("Unsupported location condition " + type);
                }
                checks.add(LocationCheck.fromJson(condition));
            }
        } else if (location != null && location.isJsonObject()) {
            // the 1.19.3 form: a location predicate
            checks.add(LocationCheck.fromPredicate(location.getAsJsonObject(), BlockPos.ORIGIN));
        }
        return new TriggerInstance(checks, LegacyItemPredicate.fromJson(json.get("item")));
    }

    public static class TriggerInstance extends AbstractCriterionInstance {
        private final List<LocationCheck> location;
        private final ItemPredicate item;

        public TriggerInstance(List<LocationCheck> location, ItemPredicate item) {
            super(ID);
            this.location = location;
            this.item = item;
        }

        public boolean matches(WorldServer world, BlockPos pos, ItemStack stack) {
            for (LocationCheck check : location) {
                if (!check.test(world, pos)) {
                    return false;
                }
            }
            return item.test(stack);
        }
    }

    private static final class LocationCheck {
        private final Set<Block> blocks;
        private final LocationPredicate predicate;
        private final BlockPos offset;

        private LocationCheck(Set<Block> blocks, LocationPredicate predicate, BlockPos offset) {
            this.blocks = blocks;
            this.predicate = predicate;
            this.offset = offset;
        }

        static LocationCheck fromJson(JsonObject condition) {
            BlockPos offset = new BlockPos(JsonUtils.getInt(condition, "offsetX", 0),
                    JsonUtils.getInt(condition, "offsetY", 0), JsonUtils.getInt(condition, "offsetZ", 0));
            return fromPredicate(JsonUtils.getJsonObject(condition, "predicate"), offset);
        }

        static LocationCheck fromPredicate(JsonObject predicate, BlockPos offset) {
            Set<Block> blocks = new HashSet<>();
            if (predicate.has("block")) {
                JsonObject block = JsonUtils.getJsonObject(predicate, "block");
                if (block.has("tag") || block.has("state") || block.has("nbt")) {
                    throw new JsonSyntaxException("Only block ids are supported in a block predicate");
                }
                if (block.has("blocks")) {
                    for (JsonElement id : JsonUtils.getJsonArray(block, "blocks")) {
                        ResourceLocation name = new ResourceLocation(JsonUtils.getString(id, "block"));
                        if (!Block.REGISTRY.containsKey(name)) {
                            throw new JsonSyntaxException("Unknown block id '" + name + "'");
                        }
                        blocks.add(Block.REGISTRY.getObject(name));
                    }
                }
            }
            // 1.12.2's Gson has no public deepCopy
            JsonObject rest = new JsonObject();
            for (java.util.Map.Entry<String, JsonElement> entry : predicate.entrySet()) {
                if (!"block".equals(entry.getKey())) {
                    rest.add(entry.getKey(), entry.getValue());
                }
            }
            return new LocationCheck(blocks, LocationPredicate.deserialize(rest), offset);
        }

        boolean test(WorldServer world, BlockPos pos) {
            BlockPos at = pos.add(offset);
            if (!blocks.isEmpty() && !blocks.contains(world.getBlockState(at).getBlock())) {
                return false;
            }
            return predicate.test(world, at.getX() + 0.5F, at.getY() + 0.5F, at.getZ() + 0.5F);
        }
    }
}
