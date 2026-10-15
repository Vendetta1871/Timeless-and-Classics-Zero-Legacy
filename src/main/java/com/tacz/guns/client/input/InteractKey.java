package com.tacz.guns.client.input;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.tacz.guns.GunMod;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.input.Keyboard;
import com.tacz.guns.legacy.util.KeyInputs;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class InteractKey {
    public static final KeyBinding INTERACT_KEY = new KeyBinding("key.tacz.interact.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            Keyboard.KEY_O,
            "key.category.tacz");

    @SubscribeEvent
    public static void onInteractKeyPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && KeyInputs.isPress() && KeyInputs.matches(INTERACT_KEY)) {
            doInteractLogic();
        }
    }

    @SubscribeEvent
    public static void onInteractMousePress(InputEvent.MouseInputEvent event) {
        if (isInGame() && KeyInputs.isMousePress() && KeyInputs.matchesMouse(INTERACT_KEY)) {
            doInteractLogic();
        }
    }

    public static boolean onInteractControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            if (player == null || player.isSpectator()) {
                return false;
            }
            if (!IGun.mainHandHoldGun(player)) {
                return false;
            }
            RayTraceResult hitResult = mc.hitResult;
            if (hitResult == null) {
                return false;
            }
            if (hitResult instanceof RayTraceResult blockHitResult) {
                interactBlock(blockHitResult, player, mc);
                return true;
            }
            if (hitResult instanceof RayTraceResult entityHitResult) {
                interactEntity(entityHitResult, mc);
                return true;
            }
        }
        return false;
    }

    private static void doInteractLogic() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return;
        }
        RayTraceResult hitResult = mc.hitResult;
        if (hitResult == null) {
            return;
        }
        if (hitResult instanceof RayTraceResult blockHitResult) {
            interactBlock(blockHitResult, player, mc);
            return;
        }
        if (hitResult instanceof RayTraceResult entityHitResult) {
            interactEntity(entityHitResult, mc);
        }
    }

    private static void interactBlock(RayTraceResult blockHitResult, EntityPlayerSP player, Minecraft mc) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        IBlockState block = player.world.getBlockState(blockPos);
        if (InteractKeyConfigRead.canInteractBlock(block)) {
            mc.startUseItem();
        }
    }

    private static void interactEntity(RayTraceResult entityHitResult, Minecraft mc) {
        Entity entity = entityHitResult.getEntity();
        if (InteractKeyConfigRead.canInteractEntity(entity)) {
            mc.startUseItem();
        }
    }
}
