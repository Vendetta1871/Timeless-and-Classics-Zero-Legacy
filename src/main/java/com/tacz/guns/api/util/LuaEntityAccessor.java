package com.tacz.guns.api.util;

import com.tacz.guns.legacy.util.Components;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.player.EntityPlayer;

@SuppressWarnings("unused")
public final class LuaEntityAccessor {
    private final EntityLivingBase entity;

    public LuaEntityAccessor(EntityLivingBase entity) {
        this.entity = entity;
    }

    public EntityLivingBase entity() {
        return entity;
    }

    public void sendSystemMessage(ITextComponent message) {
        entity.sendMessage(message);
    }

    public void sendActionBar(ITextComponent message) {
        if (entity instanceof EntityPlayer player) {
            player.sendStatusMessage(message, true);
        }
    }

    public float getHealth() {
        return entity.getHealth();
    }

    public boolean hurt(float amount) {
        return entity.attackEntityFrom(DamageSource.GENERIC, amount);
    }

    public ITextComponent literal(String text) {
        return Components.literal(text);
    }

    public ITextComponent translatable(String key) {
        return Components.translatable(key);
    }

    public ITextComponent translatable(String key, ITextComponent... components) {
        return Components.translatable(key, (Object[]) components);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LuaEntityAccessor that = (LuaEntityAccessor) o;
        return java.util.Objects.equals(entity, that.entity);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(entity);
    }

    @Override
    public String toString() {
        return "LuaEntityAccessor[" + "entity=" + entity + "]";
    }
}
