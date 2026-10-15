package com.tacz.guns.client.particle;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 * <p>
 * 1.12.2: {@code TextureSheetParticle} is a {@link Particle} on the block texture layer ({@link #getFXLayer()} 1);
 * {@code Direction#getRotation()} is written out per facing, {@code lifetime} is {@code particleMaxAge}.
 */
@SideOnly(Side.CLIENT)
public class BulletHoleParticle extends Particle {
    private final EnumFacing direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    public BulletHoleParticle(WorldClient world, double x, double y, double z, EnumFacing direction, BlockPos pos, String ammoId, String gunId, String gunDisplayId) {
        super(world, x, y, z);
        this.setSprite(this.getSprite(pos));
        this.direction = direction;
        this.pos = pos;
        this.particleMaxAge = this.getLifetimeFromConfig(world);
        this.canCollide = false;
        this.particleGravity = 0.0F;
        this.particleScale = 0.05F;
        // the 1.12.2 constructor gives the particle a random motion
        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == ModBlocks.TARGET.get() || shouldRemove()) {
            this.setExpired();
        }
        TimelessAPI.getGunDisplay(new ResourceLocation(gunDisplayId), new ResourceLocation(gunId)).ifPresent(gunIndex -> {
            float[] gunTracerColor = gunIndex.getTracerColor();
            if (gunTracerColor != null) {
                this.particleRed = gunTracerColor[0];
                this.particleGreen = gunTracerColor[1];
                this.particleBlue = gunTracerColor[2];
            } else {
                TimelessAPI.getClientAmmoIndex(new ResourceLocation(ammoId)).ifPresent(ammoIndex -> {
                    float[] ammoTracerColor = ammoIndex.getTracerColor();
                    this.particleRed = ammoTracerColor[0];
                    this.particleGreen = ammoTracerColor[1];
                    this.particleBlue = ammoTracerColor[2];
                });
            }
        });
        this.particleAlpha = 0.9F;
    }

    private int getLifetimeFromConfig(WorldClient world) {
        int configLife = RenderConfig.BULLET_HOLE_PARTICLE_LIFE.get();
        if (configLife <= 1) {
            return configLife;
        }
        return configLife + world.rand.nextInt(configLife / 2);
    }

    // upstream: protected void setSprite(TextureAtlasSprite) override
    private void setSprite(TextureAtlasSprite sprite) {
        this.setParticleTexture(sprite);
        this.uOffset = this.rand.nextInt(16);
        this.vOffset = this.rand.nextInt(16);
        // 材质应该都是方形
        this.textureDensity = (sprite.getMaxU() - sprite.getMinU()) / 16.0F;
    }

    private TextureAtlasSprite getSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            return minecraft.getBlockRendererDispatcher().getBlockModelShapes().getTexture(state);
        }
        return minecraft.getTextureMapBlocks().getMissingSprite();
    }

    protected float getU0() {
        return this.particleTexture.getMinU() + this.uOffset * this.textureDensity;
    }

    protected float getV0() {
        return this.particleTexture.getMinV() + this.vOffset * this.textureDensity;
    }

    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (shouldRemove()) {
            this.setExpired();
        }
    }

    /**
     * {@code Direction#getRotation()} applied to a point of the quad that lies in the X/Z plane facing up.
     */
    private static float[] rotate(EnumFacing direction, float x, float y, float z) {
        switch (direction) {
            case DOWN:
                return new float[]{x, -y, -z};
            case NORTH:
                return new float[]{-x, -z, -y};
            case SOUTH:
                return new float[]{x, -z, y};
            case WEST:
                return new float[]{-y, -z, x};
            case EAST:
                return new float[]{y, -z, -x};
            case UP:
            default:
                return new float[]{x, y, z};
        }
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        float particleX = (float) (this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
        float particleY = (float) (this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
        float particleZ = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
        float[][] points = new float[][]{
                // Y 值稍微大一点点，防止 z-fight
                rotate(direction, -1.0F, 0.01F, -1.0F),
                rotate(direction, -1.0F, 0.01F, 1.0F),
                rotate(direction, 1.0F, 0.01F, 1.0F),
                rotate(direction, 1.0F, 0.01F, -1.0F)
        };
        float scale = this.particleScale;

        for (int i = 0; i < 4; ++i) {
            float[] point = points[i];
            point[0] = point[0] * scale + particleX;
            point[1] = point[1] * scale + particleY;
            point[2] = point[2] * scale + particleZ;
        }

        // UV 坐标
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        // 0 - 30 tick 内，从 15 亮度到 0 亮度
        int light = Math.max(15 - this.particleAge / 2, 0);
        // LightTexture.pack(light, light): block light in bits 4.., sky light in bits 20..
        int lightU = light << 4;
        int lightV = light << 4;

        // 颜色，逐渐渐变到 0 0 0，也就是黑色
        float colorPercent = light / 15.0f;
        float red = this.particleRed * colorPercent;
        float green = this.particleGreen * colorPercent;
        float blue = this.particleBlue * colorPercent;

        // 透明度，逐渐变成 0，也就是透明
        double threshold = RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.get() * this.particleMaxAge;
        float fade = 1.0f - (float) (Math.max(this.particleAge - threshold, 0) / (this.particleMaxAge - threshold));
        float alphaFade = this.particleAlpha * fade;

        buffer.pos(points[0][0], points[0][1], points[0][2]).tex(u1, v1).color(red, green, blue, alphaFade).lightmap(lightU, lightV).endVertex();
        buffer.pos(points[1][0], points[1][1], points[1][2]).tex(u1, v0).color(red, green, blue, alphaFade).lightmap(lightU, lightV).endVertex();
        buffer.pos(points[2][0], points[2][1], points[2][2]).tex(u0, v0).color(red, green, blue, alphaFade).lightmap(lightU, lightV).endVertex();
        buffer.pos(points[3][0], points[3][1], points[3][2]).tex(u0, v1).color(red, green, blue, alphaFade).lightmap(lightU, lightV).endVertex();
    }

    /**
     * {@code ParticleRenderType.TERRAIN_SHEET}: the block texture atlas layer.
     */
    @Override
    public int getFXLayer() {
        return 1;
    }

    private boolean shouldRemove() {
        final IBlockState blockState = this.world.getBlockState(this.pos);
        if (blockState.getMaterial() == Material.AIR) {
            return true;
        } else {
            // 阻止弹孔在与方块不构成有效附着时继续渲染
            AxisAlignedBB baseBlockBoundingBox = blockState.getCollisionBoundingBox(this.world, this.pos);
            if (baseBlockBoundingBox == null) {
                return true;
            }
            AxisAlignedBB blockBoundingBox = baseBlockBoundingBox.offset(this.pos);
            boolean intersects = blockBoundingBox.intersects(
                    this.posX - 0.1, this.posY - 0.1, this.posZ - 0.1,
                    this.posX + 0.1, this.posY + 0.1, this.posZ + 0.1);
            return !intersects;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class Provider {
        public Provider() {
        }

        // upstream: ParticleProvider#createParticle (1.12.2 has no particle options: the option is the plain data class)
        public BulletHoleParticle createParticle(@NotNull BulletHoleOption option, @NotNull WorldClient world, double x, double y, double z, double pXSpeed, double pYSpeed, double pZSpeed) {
            BulletHoleParticle particle = new BulletHoleParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getAmmoId(), option.getGunId(), option.getGunDisplayId());
            return particle;
        }
    }
}
