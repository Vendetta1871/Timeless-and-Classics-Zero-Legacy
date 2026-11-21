package com.tacz.guns.client.resource;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 删除所有骨骼的额外静态模型 */
public final class ExternalModelAssets {
    private static final class Model {
        private final Supplier<ResourceLocation> resourceId;
        private final ClientAssetLoadTask<?> task;

        public Model(Supplier<ResourceLocation> resourceId, ClientAssetLoadTask<?> task) {
            this.resourceId = resourceId;
            this.task = task;
        }

        public Supplier<ResourceLocation> resourceId() {
            return resourceId;
        }

        public ClientAssetLoadTask<?> task() {
            return task;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Model)) return false;
            Model that = (Model) o;
            return java.util.Objects.equals(resourceId, that.resourceId) && java.util.Objects.equals(task, that.task);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(resourceId, task);
        }

        @Override
        public String toString() {
            return "Model[" + "resourceId=" + resourceId + ", " + "task=" + task + "]";
        }

    }

    private final List<Model> models = new ArrayList<>();

    /** 添加要后台进行的的任务 */
    public <T> ClientAssetLoadTask<T> add(Supplier<ResourceLocation> resourceId,
                                        Supplier<T> loader, Consumer<Throwable> failed) {
        ClientAssetLoadTask<T> task = ClientAssetLoadDispatcher.task(loader, failed);
        models.add(new Model(resourceId, task));
        return task;
    }

    /** 请求全部后台任务；全部结束后返回 true，缺失或不支持由外观解析判断。 */
    public boolean prepare() {
        boolean pending = false;
        for (Model model : models) {
            model.task().warmUp();
            pending |= model.task().isPending();
        }
        return !pending;
    }

    /** 只提交符合预加载策略的模型，不在此处等待后台任务。 */
    public CompletableFuture<Void> preload() {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (Model model : models) {
            tasks.add(ClientAssetLoadDispatcher.preload(model.task(), model.resourceId().get()));
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[0]));
    }
}
