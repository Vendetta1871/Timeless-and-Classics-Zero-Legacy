package com.tacz.guns.compat.jei;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IBlock;
import com.tacz.guns.api.item.IGun;
import mezz.jei.api.ISubtypeRegistry;
import net.minecraft.item.ItemStack;

/**
 * 1.12.2 port of upstream's subtype interpreters: distinguishes NBT variants (gun/ammo/attachment ids)
 * so JEI treats e.g. different guns as different ingredients.
 */
public class GunModSubtype {
    public static ISubtypeRegistry.ISubtypeInterpreter getAmmoSubtype() {
        return stack -> {
            if (stack.getItem() instanceof IAmmo) {
                return ((IAmmo) stack.getItem()).getAmmoId(stack).toString();
            }
            return ISubtypeRegistry.ISubtypeInterpreter.NONE;
        };
    }

    public static ISubtypeRegistry.ISubtypeInterpreter getGunSubtype() {
        return stack -> {
            if (stack.getItem() instanceof IGun) {
                return ((IGun) stack.getItem()).getGunId(stack).toString();
            }
            return ISubtypeRegistry.ISubtypeInterpreter.NONE;
        };
    }

    public static ISubtypeRegistry.ISubtypeInterpreter getAttachmentSubtype() {
        return stack -> {
            if (stack.getItem() instanceof IAttachment) {
                return ((IAttachment) stack.getItem()).getAttachmentId(stack).toString();
            }
            return ISubtypeRegistry.ISubtypeInterpreter.NONE;
        };
    }

    public static ISubtypeRegistry.ISubtypeInterpreter getTableSubType() {
        return stack -> {
            if (stack.getItem() instanceof IBlock) {
                return ((IBlock) stack.getItem()).getBlockId(stack).toString();
            }
            return ISubtypeRegistry.ISubtypeInterpreter.NONE;
        };
    }

    public static ISubtypeRegistry.ISubtypeInterpreter getAmmoBoxSubtype() {
        return stack -> {
            if (stack.getItem() instanceof IAmmoBox) {
                IAmmoBox iAmmoBox = (IAmmoBox) stack.getItem();
                if (iAmmoBox.isAllTypeCreative(stack)) {
                    return "all_type_creative";
                }
                if (iAmmoBox.isCreative(stack)) {
                    return "creative";
                }
                return String.format("level_%d", iAmmoBox.getAmmoLevel(stack));
            }
            return ISubtypeRegistry.ISubtypeInterpreter.NONE;
        };
    }
}
