package com.tacz.guns.client.event;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.client.gui.overlay.DisplayCaseLockHintOverlay;
import com.tacz.guns.legacy.util.Components;
import com.tacz.guns.legacy.util.KeyInputs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = GunMod.MOD_ID)
public class GunDisplayCaseClientInteractionEvent {
    @SubscribeEvent
    public static void onUseKey(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || player.isSpectator() || !KeyInputs.isPress() || !KeyInputs.matches(minecraft.gameSettings.keyBindUseItem)) {
            return;
        }
        if (!player.getHeldItemMainhand().isEmpty() || player.isSneaking()) {
            return;
        }
        showHintForTarget(minecraft, false);
    }

    @SubscribeEvent
    public static void onUseMouse(InputEvent.MouseInputEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || player.isSpectator() || !KeyInputs.isMousePress() || !KeyInputs.matchesMouse(minecraft.gameSettings.keyBindUseItem)) {
            return;
        }
        if (!player.getHeldItemMainhand().isEmpty() || player.isSneaking()) {
            return;
        }
        showHintForTarget(minecraft, false);
    }

    public static void showHintForTarget(Minecraft minecraft, boolean lockedOnly) {
        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || minecraft.world == null) {
            return;
        }
        IBlockState state = minecraft.world.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof GunDisplayCaseBlock)) {
            return;
        }
        boolean locked = state.getValue(GunDisplayCaseBlock.LOCKED);
        if (lockedOnly && !locked) {
            return;
        }
        DisplayCaseLockHintOverlay.show(createHint(locked, minecraft));
    }

    private static ITextComponent createHint(boolean locked, Minecraft minecraft) {
        String shiftKey = minecraft.gameSettings.keyBindSneak.getDisplayName();
        String useKey = minecraft.gameSettings.keyBindUseItem.getDisplayName();
        String chord = shiftKey + " + " + useKey;
        return Components.translatable(locked ? "gui.tacz.display_case.locked_hint" : "gui.tacz.display_case.unlocked_hint", chord);
    }

    public static boolean holdingGun(EntityPlayerSP player) {
        return player.getHeldItemMainhand().getItem() instanceof IGun;
    }
}
