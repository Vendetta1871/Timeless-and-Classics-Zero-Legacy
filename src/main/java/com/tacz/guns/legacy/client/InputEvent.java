package com.tacz.guns.legacy.client;

import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.20.1 {@code InputEvent} (only {@link InteractionKeyMappingTriggered}). Posted on the Forge event bus by
 * {@code MinecraftMixin} before {@code clickMouse} / {@code rightClickMouse} / {@code middleClickMouse}.
 */
@SideOnly(Side.CLIENT)
public class InputEvent extends Event {
    @Cancelable
    public static class InteractionKeyMappingTriggered extends InputEvent {
        public enum Kind {
            ATTACK, USE_ITEM, PICK_BLOCK
        }

        private final Kind kind;
        private final EnumHand hand;
        private boolean handSwing = true;

        public InteractionKeyMappingTriggered(Kind kind, EnumHand hand) {
            this.kind = kind;
            this.hand = hand;
        }

        public boolean isAttack() {
            return kind == Kind.ATTACK;
        }

        public boolean isUseItem() {
            return kind == Kind.USE_ITEM;
        }

        public boolean isPickItem() {
            return kind == Kind.PICK_BLOCK;
        }

        public EnumHand getHand() {
            return hand;
        }

        public void setSwingHand(boolean value) {
            handSwing = value;
        }

        public boolean shouldSwingHand() {
            return handSwing;
        }
    }
}
