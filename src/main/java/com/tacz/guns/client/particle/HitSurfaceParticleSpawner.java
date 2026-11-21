package com.tacz.guns.client.particle;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.pojo.display.ammo.HitSurfaceParticle;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.ColorHex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import com.tacz.guns.legacy.util.LegacyParticles;
import com.tacz.guns.legacy.util.ResourceLocations;
import net.minecraft.block.Block;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;

@SideOnly(Side.CLIENT)
public final class HitSurfaceParticleSpawner {
    private static final int MAX_BLOCK_PARTICLE_COUNT = 32;
    private static final int MAX_DISPLAY_PARTICLE_COUNT_PER_HIT = 64;
    private static final float DEFAULT_SURFACE_OFFSET = 0.03f;
    private static final float DEFAULT_RANDOM_OFFSET = 0.01f;
    private static final int DEFAULT_BLOCK_PARTICLE_COUNT = 3;
    private static final float DEFAULT_BLOCK_NORMAL_SPEED = 0.14f;
    private static final float DEFAULT_BLOCK_RANDOM_SPEED = 0.015f;
    private static final float DEFAULT_BLOCK_SPREAD_ANGLE = 30.0f;
    private static final String DUST = "minecraft:dust";
    private static final String DUST_COLOR_TRANSITION = "minecraft:dust_color_transition";
    private static final String BLOCK = "minecraft:block";
    private static final String FALLING_DUST = "minecraft:falling_dust";
    private static final String ITEM = "minecraft:item";

    private HitSurfaceParticleSpawner() {
    }

    /**
     * 1.12.2 stand-in for {@code ParticleOptions}: a vanilla particle type with its int arguments (block state id for
     * block particles) and, for dust, the color that 1.12.2 passes in place of the speed.
     */
    private static final class ParticleOptions {
        private final EnumParticleTypes type;
        private final int[] arguments;
        @Nullable
        private final float[] dustColor;
        private final float dustScale;

        private ParticleOptions(EnumParticleTypes type, int... arguments) {
            this(type, null, 1.0f, arguments);
        }

        private ParticleOptions(EnumParticleTypes type, @Nullable float[] dustColor, float dustScale, int... arguments) {
            this.type = type;
            this.arguments = arguments;
            this.dustColor = dustColor;
            this.dustScale = dustScale;
        }

        private static ParticleOptions block(EnumParticleTypes type, IBlockState state) {
            return new ParticleOptions(type, Block.getStateId(state));
        }
    }

    /**
     * 1.12.2 stand-in for {@code ParticleEngine#createParticle}. Vanilla 1.12.2 particles randomize the given speed a
     * little (the 7-argument {@code Particle} constructor), and dust (redstone) takes its color in place of the speed.
     */
    @Nullable
    private static Particle createParticle(ParticleManager particleEngine, ParticleOptions options, double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed) {
        if (options.dustColor != null) {
            // ParticleRedstone turns a red of 0 into 1
            // TODO(port): 1.12.2 dust ignores the speed (the arguments are its color)
            Particle particle = particleEngine.spawnEffectParticle(options.type.getParticleID(), x, y, z,
                    Math.max(options.dustColor[0], 1.0e-4f), options.dustColor[1], options.dustColor[2]);
            if (particle != null && options.dustScale != 1.0f) {
                particle.multipleParticleScaleBy(options.dustScale);
            }
            return particle;
        }
        return particleEngine.spawnEffectParticle(options.type.getParticleID(), x, y, z, xSpeed, ySpeed, zSpeed, options.arguments);
    }

    public static void spawn(WorldClient level, Vec3d hitPos, EnumFacing direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        if (!RenderConfig.HIT_PARTICLE_ENABLE.get() || !isCloseEnough(hitPos)) {
            return;
        }
        IBlockState blockState = level.getBlockState(blockPos);
        ParticleManager particleEngine = Minecraft.getMinecraft().effectRenderer;
        Random random = level.rand;
        Integer blockParticleCount = getAmmoBlockParticleCount(ammoId);
        spawnBlockParticles(particleEngine, random, blockState, hitPos, direction, blockParticleCount, incendiary);
        if (!RenderConfig.HIT_EXTRA_PARTICLE_ENABLE.get()) {
            return;
        }
        List<HitSurfaceParticle> particles = getDisplayParticles(ammoId, gunId, gunDisplayId);
        if (particles == null || particles.isEmpty()) {
            return;
        }
        int remainingCount = MAX_DISPLAY_PARTICLE_COUNT_PER_HIT;
        for (HitSurfaceParticle particle : particles) {
            remainingCount -= spawnDisplayParticle(particleEngine, random, blockState, hitPos, direction, particle, remainingCount);
            if (remainingCount <= 0) {
                return;
            }
        }
    }

    private static boolean isCloseEnough(Vec3d hitPos) {
        int maxDistance = RenderConfig.HIT_PARTICLE_MAX_DISTANCE.get();
        if (maxDistance <= 0) {
            return true;
        }
        EntityPlayer player = Minecraft.getMinecraft().player;
        double maxDistanceSqr = (double) maxDistance * (double) maxDistance;
        return player != null && player.getDistanceSq(hitPos.x, hitPos.y, hitPos.z) <= maxDistanceSqr;
    }

    private static void spawnBlockParticles(ParticleManager particleEngine, Random random, IBlockState blockState, Vec3d hitPos, EnumFacing direction, @Nullable Integer configuredCount, boolean incendiary) {
        int count = configuredCount == null ? DEFAULT_BLOCK_PARTICLE_COUNT : Math.min(configuredCount, MAX_BLOCK_PARTICLE_COUNT);
        if (count <= 0) {
            return;
        }
        ParticleOptions options = incendiary ? new ParticleOptions(EnumParticleTypes.LAVA) : ParticleOptions.block(EnumParticleTypes.BLOCK_CRACK, blockState);
        for (int i = 0; i < count; i++) {
            Vec3d pos = randomPosition(random, hitPos, direction, DEFAULT_SURFACE_OFFSET, DEFAULT_RANDOM_OFFSET);
            Vec3d speed = randomVelocity(random,
                    direction,
                    DEFAULT_BLOCK_NORMAL_SPEED,
                    DEFAULT_BLOCK_RANDOM_SPEED,
                    DEFAULT_BLOCK_SPREAD_ANGLE);
            createParticle(particleEngine, options, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
        }
    }

    @Nullable
    private static Integer getAmmoBlockParticleCount(String ammoId) {
        try {
            return TimelessAPI.getClientAmmoIndex(new ResourceLocation(ammoId))
                    .map(ammoIndex -> ammoIndex.getHitBlockParticleCount())
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static List<HitSurfaceParticle> getDisplayParticles(String ammoId, String gunId, String gunDisplayId) {
        try {
            ResourceLocation gunDisplayLocation = new ResourceLocation(gunDisplayId);
            ResourceLocation gunLocation = new ResourceLocation(gunId);
            var gunDisplay = TimelessAPI.getGunDisplay(gunDisplayLocation, gunLocation);
            if (gunDisplay.isPresent() && gunDisplay.get().getHitSurfaceParticles() != null) {
                return gunDisplay.get().getHitSurfaceParticles();
            }
            return TimelessAPI.getClientAmmoIndex(new ResourceLocation(ammoId))
                    .map(ammoIndex -> ammoIndex.getHitSurfaceParticles())
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int spawnDisplayParticle(ParticleManager particleEngine, Random random, IBlockState blockState, Vec3d hitPos, EnumFacing direction, HitSurfaceParticle particle, int maxCount) {
        ParticleOptions options = parseParticle(blockState, particle);
        if (options == null || maxCount <= 0) {
            return 0;
        }
        int count = Math.min(randomCount(random, particle), maxCount);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Vec3d pos = randomPosition(random, hitPos, direction, particle.getSurfaceOffset(), particle.getRandomOffset());
            Vec3d speed = randomVelocity(random, direction, particle.getNormalSpeed(), particle.getRandomSpeed(), particle.getSpreadAngle());
            Particle result = createParticle(particleEngine, options, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
            if (result != null) {
                applyParticleOverrides(result, particle);
                spawned++;
            }
        }
        return spawned;
    }

    private static int randomCount(Random random, HitSurfaceParticle particle) {
        int min = particle.getCountMin();
        int max = particle.getCountMax();
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        if (max == min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    @Nullable
    private static ParticleOptions parseParticle(IBlockState blockState, HitSurfaceParticle particle) {
        String name = particle.getName();
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String trimmedName = name.trim();
        boolean hasArguments = trimmedName.split("\\s+", 2).length > 1;
        if (hasArguments) {
            // TODO(port): 1.12.2 has no particle argument parser; arguments are ignored
            return readParticle(trimmedName);
        }
        String particleId = normalizeParticleId(trimmedName);
        if (BLOCK.equals(particleId)) {
            return ParticleOptions.block(EnumParticleTypes.BLOCK_CRACK, blockState);
        }
        if (FALLING_DUST.equals(particleId)) {
            return ParticleOptions.block(EnumParticleTypes.FALLING_DUST, blockState);
        }
        if (DUST.equals(particleId)) {
            float[] color = colorOrDefault(particle.getColor(), 1.0f, 0.35f, 0.1f);
            return new ParticleOptions(EnumParticleTypes.REDSTONE, color, positiveScale(particle));
        }
        if (DUST_COLOR_TRANSITION.equals(particleId)) {
            float[] color = colorOrDefault(particle.getColor(), 1.0f, 0.35f, 0.1f);
            // TODO(port): 1.12.2 dust cannot fade to a second color (toColor = color * 0.25)
            return new ParticleOptions(EnumParticleTypes.REDSTONE, color, positiveScale(particle));
        }
        if (ITEM.equals(particleId)) {
            return null;
        }
        return readParticle(trimmedName);
    }

    /**
     * 1.12.2 stand-in for {@code ParticleArgument.readParticle}: the closest vanilla particle type, without arguments.
     */
    @Nullable
    private static ParticleOptions readParticle(String name) {
        EnumParticleTypes type = LegacyParticles.readParticle(name);
        // particles that need arguments (block/item states) cannot be built from a name alone
        if (type == null || type.getArgumentCount() > 0) {
            return null;
        }
        return new ParticleOptions(type);
    }

    private static String normalizeParticleId(String particleName) {
        String firstToken = particleName.split("\\s+", 2)[0];
        ResourceLocation id = ResourceLocations.tryParse(firstToken);
        if (id == null) {
            return firstToken;
        }
        return id.toString();
    }

    private static float positiveScale(HitSurfaceParticle particle) {
        Float scale = particle.getScale();
        return scale == null ? 1.0f : Math.max(scale, 0.0f);
    }

    private static float[] colorOrDefault(@Nullable String color, float red, float green, float blue) {
        if (StringUtils.isBlank(color)) {
            return new float[]{red, green, blue};
        }
        return ColorHex.colorTextToRbgFloatArray(color);
    }

    private static void applyParticleOverrides(Particle result, HitSurfaceParticle particle) {
        result.setMaxAge(particle.getLifeTime());
        String color = particle.getColor();
        if (StringUtils.isNotBlank(color)) {
            float[] rgb = ColorHex.colorTextToRbgFloatArray(color);
            result.setRBGColorF(rgb[0], rgb[1], rgb[2]);
        }
        Float scale = particle.getScale();
        if (scale != null && scale > 0 && !usesScaleInParticleOptions(particle)) {
            result.multipleParticleScaleBy(scale);
        }
    }

    private static boolean usesScaleInParticleOptions(HitSurfaceParticle particle) {
        String name = particle.getName();
        if (StringUtils.isBlank(name)) {
            return false;
        }
        String particleId = normalizeParticleId(name.trim());
        return DUST.equals(particleId) || DUST_COLOR_TRANSITION.equals(particleId);
    }

    private static Vec3d randomPosition(Random random, Vec3d hitPos, EnumFacing direction, float surfaceOffset, float randomOffset) {
        Vec3d normal = new Vec3d(direction.getDirectionVec());
        double offsetX = randomOffset == 0 ? 0 : random.nextGaussian() * randomOffset;
        double offsetY = randomOffset == 0 ? 0 : random.nextGaussian() * randomOffset;
        double offsetZ = randomOffset == 0 ? 0 : random.nextGaussian() * randomOffset;
        return hitPos.add(normal.scale(surfaceOffset)).add(offsetX, offsetY, offsetZ);
    }

    private static Vec3d randomVelocity(Random random, EnumFacing direction, float normalSpeed, float randomSpeed, float spreadAngle) {
        Vec3d normal = new Vec3d(direction.getDirectionVec()).normalize();
        Vec3d tangent = Math.abs(normal.y) > 0.9 ? new Vec3d(1, 0, 0) : normal.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d bitangent = normal.crossProduct(tangent).normalize();
        double spread = Math.tan(Math.toRadians(spreadAngle));
        Vec3d cone = normal
                .add(tangent.scale(random.nextGaussian() * spread))
                .add(bitangent.scale(random.nextGaussian() * spread))
                .normalize()
                .scale(normalSpeed);
        Vec3d randomVector = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(randomSpeed);
        return cone.add(randomVector);
    }
}
