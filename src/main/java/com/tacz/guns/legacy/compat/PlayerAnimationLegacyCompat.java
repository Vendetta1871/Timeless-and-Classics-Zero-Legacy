package com.tacz.guns.legacy.compat;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Compatibility with Player Animation Legacy (https://www.curseforge.com/minecraft/mc-mods/player-animation-legacy,
 * mod id {@code entity_features}), a 1.12.2 port of Entity Model Features running the Fresh Animations player
 * model. It has no API: at the end of {@link ModelPlayer#setRotationAngles} it reads the vanilla pose, runs its own
 * animations and writes every part back, which replaced the TACZ gun pose (hardcoded or Player Animator) with its
 * idle/walk animations, so players held guns like plain items.
 * <p>
 * While a TACZ gun pose is active, {@code ModelPlayerPoseCaptureMixin} (runs before it) saves the pose TACZ
 * computed and {@code ModelPlayerPoseRestoreMixin} (runs after it) puts it back: arms, body and head always (the
 * gun follows them), legs only when Player Animator drives them too; otherwise the legs keep Fresh Animations'
 * walking. Armor models, which Player Animation Legacy syncs to its own pose, get the restored pose through
 * {@code ModelBipedArmorPoseMixin}.
 */
@SideOnly(Side.CLIENT)
public final class PlayerAnimationLegacyCompat {
    public static final String MOD_ID = "entity_features";

    private static final boolean PRESENT = Loader.isModLoaded(MOD_ID);

    private static final int HEAD = 0, BODY = 1, RIGHT_ARM = 2, LEFT_ARM = 3, RIGHT_LEG = 4, LEFT_LEG = 5;
    private static final float[][] POSE = new float[6][6];

    /** The entity whose pose {@link #POSE} holds, or null when nothing is to be restored. */
    private static Entity capturedEntity;
    private static boolean capturedBody;
    private static boolean capturedLegs;

    private PlayerAnimationLegacyCompat() {
    }

    public static void capture(ModelPlayer model, Entity entity, float ageInTicks) {
        capturedEntity = null;
        if (!PRESENT || !(entity instanceof EntityPlayer player)) {
            return;
        }
        if (ageInTicks == 0F) {
            // first-person arm (PlayerModelMixin resets the arm for a held gun): keep it out of the animation too
            if (IGun.getIGunOrNull(KeepingItemRenderer.getRenderer().getCurrentItem()) == null) {
                return;
            }
            capturedBody = false;
            capturedLegs = false;
        } else {
            if (!isGunPosed(player)) {
                return;
            }
            capturedBody = true;
            capturedLegs = TimelessAPI.getGunDisplay(player.getHeldItemMainhand())
                    .map(display -> PlayerAnimatorCompat.hasPlayerAnimator3rd(player, display)).orElse(false);
        }
        save(model.bipedHead, HEAD);
        save(model.bipedBody, BODY);
        save(model.bipedRightArm, RIGHT_ARM);
        save(model.bipedLeftArm, LEFT_ARM);
        save(model.bipedRightLeg, RIGHT_LEG);
        save(model.bipedLeftLeg, LEFT_LEG);
        capturedEntity = entity;
    }

    public static void restore(ModelPlayer model, Entity entity) {
        if (capturedEntity == null || capturedEntity != entity) {
            return;
        }
        apply(model);
        ModelBase.copyModelAngles(model.bipedRightArm, model.bipedRightArmwear);
        ModelBase.copyModelAngles(model.bipedLeftArm, model.bipedLeftArmwear);
        if (capturedBody) {
            ModelBase.copyModelAngles(model.bipedBody, model.bipedBodyWear);
            ModelBase.copyModelAngles(model.bipedHead, model.bipedHeadwear);
        }
        if (capturedLegs) {
            ModelBase.copyModelAngles(model.bipedRightLeg, model.bipedRightLegwear);
            ModelBase.copyModelAngles(model.bipedLeftLeg, model.bipedLeftLegwear);
        }
    }

    /** Armor layers render right after the player model of the same entity. */
    public static void restoreArmor(ModelBiped model, Entity entity) {
        if (capturedEntity == null || capturedEntity != entity || model instanceof ModelPlayer) {
            return;
        }
        apply(model);
        ModelBase.copyModelAngles(model.bipedHead, model.bipedHeadwear);
    }

    private static void apply(ModelBiped model) {
        load(model.bipedRightArm, RIGHT_ARM);
        load(model.bipedLeftArm, LEFT_ARM);
        if (capturedBody) {
            load(model.bipedHead, HEAD);
            load(model.bipedBody, BODY);
        }
        if (capturedLegs) {
            load(model.bipedRightLeg, RIGHT_LEG);
            load(model.bipedLeftLeg, LEFT_LEG);
        }
    }

    /** Mirrors the checks in {@code InnerThirdPersonManager#setRotationAnglesHead}: when TACZ posed the model. */
    private static boolean isGunPosed(EntityLivingBase entity) {
        if (Minecraft.getMinecraft().isGamePaused() || !(entity instanceof IGunOperator)) {
            return false;
        }
        ItemStack mainHandItem = entity.getHeldItemMainhand();
        if (IGun.getIGunOrNull(mainHandItem) == null) {
            return false;
        }
        if (entity instanceof EntityPlayer player && player.isPlayerSleeping() || entity.isOnLadder() || entity.isElytraFlying()) {
            return false;
        }
        return TimelessAPI.getGunDisplay(mainHandItem).isPresent();
    }

    private static void save(ModelRenderer part, int index) {
        float[] p = POSE[index];
        p[0] = part.rotateAngleX;
        p[1] = part.rotateAngleY;
        p[2] = part.rotateAngleZ;
        p[3] = part.rotationPointX;
        p[4] = part.rotationPointY;
        p[5] = part.rotationPointZ;
    }

    private static void load(ModelRenderer part, int index) {
        float[] p = POSE[index];
        part.rotateAngleX = p[0];
        part.rotateAngleY = p[1];
        part.rotateAngleZ = p[2];
        part.rotationPointX = p[3];
        part.rotationPointY = p[4];
        part.rotationPointZ = p[5];
    }
}
