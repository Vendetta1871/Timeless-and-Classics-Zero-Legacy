package com.tacz.guns.entity.sync.core;

import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

/**
 * Author: MrCrayfish.
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public final class SyncedClassKey<E extends Entity> {
    private final Class<E> entityClass;
    private final ResourceLocation id;

    public SyncedClassKey(Class<E> entityClass, ResourceLocation id) {
        this.entityClass = entityClass;
        this.id = id;
    }

    public Class<E> entityClass() {
        return entityClass;
    }

    public ResourceLocation id() {
        return id;
    }

    @Override
    public String toString() {
        return "SyncedClassKey[" + "entityClass=" + entityClass + ", " + "id=" + id + "]";
    }

    public static final SyncedClassKey<EntityLivingBase> LIVING_ENTITY = new SyncedClassKey<>(EntityLivingBase.class, new ResourceLocation("living_entity"));

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncedClassKey<?> that = (SyncedClassKey<?>) o;
        return this.entityClass.getName().equals(that.entityClass.getName());
    }

    @Override
    public int hashCode() {
        return this.entityClass.getName().hashCode();
    }
}
