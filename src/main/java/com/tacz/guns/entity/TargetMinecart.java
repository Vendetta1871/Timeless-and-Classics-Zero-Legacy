package com.tacz.guns.entity;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.legacy.util.Components;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.entity.item.EntityMinecart.Type.RIDEABLE;

public class TargetMinecart extends EntityMinecart implements ITargetEntity {
    // 1.12.2: the EntityType is an EntityEntry, registered by ModEntities. clientTrackingRange(8) chunks = 128 blocks
    public static EntityEntry TYPE = EntityEntryBuilder.create().entity(TargetMinecart.class)
            .id(new ResourceLocation(GunMod.MOD_ID, "target_minecart"), 0)
            .name("target_minecart")
            .tracker(128, 3, true)
            .build();

    private @Nullable GameProfile gameProfile = null;

    public TargetMinecart(World world) {
        super(world);
        this.setSize(0.75F, 2.4F);
    }

    public TargetMinecart(World level, double x, double y, double z) {
        super(level, x, y, z);
        this.setSize(0.75F, 2.4F);
    }

    @Override
    public void onProjectileHit(Entity entity, RayTraceResult result, DamageSource source, float damage) {
        if (this.world.isRemote || this.isDead) {
            return;
        }
        // 1.12.2: DamageSource#isIndirect is "the direct entity is not the causing entity"
        if (!(source.getImmediateSource() != source.getTrueSource())) {
            return;
        }
        Entity sourceEntity = source.getTrueSource();
        if (sourceEntity instanceof EntityPlayer player) {
            this.setRollingDirection(-1);
            this.setRollingAmplitude(10);
            this.velocityChanged = true;
            this.setDamage(10);
            double dis = this.getPositionVector().distanceTo(sourceEntity.getPositionVector());
            player.sendStatusMessage(Components.translatable("message.tacz.target_minecart.hit", String.format("%.1f", damage), String.format("%.2f", dis)), true);
            // 原版的声音传播距离由 volume 决定
            // 当声音大于 1 时，距离为 = 16 * volume
            float volume = OtherConfig.TARGET_SOUND_DISTANCE.get() / 16.0f;
            volume = Math.max(volume, 0);
            world.playSound(null, this.posX, this.posY, this.posZ, ModSounds.TARGET_HIT.get(), SoundCategory.BLOCKS, volume, this.world.rand.nextFloat() * 0.1F + 0.9F);

            // TODO(port): needs EntityKineticBullet (EntityHurtByGunEvent.Post, ServerMessageGunHurt)
//            if (entity instanceof EntityKineticBullet projectile) {
//                boolean isHeadshot = false;
//                float headshotMultiplier = 1;
//                MinecraftForge.EVENT_BUS.post(new EntityHurtByGunEvent.Post(projectile, this, player, projectile.getGunId(), projectile.getGunDisplayId(), damage, Pair.of(source, source), isHeadshot, headshotMultiplier, Side.SERVER));
//                NetworkHandler.sendToDimension(new ServerMessageGunHurt(projectile.getEntityId(), this.getEntityId(), player.getEntityId(), projectile.getGunId(), projectile.getGunDisplayId(), damage, isHeadshot, headshotMultiplier), this);
//            }
        }
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return source.isExplosion() || super.isEntityInvulnerable(source);
    }

    /**
     * 1.12.2: upstream's {@code canBeRidden() == false}; riding is started by the interaction.
     */
    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        double size = this.getEntityBoundingBox().getAverageEdgeLength();
        if (Double.isNaN(size)) {
            size = 1.0;
        }
        size *= RenderConfig.TARGET_RENDER_DISTANCE.get() * getRenderDistanceWeight();
        return distance < size * size;
    }

    @Override
    public void killMinecart(DamageSource source) {
        this.setDead();
        if (this.world.getGameRules().getBoolean("doEntityDrops")) {
            ItemStack itemStack = new ItemStack(ModItems.TARGET_MINECART.get());
            if (this.hasCustomName()) {
                itemStack.setStackDisplayName(this.getCustomNameTag());
            }
            this.entityDropItem(itemStack, 0.0F);
        }
    }

    /**
     * 1.12.2: {@code getPickResult} (Forge's {@code getCartItem})
     */
    @Override
    public ItemStack getCartItem() {
        ItemStack itemStack = new ItemStack(ModItems.TARGET_MINECART.get());
        if (this.hasCustomName()) {
            itemStack.setStackDisplayName(this.getCustomNameTag());
        }
        return itemStack;
    }

    @Nullable
    public GameProfile getGameProfile() {
        if (this.gameProfile == null && this.hasCustomName()) {
            this.gameProfile = new GameProfile(null, this.getCustomNameTag());
            this.gameProfile = TileEntitySkull.updateGameProfile(this.gameProfile);
        }
        return gameProfile;
    }

    @Override
    @NotNull
    public IBlockState getDefaultDisplayTile() {
        return ModBlocks.TARGET.get().getDefaultState();
    }

    @NotNull
    @Override
    public Type getType() {
        return RIDEABLE;
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.2F;
    }
}
