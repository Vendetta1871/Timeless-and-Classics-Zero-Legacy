package com.tacz.guns.legacy.resource;

/**
 * 1.20.1 {@code SimplePreparableReloadListener}: {@link #prepare} then {@link #apply}, on the calling thread.
 */
public abstract class SimplePreparableReloadListener<T> implements PreparableReloadListener {
    @Override
    public final void reload(ResourceManager resourceManager) {
        T prepared = this.prepare(resourceManager, ProfilerFiller.INACTIVE);
        this.apply(prepared, resourceManager, ProfilerFiller.INACTIVE);
    }

    protected abstract T prepare(ResourceManager resourceManager, ProfilerFiller profiler);

    protected abstract void apply(T prepared, ResourceManager resourceManager, ProfilerFiller profiler);
}
