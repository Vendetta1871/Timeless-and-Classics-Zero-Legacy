package com.tacz.guns.client.input;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.client.event.GunDisplayCaseClientInteractionEvent;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageAmmoStorageInteract;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.input.Keyboard;
import com.tacz.guns.legacy.util.MinecraftAccess;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
public class InteractKey {
    public static final KeyBinding INTERACT_KEY = new KeyBinding("key.tacz.interact.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_O,
            "key.category.tacz");

    public static boolean onInteractControllerPress(boolean isPress) {
        return isPress && onInteractPress(true);
    }

    public static boolean onInteractPress() {
        return onInteractPress(false);
    }

    public static boolean sharesUseBinding(KeyBinding useKey) {
        // 1.12.2: an unbound key has key code 0 (KEY_NONE)
        return useKey.getKeyCode() != Keyboard.KEY_NONE
                && INTERACT_KEY.getKeyCode() == useKey.getKeyCode()
                && INTERACT_KEY.getKeyModifier() == useKey.getKeyModifier();
    }

    private static boolean onInteractPress(boolean fromController) {
        if (!isInGame()) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return false;
        }
        RayTraceResult hitResult = mc.objectMouseOver;
        if (hitResult == null) {
            return false;
        }
        boolean vanillaHandlesUse = !fromController && sharesUseBinding(mc.gameSettings.keyBindUseItem);
        if (hitResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            interactBlock(hitResult, player, mc, vanillaHandlesUse);
            return true;
        }
        if (hitResult.typeOfHit == RayTraceResult.Type.ENTITY) {
            interactEntity(hitResult, mc, vanillaHandlesUse);
            return true;
        }
        return false;
    }

    /**
     * 1.12.2: whether the crosshair is on a block or entity this key would interact with.
     * {@code KeyConfig.RMB_INTERACT_WITH_OBJECTS} lets the right mouse button interact with such a target
     * instead of aiming (upstream always aims with it and interacts only through this key).
     */
    public static boolean isTargetInteractable(Minecraft mc) {
        RayTraceResult hitResult = mc.objectMouseOver;
        if (hitResult == null) {
            return false;
        }
        if (hitResult.typeOfHit == RayTraceResult.Type.BLOCK) {
            EntityPlayerSP player = mc.player;
            return player != null && InteractKeyConfigRead.canInteractBlock(player.world.getBlockState(hitResult.getBlockPos()));
        }
        if (hitResult.typeOfHit == RayTraceResult.Type.ENTITY) {
            return InteractKeyConfigRead.canInteractEntity(hitResult.entityHit);
        }
        return false;
    }

    private static void interactBlock(RayTraceResult blockHitResult, EntityPlayerSP player, Minecraft mc, boolean vanillaHandlesUse) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        IBlockState block = player.world.getBlockState(blockPos);
        if (block.getBlock() instanceof GunDisplayCaseBlock) {
            GunDisplayCaseClientInteractionEvent.showHintForTarget(mc, true);
        }
        if (block.getBlock() instanceof AmmoStorageBlock) {
            NetworkHandler.CHANNEL.sendToServer(new ClientMessageAmmoStorageInteract(blockPos));
            return;
        }
        if (!vanillaHandlesUse && InteractKeyConfigRead.canInteractBlock(block)) {
            MinecraftAccess.startUseItem(mc);
        }
    }

    private static void interactEntity(RayTraceResult entityHitResult, Minecraft mc, boolean vanillaHandlesUse) {
        Entity entity = entityHitResult.entityHit;
        if (!vanillaHandlesUse && InteractKeyConfigRead.canInteractEntity(entity)) {
            MinecraftAccess.startUseItem(mc);
        }
    }
}
