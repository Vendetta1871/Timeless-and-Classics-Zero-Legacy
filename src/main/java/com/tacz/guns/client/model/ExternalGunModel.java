package com.tacz.guns.client.model;

import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 外部视角的 SBM 模型资源。它从 TACZ 已解析的同一份枪包几何构造，
 * 不读取或渲染 BedrockGunModel 的可变骨骼树。
 * <p>
 * TODO(port): needs SimpleBedrockModel v2 (BakedBedrockModel and the VBO mesh path), which is not ported yet: no
 *  external model is ever baked, so every user (display case, pegboard, item VBO rendering) takes its non-VBO
 *  fallback through BedrockGunModel. Upstream also passes a DisplayGunModelInstance.Kind.
 */
public final class ExternalGunModel {
    private ExternalGunModel() {
    }

    @Nullable
    public static ExternalGunModel bake(BedrockModelPOJO source, Set<String> hiddenTextBones) {
        return null;
    }
}
